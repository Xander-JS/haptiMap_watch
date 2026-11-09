package com.xanderjs.haptimap.feature.navigationui

import com.xanderjs.haptimap.core.model.DisplayMode
import com.xanderjs.haptimap.core.model.ExplorationCategory
import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.navigation.NavigationProgress
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Lo que se dibuja en el mapa continuo (paso 22). Puro, sin el SDK de Maps: lo produce [navigationMapState] a
 * partir de datos que YA existen (la ruta activa, el avance del motor, el desvío y la sugerencia del Tour), nunca
 * de nuevas llamadas a la red. El Composable [GoogleNavigationMap] solo lo pinta.
 */
data class NavigationMapState(
    /** Geometría de la ruta que se sigue (paso 05): la polilínea principal. */
    val route: List<GeoPoint>,
    /** El tramo del desvío (actual → POI → destino) cuando hay un desvío activo (paso 20); null si no. */
    val detourLeg: List<GeoPoint>?,
    val destination: GeoPoint,
    /** Último fix bueno (paso 07); null hasta el primero. */
    val userPosition: GeoPoint?,
    /** Rumbo del marcador del usuario: hacia la próxima maniobra. Más estable que el rumbo del GPS andando despacio. */
    val userBearingDegrees: Float?,
    /** POI sugerido o aceptado del Tour (pasos 19/20), con los datos ya descargados en el paso 17; null fuera del Tour. */
    val poi: MapMarkerUi?,
)

/** Un marcador con su texto para TalkBack / la etiqueta del mapa y su categoría (elige el icono del design system). */
data class MapMarkerUi(val position: GeoPoint, val title: String, val category: ExplorationCategory?)

/**
 * Compone el estado del mapa. Null sin ruta activa (no hay nada que seguir). El desvío manda sobre la sugerencia
 * para el POI que se marca: si hay un desvío en curso, es ese POI.
 */
fun navigationMapState(
    active: ActiveRoute?,
    progress: NavigationProgress?,
    detour: TourDetour,
    suggestion: TourSuggestion,
): NavigationMapState? {
    val route = active?.route ?: return null
    val poiEval = detour.poi ?: (suggestion as? TourSuggestion.Showing)?.poi ?: (suggestion as? TourSuggestion.Accepted)?.poi
    val position = progress?.position
    return NavigationMapState(
        route = route.points,
        detourLeg = detour.detourRoute?.points,
        destination = route.destination.location,
        userPosition = position,
        userBearingDegrees = position?.let { bearingBetween(it, progress.nextManeuver.end) },
        poi = poiEval?.candidate?.let { MapMarkerUi(it.location, it.name, it.category) },
    )
}

/** Qué vista usa la navegación. */
enum class NavigationDisplay { ECO, MAP }

/**
 * Elige entre Eco/AOD (paso 12) y el mapa continuo (paso 22). El mapa solo cuando el usuario lo pidió y se puede:
 * en ambiente vuelve a Eco (no sigue renderizando, paso 12) y sin red también (las teselas no cargan, y la
 * navegación y el háptico siguen igual). Eco es el valor por defecto y el de reserva.
 */
fun navigationDisplay(mode: DisplayMode, ambient: Boolean, offline: Boolean): NavigationDisplay =
    if (mode == DisplayMode.CONTINUOUS_MAP && !ambient && !offline) NavigationDisplay.MAP else NavigationDisplay.ECO

/** Zoom a pie: una manzana o dos a la vista, suficiente para ver el próximo giro sin perder el contexto. */
const val WALKING_ZOOM = 17.5f

/** Lo que mueve el seguimiento de cámara. */
enum class MapGesture { USER_PANNED, RECENTER }

/**
 * La cámara sigue al usuario salvo que este mueva el mapa a mano; recentrar reactiva el seguimiento. Puro para
 * poder probarlo; el Composable traduce los gestos del SDK a [MapGesture].
 */
fun following(currentlyFollowing: Boolean, gesture: MapGesture): Boolean = when (gesture) {
    MapGesture.USER_PANNED -> false
    MapGesture.RECENTER -> true
}

/** Rumbo inicial (great-circle) de [from] a [to], en grados [0,360). Norte = 0, sentido horario. */
fun bearingBetween(from: GeoPoint, to: GeoPoint): Float {
    val lat1 = Math.toRadians(from.latitude)
    val lat2 = Math.toRadians(to.latitude)
    val dLon = Math.toRadians(to.longitude - from.longitude)
    val y = sin(dLon) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
    return ((Math.toDegrees(atan2(y, x)) + 360.0) % 360.0).toFloat()
}
