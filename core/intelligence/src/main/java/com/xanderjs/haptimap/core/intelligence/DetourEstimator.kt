package com.xanderjs.haptimap.core.intelligence

import com.xanderjs.haptimap.core.model.MeasuredPath
import com.xanderjs.haptimap.core.model.Route
import com.xanderjs.haptimap.core.model.distanceTo
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** El coste de desviarse a un POI: cuánto se aleja de la ruta, los metros extra y el tiempo extra. */
data class DetourEstimate(
    val distanceFromRouteMeters: Double,
    val detourMeters: Double,
    val addedTime: Duration,
)

/**
 * Estima el desvío hasta un POI (paso 18). Detrás de una interfaz para sustituirlo luego por una
 * estimación real de la Routes API sin tocar la lógica de evaluación.
 */
fun interface DetourEstimator {
    /** [fromAlongMeters] es el avance actual sobre la ruta (el tramo activo): no se mira hacia atrás. */
    fun estimate(candidate: PoiCandidate, route: Route, fromAlongMeters: Double): DetourEstimate
}

/**
 * Estimación geométrica sin llamar a ninguna API (paso 18): proyecta el POI sobre la polilínea de la
 * ruta, a partir del avance actual, y aproxima el desvío como ir y volver (2 × la distancia recta del
 * POI a la ruta). El tiempo extra sale de [PoiEvaluationConfig.walkingSpeedMetersPerSecond].
 *
 * shortcut: ir y volver en línea recta; sustituir por una ruta real (Routes API) cuando haya clave.
 */
class GeometricDetourEstimator(private val config: PoiEvaluationConfig = PoiEvaluationConfig()) : DetourEstimator {
    override fun estimate(candidate: PoiCandidate, route: Route, fromAlongMeters: Double): DetourEstimate {
        val perpendicular = distanceToRouteAhead(route, candidate, fromAlongMeters)
        val detour = 2 * perpendicular
        val time = (detour / config.walkingSpeedMetersPerSecond).seconds
        return DetourEstimate(perpendicular, detour, time)
    }

    // distancia recta del POI a la ruta sin mirar antes del avance: un POI detrás proyecta en la
    // posición actual (pointAt(fromAlong)), así que arrastra la distancia de volver sobre los pasos
    private fun distanceToRouteAhead(route: Route, candidate: PoiCandidate, fromAlongMeters: Double): Double {
        if (route.points.isEmpty()) return candidate.location.distanceTo(route.destination.location)
        val path = MeasuredPath(route.points)
        if (path.segmentCount == 0) return candidate.location.distanceTo(route.points.first())
        val from = fromAlongMeters.coerceIn(0.0, path.length)
        var best = candidate.location.distanceTo(path.pointAt(from))
        for (segment in path.segmentAt(from)..path.segmentCount - 1) {
            val (along, distance) = path.project(candidate.location, segment)
            if (along >= from) best = min(best, distance)
        }
        return best
    }
}
