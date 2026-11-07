package com.xanderjs.haptimap.feature.navigationui

import com.xanderjs.haptimap.core.intelligence.PoiEvaluation
import com.xanderjs.haptimap.core.model.Route

/**
 * Máquina de estados del desvío a un POI del Modo Tour (paso 20), como función pura. Se superpone a la navegación,
 * igual que [TourSuggestion]: arranca en la costura ACCEPTED (cuando se acepta una sugerencia) y lleva al usuario al
 * POI y luego de vuelta al destino original. Solo en TOUR; el llamador aplica las puertas y los efectos.
 *
 * NAVIGATION_ACTIVE ([Inactive]) → ACCEPTED (lo emite [TourSuggestion]) → DETOUR_LOADING ([Loading]) →
 * DETOUR_TO_POI ([ToPoi]) → POI_REACHED ([PoiReached]) → RETURNING ([Returning]) → NAVIGATION_ACTIVE / ARRIVAL
 * (de vuelta a [Inactive], lo lleva la sesión de navegación). [TourDetourEvent.Cancel] vuelve a [Inactive] desde
 * cualquier estado de desvío (CANCEL_DETOUR): el llamador repone la ruta original.
 *
 * Cada estado lleva el [poi] aceptado (para la UI y el háptico), la [originalRoute] (para cancelar sin pedir otra
 * ruta si se sigue cerca de ella) y, desde [ToPoi], la [detourRoute] que se navega (actual → POI → destino original).
 */
sealed interface TourDetour {
    /** NAVIGATION_ACTIVE: navegando el trayecto normal, sin desvío. */
    data object Inactive : TourDetour

    /** DETOUR_LOADING: aceptado el POI, pidiendo la ruta del desvío (una sola llamada, con el POI de parada). */
    data class Loading(val poi: PoiEvaluation, val originalRoute: Route) : TourDetour

    /** DETOUR_TO_POI: navegando hacia el POI por [detourRoute]. */
    data class ToPoi(val poi: PoiEvaluation, val originalRoute: Route, val detourRoute: Route) : TourDetour

    /** POI_REACHED: se ha llegado al POI (su háptico ya sonó); se continúa solo hacia el destino. */
    data class PoiReached(val poi: PoiEvaluation, val originalRoute: Route, val detourRoute: Route) : TourDetour

    /** RETURNING: tras el POI, el segundo tramo de [detourRoute] sigue hasta el destino original. */
    data class Returning(val poi: PoiEvaluation, val originalRoute: Route, val detourRoute: Route) : TourDetour
}

/** Lo que mueve la máquina del desvío. La puerta (solo TOUR navegando) y los efectos los aplica el llamador. */
sealed interface TourDetourEvent {
    /** Costura ACCEPTED: empieza el desvío al [poi], guardando la [originalRoute] para poder cancelar. */
    data class Start(val poi: PoiEvaluation, val originalRoute: Route) : TourDetourEvent

    /** La ruta del desvío (actual → POI → destino) ya está: DETOUR_LOADING → DETOUR_TO_POI. */
    data class Loaded(val detourRoute: Route) : TourDetourEvent

    /** No se pudo calcular el desvío: vuelve a [TourDetour.Inactive] (se sigue el trayecto original). */
    data object LoadFailed : TourDetourEvent

    /** Se ha alcanzado el POI: DETOUR_TO_POI → POI_REACHED. */
    data object ReachPoi : TourDetourEvent

    /** Se continúa hacia el destino (automático al alejarse del POI): POI_REACHED → RETURNING. */
    data object Resume : TourDetourEvent

    /** El trayecto original se reanuda o se llega: RETURNING → [TourDetour.Inactive]. */
    data object Finish : TourDetourEvent

    /** CANCEL_DETOUR: el usuario cancela desde cualquier estado de desvío; el llamador repone la ruta original. */
    data object Cancel : TourDetourEvent
}

/**
 * Transición pura. Un evento que no aplica al estado actual devuelve el mismo estado; nunca lanza.
 * [TourDetourEvent.Cancel] vuelve a [TourDetour.Inactive] desde cualquier estado de desvío.
 */
fun reduce(state: TourDetour, event: TourDetourEvent): TourDetour = when (event) {
    is TourDetourEvent.Start -> if (state is TourDetour.Inactive) TourDetour.Loading(event.poi, event.originalRoute) else state
    is TourDetourEvent.Loaded -> if (state is TourDetour.Loading) TourDetour.ToPoi(state.poi, state.originalRoute, event.detourRoute) else state
    TourDetourEvent.LoadFailed -> if (state is TourDetour.Loading) TourDetour.Inactive else state
    TourDetourEvent.ReachPoi -> if (state is TourDetour.ToPoi) TourDetour.PoiReached(state.poi, state.originalRoute, state.detourRoute) else state
    TourDetourEvent.Resume -> if (state is TourDetour.PoiReached) TourDetour.Returning(state.poi, state.originalRoute, state.detourRoute) else state
    TourDetourEvent.Finish -> if (state is TourDetour.Returning) TourDetour.Inactive else state
    TourDetourEvent.Cancel -> if (state is TourDetour.Inactive) state else TourDetour.Inactive
}

/** Hay un desvío en curso (cualquier estado menos [TourDetour.Inactive]): las sugerencias se pausan (paso 20). */
val TourDetour.active: Boolean get() = this !is TourDetour.Inactive

/** El POI del desvío (para la tarjeta y el háptico); null en [TourDetour.Inactive]. */
val TourDetour.poi: PoiEvaluation?
    get() = when (this) {
        is TourDetour.Loading -> poi
        is TourDetour.ToPoi -> poi
        is TourDetour.PoiReached -> poi
        is TourDetour.Returning -> poi
        TourDetour.Inactive -> null
    }

/** La ruta original del trayecto (para cancelar sin pedir otra si se sigue cerca); null en [TourDetour.Inactive]. */
val TourDetour.originalRoute: Route?
    get() = when (this) {
        is TourDetour.Loading -> originalRoute
        is TourDetour.ToPoi -> originalRoute
        is TourDetour.PoiReached -> originalRoute
        is TourDetour.Returning -> originalRoute
        TourDetour.Inactive -> null
    }

/** La ruta del desvío que se navega (actual → POI → destino); null mientras aún se calcula o no hay desvío. */
val TourDetour.detourRoute: Route?
    get() = when (this) {
        is TourDetour.ToPoi -> detourRoute
        is TourDetour.PoiReached -> detourRoute
        is TourDetour.Returning -> detourRoute
        is TourDetour.Loading, TourDetour.Inactive -> null
    }
