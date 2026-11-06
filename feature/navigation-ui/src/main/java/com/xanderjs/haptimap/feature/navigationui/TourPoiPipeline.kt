package com.xanderjs.haptimap.feature.navigationui

import com.xanderjs.haptimap.core.intelligence.PoiCandidate
import com.xanderjs.haptimap.core.intelligence.PoiEvaluation
import com.xanderjs.haptimap.core.intelligence.PoiMapper
import com.xanderjs.haptimap.core.intelligence.RejectedPoiMemory
import com.xanderjs.haptimap.core.intelligence.poiEvaluator
import com.xanderjs.haptimap.core.intelligence.suggestions
import com.xanderjs.haptimap.core.intelligence.toPoiEvaluationConfig
import com.xanderjs.haptimap.core.model.ConnectivityPolicy
import com.xanderjs.haptimap.core.model.ConnectivityStatus
import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.MeasuredPath
import com.xanderjs.haptimap.core.model.NavigationProfile
import com.xanderjs.haptimap.core.model.Route
import com.xanderjs.haptimap.core.model.UserSettings
import com.xanderjs.haptimap.core.navigation.NavigationProgress
import com.xanderjs.haptimap.core.navigation.NavigationSession
import com.xanderjs.haptimap.core.navigation.navigatingRoute
import com.xanderjs.haptimap.core.navigation.navigationProfile
import com.xanderjs.haptimap.core.network.ApiCallBudget
import com.xanderjs.haptimap.core.network.PlaceDto
import com.xanderjs.haptimap.core.network.PoiFetch
import com.xanderjs.haptimap.core.network.PoiSource
import com.xanderjs.haptimap.core.network.explorationCategoryForPlaceType
import java.util.logging.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Convierte un resultado de Places (New) en un [PoiCandidate] del Modo Tour (paso 17), la costura del paso 18.
 * La categoría de exploración sale del primaryType ([explorationCategoryForPlaceType]); null si Places no la mapea,
 * y entonces la tubería lo descarta antes de evaluar.
 */
val placeDtoPoiMapper = PoiMapper<PlaceDto> { dto ->
    PoiCandidate(
        id = dto.id,
        name = dto.displayName?.text.orEmpty(),
        category = explorationCategoryForPlaceType(dto.primaryType),
        location = GeoPoint(dto.location.latitude, dto.location.longitude),
    )
}

/**
 * Tubería de POIs del Modo Tour (paso 17): mientras se navega en Tour, pide POIs reales por delante en la ruta,
 * los convierte con el PoiMapper y los evalúa con el PoiEvaluator; publica un flujo de sugerencias (solo SUGGEST,
 * ordenadas) que consumirá el paso 19. No avisa al usuario todavía.
 *
 * Disparo (coste/batería): solo consulta cuando el avance supera [queryAdvanceMeters] o cambia el tramo activo,
 * nunca en cada fix. Caché por trayecto con deduplicado por id; se vacía al terminar. En FOCUS no consulta nada.
 * Sin red suspende las consultas (la navegación no se toca) y reanuda al volver. El radio sale de la tolerancia al
 * desvío de ajustes, así que nunca se busca más lejos de lo que el evaluador aceptaría. Respeta [RejectedPoiMemory].
 */
class TourPoiPipeline(
    private val poiSource: PoiSource,
    private val sessions: Flow<NavigationSession>,
    private val progress: Flow<NavigationProgress?>,
    private val connectivity: Flow<ConnectivityStatus>,
    private val settings: Flow<UserSettings>,
    private val budget: ApiCallBudget,
    private val languageCode: () -> String,
    private val mapper: PoiMapper<PlaceDto> = placeDtoPoiMapper,
    private val debug: Boolean = false,
    private val queryAdvanceMeters: Double = QUERY_ADVANCE_METERS,
    private val lookAheadMeters: Double = LOOK_AHEAD_METERS,
) {
    private val _suggestions = MutableStateFlow<List<PoiEvaluation>>(emptyList())

    /** Sugerencias del trayecto, ordenadas (solo SUGGEST). Vacía fuera del Tour o al terminar. Para el paso 19. */
    val suggestions: StateFlow<List<PoiEvaluation>> = _suggestions.asStateFlow()

    // estado del trayecto: un único flujo lo toca, sin sincronizar (como RejectedPoiMemory)
    private val cache = LinkedHashMap<String, PlaceDto>()
    private val rejected = RejectedPoiMemory()
    private var lastQueryAlong = Double.NEGATIVE_INFINITY
    private var lastStepIndex = -1

    /** Rechaza un POI durante el trayecto (lo usará el paso 19); no se vuelve a sugerir hasta terminar. */
    fun reject(id: String) = rejected.reject(id)

    suspend fun run() {
        // un trayecto Tour está activo mientras hay ruta navegándose y el perfil es TOUR; distinct para no reiniciar
        // la caché en cada subcambio del mismo trayecto (NavigationActive <-> OffRoute <-> Recalculating)
        sessions.map { it.navigatingRoute != null && it.navigationProfile == NavigationProfile.TOUR }
            .distinctUntilChanged()
            .collectLatest { active ->
                if (!active) {
                    clearTrip()
                    return@collectLatest
                }
                startTrip()
                runTripQueries()
            }
    }

    private fun startTrip() {
        cache.clear()
        rejected.clear()
        lastQueryAlong = Double.NEGATIVE_INFINITY
        lastStepIndex = -1
        budget.startTrip()
        _suggestions.value = emptyList()
    }

    private fun clearTrip() {
        cache.clear()
        rejected.clear()
        _suggestions.value = emptyList()
    }

    private suspend fun runTripQueries() {
        combine(sessions, progress, connectivity, settings) { session, p, conn, s -> Snapshot(session, p, conn, s) }
            .collect { (session, p, conn, s) ->
                val route = session.navigatingRoute ?: return@collect
                val currentProgress = p ?: return@collect
                val path = MeasuredPath(route.points)
                if (path.length <= 0.0) return@collect
                val along = (path.length - currentProgress.remainingMeters).coerceIn(0.0, path.length)

                val advanced = along - lastQueryAlong >= queryAdvanceMeters
                val segmentChanged = currentProgress.stepIndex != lastStepIndex
                if (!advanced && !segmentChanged) return@collect
                // sin red: se suspende sin marcar el disparo, así se reintenta al volver la conexión o avanzar
                if (!ConnectivityPolicy.canFetchPois(conn)) return@collect

                val config = s.detourTolerance.toPoiEvaluationConfig()
                // radio = corredor de la tolerancia al desvío: nunca más lejos de lo que el evaluador aceptaría (TOO_FAR)
                val radius = config.maxDistanceFromRouteMeters
                val center = path.pointAt((along + lookAheadMeters).coerceAtMost(path.length))

                when (val fetch = poiSource.searchAhead(center, radius, languageCode())) {
                    is PoiFetch.Found -> {
                        fetch.places.forEach { cache[it.id] = it } // dedup por id
                        markQueried(along, currentProgress.stepIndex)
                        evaluateAndEmit(route, along, s)
                    }
                    // consulta hecha pero sin nada que añadir: consume el disparo para no repetir la misma consulta
                    PoiFetch.Empty, PoiFetch.BudgetReached -> markQueried(along, currentProgress.stepIndex)
                    // sin red o fallo puntual: no se consume el disparo; se reintentará al avanzar o reconectar
                    PoiFetch.Offline, is PoiFetch.Failed -> Unit
                }
            }
    }

    private fun markQueried(along: Double, stepIndex: Int) {
        lastQueryAlong = along
        lastStepIndex = stepIndex
    }

    private fun evaluateAndEmit(route: Route, along: Double, settings: UserSettings) {
        // descarta los que Places no mapea a ninguna categoría de exploración, antes de evaluar
        val candidates = cache.values.map(mapper::toCandidate).filter { it.category != null }
        val evaluations = poiEvaluator(settings).evaluate(candidates, route, along, NavigationProfile.TOUR, rejected)
        _suggestions.value = evaluations.suggestions()
        if (debug) {
            logger.info("Evaluated ${candidates.size} POIs @${along.toInt()}m | ${budget.debugSummary()}")
            evaluations.forEach {
                val verdict = it.discardReason?.name ?: "SUGGEST score=%.2f".format(it.score)
                logger.info("  ${it.candidate.name} [${it.candidate.category}] -> $verdict")
            }
        }
    }

    private data class Snapshot(
        val session: NavigationSession,
        val progress: NavigationProgress?,
        val connectivity: ConnectivityStatus,
        val settings: UserSettings,
    )

    companion object {
        /** Se vuelve a consultar al avanzar esto sobre la ruta; con el look-ahead cubre el trayecto solapándose. */
        const val QUERY_ADVANCE_METERS = 120.0

        /** El centro de la búsqueda va este tanto por delante del avance actual, para sugerir con margen. */
        const val LOOK_AHEAD_METERS = 150.0

        private val logger: Logger = Logger.getLogger("HaptiPois")
    }
}
