package com.xanderjs.haptimap.feature.navigationui

import com.xanderjs.haptimap.core.intelligence.PoiEvaluation
import com.xanderjs.haptimap.core.model.NavigationProfile
import com.xanderjs.haptimap.core.navigation.NavigationProgress
import com.xanderjs.haptimap.core.navigation.NavigationSession
import com.xanderjs.haptimap.core.navigation.NavigationThresholds
import com.xanderjs.haptimap.core.navigation.navigatingRoute
import com.xanderjs.haptimap.core.navigation.navigationProfile
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Máquina de estados de las sugerencias de POI del Modo Tour (paso 19), como función pura. Se superpone a la
 * navegación: mientras se navega el estado es [Inactive] (NAVIGATION_ACTIVE) y, cuando una sugerencia pasa la
 * puerta ([canOfferSuggestion]), se muestra una sola a la vez.
 *
 * NAVIGATION_ACTIVE ([Inactive]) → POI_SUGGESTION ([Showing]) → { [Accepted] | vuelta a [Inactive] por timeout,
 * descarte o maniobra }. [Accepted] es la costura del paso 20 (DETOUR): hoy el controlador la recoge con [Consumed]
 * y vuelve a [Inactive]; cuando exista el paso 20, tomará el relevo ahí en vez de volver.
 */
sealed interface TourSuggestion {
    /** NAVIGATION_ACTIVE: navegando, sin sugerencia en pantalla. */
    data object Inactive : TourSuggestion

    /** POI_SUGGESTION: se muestra [poi] (la mejor del flujo del paso 17) y corre su espera. */
    data class Showing(val poi: PoiEvaluation) : TourSuggestion

    /**
     * El usuario la aceptó (botón físico o acción en pantalla). Estado de traspaso: aquí arrancará el paso 20
     * (DETOUR). TODO(paso 20): empezar el desvío al POI en vez de que el controlador emita [Consumed] y vuelva.
     */
    data class Accepted(val poi: PoiEvaluation) : TourSuggestion
}

/** Lo que mueve la máquina de sugerencias. La puerta ([canOfferSuggestion]) y el cooldown los aplica el llamador. */
sealed interface TourSuggestionEvent {
    /** Una sugerencia ha pasado la puerta y debe mostrarse. */
    data class Offer(val poi: PoiEvaluation) : TourSuggestionEvent
    data object Accept : TourSuggestionEvent
    data object Dismiss : TourSuggestionEvent

    /** Expiró la espera configurada en ajustes ([com.xanderjs.haptimap.core.model.UserSettings.suggestionTimeoutSeconds]). */
    data object Timeout : TourSuggestionEvent

    /** Una maniobra se volvió inminente mientras se mostraba: la navegación manda y la sugerencia se retira. */
    data object ManeuverWins : TourSuggestionEvent

    /** El paso 20 (hoy el controlador) ha recogido la aceptación: vuelve a [TourSuggestion.Inactive]. */
    data object Consumed : TourSuggestionEvent
}

/**
 * Transición pura. Un evento que no aplica al estado actual devuelve el mismo estado; nunca lanza.
 * Solo se muestra una sugerencia a la vez: un [TourSuggestionEvent.Offer] con otra ya en curso se ignora
 * (el controlador elige antes la mejor del flujo, no encola avisos).
 */
fun reduce(state: TourSuggestion, event: TourSuggestionEvent): TourSuggestion = when (event) {
    is TourSuggestionEvent.Offer -> if (state is TourSuggestion.Inactive) TourSuggestion.Showing(event.poi) else state
    TourSuggestionEvent.Accept -> if (state is TourSuggestion.Showing) TourSuggestion.Accepted(state.poi) else state
    TourSuggestionEvent.Dismiss,
    TourSuggestionEvent.Timeout,
    TourSuggestionEvent.ManeuverWins,
    -> if (state is TourSuggestion.Showing) TourSuggestion.Inactive else state
    TourSuggestionEvent.Consumed -> if (state is TourSuggestion.Accepted) TourSuggestion.Inactive else state
}

/** El POI de un estado con sugerencia (mostrada o aceptada), para rechazarlo al cerrarse; null en [TourSuggestion.Inactive]. */
val TourSuggestion.suggestedPoi: PoiEvaluation?
    get() = when (this) {
        is TourSuggestion.Showing -> poi
        is TourSuggestion.Accepted -> poi
        TourSuggestion.Inactive -> null
    }

/**
 * Si puede ofrecerse una sugerencia ahora (paso 19). Reglas que no dependen del tiempo (el cooldown lo lleva el
 * controlador con el reloj, [SUGGESTION_COOLDOWN]):
 *  - solo en TOUR y navegando: en FOCUS no hay sugerencias (el flujo del paso 17 ya llega vacío, pero se guarda igual);
 *  - la navegación manda: nada si hay una maniobra dentro del radio de anticipación ([maneuverImminent]);
 *  - una a la vez: solo desde [TourSuggestion.Inactive].
 */
fun canOfferSuggestion(
    state: TourSuggestion,
    session: NavigationSession,
    progress: NavigationProgress?,
    thresholds: NavigationThresholds,
): Boolean =
    state is TourSuggestion.Inactive &&
        session.navigatingRoute != null &&
        session.navigationProfile == NavigationProfile.TOUR &&
        !maneuverImminent(progress, thresholds)

/**
 * Si hay una maniobra lo bastante cerca como para que una sugerencia estorbe (paso 19). Se usa el radio de
 * aproximación ([NavigationThresholds.approachMeters], el anillo exterior, ~18 m), no el de "inminente" (~6 m):
 * así la tarjeta nunca aparece mientras el usuario ya está entrando en el giro, y una maniobra que cruza ese
 * radio con una sugerencia en pantalla la retira ([TourSuggestionEvent.ManeuverWins]).
 */
fun maneuverImminent(progress: NavigationProgress?, thresholds: NavigationThresholds): Boolean =
    progress != null && progress.distanceToManeuverMeters <= thresholds.approachMeters

/**
 * Silencio mínimo entre el fin de una sugerencia y la siguiente (paso 19), en un solo sitio: evita una ráfaga de
 * tarjetas cuando hay muchos POIs seguidos. El controlador lo cuenta con el reloj tras cerrar cada sugerencia.
 */
val SUGGESTION_COOLDOWN: Duration = 30.seconds
