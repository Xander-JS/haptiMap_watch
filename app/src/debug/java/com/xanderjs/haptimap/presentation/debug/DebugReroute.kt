package com.xanderjs.haptimap.presentation.debug

import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.Maneuver
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.Route
import com.xanderjs.haptimap.core.model.RouteError
import com.xanderjs.haptimap.core.model.RouteResult
import com.xanderjs.haptimap.core.model.bearingTo
import com.xanderjs.haptimap.core.model.distanceTo
import com.xanderjs.haptimap.core.network.RoutesRepository
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Lo que tarda la petición falsa: deja ver Recalculating y volver a la ruta con ella en curso. */
private val FAKE_LATENCY = 2.seconds

// tramos de menos de esto no merecen paso propio
private const val MIN_LEG_METERS = 5.0

/**
 * Las rutas (pasos 08 y 11). Con clave, las de Google; sin clave, en debug, una ruta falsa ([debugReroute])
 * para enseñar el resumen, la navegación y el recálculo enteros. La espera no se cancela, como una respuesta
 * HTTP que ya está llegando: así se ve en el emulador que el reductor ignora un RouteRecalculated tardío.
 */
fun debugRoutes(routes: RoutesRepository, language: () -> String): RoutesRepository =
    if (routes.hasApiKey) routes else object : RoutesRepository {
        // dice que hay clave: si no, el resumen de ruta la rechaza (MISSING_KEY) antes de pedir
        override val hasApiKey = true

        override suspend fun computeRoute(origin: GeoPoint?, destination: Place, alternatives: Boolean): RouteResult {
            withContext(NonCancellable) { delay(FAKE_LATENCY) }
            return origin?.let { RouteResult.Success(debugReroute(it, destination, language())) } ?: RouteResult.Failure(RouteError.NO_LOCATION)
        }
    }

/**
 * Una ruta plausible de [origin] a [destination]: en L, primero este-oeste y después norte-sur, con un
 * giro (o recta si un tramo es muy corto). No sigue calles: solo para depurar sin clave.
 */
fun debugReroute(origin: GeoPoint, destination: Place, language: String): Route {
    val corner = GeoPoint(origin.latitude, destination.location.longitude)
    val targets = listOf(corner, destination.location).filterIndexed { i, target ->
        val from = if (i == 0) origin else corner
        from.distanceTo(target) >= MIN_LEG_METERS
    }.ifEmpty { listOf(destination.location) }
    var from = origin
    var previousBearing: Double? = null
    val legs = targets.map { target ->
        val bearing = from.bearingTo(target)
        val maneuver = when (val turn = previousBearing?.let { (bearing - it + 360) % 360 }) {
            null -> Maneuver.CONTINUE
            else -> if (turn < 180) Maneuver.TURN_RIGHT else Maneuver.TURN_LEFT
        }
        val leg = SampleLeg(maneuver, bearing, from.distanceTo(target), english(maneuver, destination.name), spanish(maneuver, destination.name))
        from = target
        previousBearing = bearing
        leg
    }
    return routeAlong(origin, legs, language) { _, _ -> destination }
}

private fun english(maneuver: Maneuver, name: String) = when (maneuver) {
    Maneuver.TURN_LEFT -> "Turn left toward $name"
    Maneuver.TURN_RIGHT -> "Turn right toward $name"
    else -> "Head toward $name"
}

private fun spanish(maneuver: Maneuver, name: String) = when (maneuver) {
    Maneuver.TURN_LEFT -> "Gira a la izquierda hacia $name"
    Maneuver.TURN_RIGHT -> "Gira a la derecha hacia $name"
    else -> "Ve hacia $name"
}
