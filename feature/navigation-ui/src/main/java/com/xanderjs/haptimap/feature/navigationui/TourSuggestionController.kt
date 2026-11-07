package com.xanderjs.haptimap.feature.navigationui

import com.xanderjs.haptimap.core.intelligence.PoiEvaluation
import com.xanderjs.haptimap.core.model.HapticEvent
import com.xanderjs.haptimap.core.model.NavigationProfile
import com.xanderjs.haptimap.core.model.UserSettings
import com.xanderjs.haptimap.core.navigation.NavigationProgress
import com.xanderjs.haptimap.core.navigation.NavigationSession
import com.xanderjs.haptimap.core.navigation.NavigationThresholds
import com.xanderjs.haptimap.core.navigation.navigatingRoute
import com.xanderjs.haptimap.core.navigation.navigationProfile
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Orquesta las sugerencias de POI del Modo Tour (paso 19) sobre la máquina pura [reduce]. Vive a nivel de proceso,
 * como [TourPoiPipeline]: consume el flujo de sugerencias del paso 17 (sin pedir nada nuevo a Google) y, cuando pasa
 * la puerta ([canOfferSuggestion]), muestra la mejor, avisa con el háptico POI_SUGGESTION, espera el tiempo de ajustes
 * y, al cerrarse, la manda a [com.xanderjs.haptimap.core.intelligence.RejectedPoiMemory] vía [reject].
 *
 * Una sola sugerencia a la vez (nunca una cola de avisos): mientras una está en pantalla, las demás esperan. El
 * tiempo de espera se lee de ajustes en cada sugerencia, nunca se copia. Entre una y otra hay un [cooldown]. La
 * navegación manda: una maniobra dentro del radio de aproximación la retira ([TourSuggestionEvent.ManeuverWins]).
 *
 * No conoce la UI: expone [state] para la tarjeta y el botón físico, y [accept]/[dismiss] como entradas. El háptico
 * entra como lambda ([playHaptic] = HapticEngine::play) para no depender del motor y poder probarse sin Android.
 */
class TourSuggestionController(
    private val suggestions: Flow<List<PoiEvaluation>>,
    private val sessions: Flow<NavigationSession>,
    private val progress: Flow<NavigationProgress?>,
    private val settings: Flow<UserSettings>,
    private val reject: (String) -> Unit,
    private val playHaptic: (HapticEvent) -> Unit,
    // paso 20: al aceptar, el desvío toma el relevo (costura ACCEPTED); mientras haya un desvío no se ofrece nada nuevo
    private val onAccept: (PoiEvaluation) -> Unit = {},
    private val detourActive: Flow<Boolean> = flowOf(false),
    private val thresholds: NavigationThresholds = NavigationThresholds(),
    private val cooldown: Duration = SUGGESTION_COOLDOWN,
) {
    private val _state = MutableStateFlow<TourSuggestion>(TourSuggestion.Inactive)

    /** La sugerencia en curso (o [TourSuggestion.Inactive]). La tarjeta la pinta y el botón físico la acepta. */
    val state: StateFlow<TourSuggestion> = _state.asStateFlow()

    // acciones del usuario, para cruzarlas con el timeout y la maniobra; búfer para no perder un toque
    private val actions = Channel<TourSuggestionEvent>(Channel.UNLIMITED)

    /** Acepta la sugerencia visible (botón físico del reloj o acción en pantalla). Sin tarjeta, no hace nada. */
    fun accept() { actions.trySend(TourSuggestionEvent.Accept) }

    /** Descarta la sugerencia visible. Sin tarjeta, no hace nada. */
    fun dismiss() { actions.trySend(TourSuggestionEvent.Dismiss) }

    suspend fun run() = coroutineScope {
        val session = sessions.stateIn(this)
        val best = suggestions.map { it.firstOrNull() }.stateIn(this)
        val prog = progress.stateIn(this)
        val config = settings.stateIn(this)
        val detour = detourActive.stateIn(this)
        while (true) {
            val poi = awaitOfferable(session, best, prog, detour)
            drainStaleActions()
            _state.value = reduce(_state.value, TourSuggestionEvent.Offer(poi))
            playHaptic(HapticEvent.POI_SUGGESTION)

            val outcome = awaitOutcome(poi, session, prog, config.value)
            when (outcome) {
                // dejó de navegar en TOUR: el trayecto se vacía solo (pipeline), no gastamos reject ni cooldown
                null -> _state.value = TourSuggestion.Inactive
                TourSuggestionEvent.Accept -> {
                    _state.value = reduce(_state.value, TourSuggestionEvent.Accept) // -> Accepted (costura paso 20)
                    reject(poi.candidate.id)
                    onAccept(poi) // paso 20: el desvío al POI toma el relevo aquí
                    _state.value = reduce(_state.value, TourSuggestionEvent.Consumed) // cierra la tarjeta; sigue el desvío
                }
                // la navegación manda, pero el POI sigue siendo bueno: se retira sin rechazar y, pasada la
                // maniobra (y el cooldown), el flujo del paso 17 puede volver a ofrecerlo si sigue siendo válido
                TourSuggestionEvent.ManeuverWins -> _state.value = reduce(_state.value, outcome)
                else -> { // Timeout / Dismiss: expiró o el usuario lo descartó -> se rechaza
                    reject(poi.candidate.id)
                    _state.value = reduce(_state.value, outcome)
                }
            }
            if (outcome != null && isTourActive(session.value)) delay(cooldown)
        }
    }

    /** Suspende hasta que la mejor sugerencia puede ofrecerse (puerta de perfil, maniobra y "una a la vez"). */
    private suspend fun awaitOfferable(
        session: StateFlow<NavigationSession>,
        best: StateFlow<PoiEvaluation?>,
        prog: StateFlow<NavigationProgress?>,
        detour: StateFlow<Boolean>,
    ): PoiEvaluation =
        combine(session, best, prog, detour) { s, b, p, inDetour ->
            // paso 20: durante un desvío no se ofrecen sugerencias nuevas; se reanudan al volver a la ruta principal
            b?.takeIf { !inDetour && canOfferSuggestion(TourSuggestion.Inactive, s, p, thresholds) }
        }.filterNotNull().first()

    /**
     * Espera a que una sugerencia se resuelva: acción del usuario, expiración del tiempo de ajustes (leído aquí, no
     * copiado), maniobra inminente o fin del trayecto (null). El primero gana; el resto se cancela.
     */
    private suspend fun awaitOutcome(
        poi: PoiEvaluation,
        session: StateFlow<NavigationSession>,
        prog: StateFlow<NavigationProgress?>,
        settings: UserSettings,
    ): TourSuggestionEvent? = coroutineScope {
        val result = CompletableDeferred<TourSuggestionEvent?>()
        val jobs = listOf(
            launch { result.complete(actions.receive()) },
            launch { prog.first { maneuverImminent(it, thresholds) }; result.complete(TourSuggestionEvent.ManeuverWins) },
            launch { session.first { !isTourActive(it) }; result.complete(null) },
            launch { delay(settings.suggestionTimeoutSeconds.seconds); result.complete(TourSuggestionEvent.Timeout) },
        )
        val outcome = result.await()
        jobs.forEach { it.cancel() }
        outcome
    }

    private fun drainStaleActions() {
        while (actions.tryReceive().isSuccess) Unit
    }

    private fun isTourActive(session: NavigationSession): Boolean =
        session.navigatingRoute != null && session.navigationProfile == NavigationProfile.TOUR
}
