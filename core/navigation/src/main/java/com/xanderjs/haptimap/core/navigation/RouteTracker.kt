package com.xanderjs.haptimap.core.navigation

import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.LocationFix
import com.xanderjs.haptimap.core.model.MeasuredPath
import com.xanderjs.haptimap.core.model.Route
import com.xanderjs.haptimap.core.model.distanceTo
import com.xanderjs.haptimap.core.navigation.NavigationCue.ManeuverApproaching
import com.xanderjs.haptimap.core.navigation.NavigationCue.ManeuverImminent
import com.xanderjs.haptimap.core.navigation.NavigationCue.ManeuverPassed
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration

/** Lo que produce un fix: el avance y los avisos que ha cruzado, en orden. */
data class TrackingUpdate(val progress: NavigationProgress, val cues: List<NavigationCue>)

/**
 * Sigue al usuario por [route] fix a fix (paso 07). Sin E/S ni reloj propio: el tiempo sale de los fixes,
 * así que se prueba reproduciendo recorridos. Ver ARCHITECTURE.md, "Navigation Engine".
 *
 * Map matching: cada fix bueno se proyecta sobre la polilínea, solo en una ventana alrededor del avance
 * actual (no en toda la ruta, que puede pasar cerca de sí misma), y el avance nunca retrocede. El
 * primer fix busca en toda la ruta: al retomar tras la muerte del proceso el usuario ya va por la mitad.
 *
 * Desvíos (paso 08): un fix bueno a más de NavigationThresholds.offRouteMeters (más su precisión) está
 * fuera; una racha de fuera declara el desvío (cue OffRoute) y otra de cerca, con un umbral menor, la
 * vuelta (BackOnRoute). Un fix fuera, o cualquiera durante el desvío, no mueve el avance ni da avisos de maniobra.
 */
class RouteTracker(private val route: Route, private val thresholds: NavigationThresholds = NavigationThresholds()) {
    private val path = MeasuredPath(route.points)
    private val last = route.steps.lastIndex

    // posición a lo largo de la ruta de la maniobra de cada paso; la 0 es la salida y la última, el final
    private val maneuverAt = locateManeuvers()
    private val reached = IntArray(route.steps.size)
    private var along = 0.0
    // posición y velocidad filtradas a lo largo de la ruta (filtro alfa-beta): el avance es su máximo, no
    // el de los fixes en bruto, que con ruido solo puede adelantarse y daba las maniobras por hechas antes de llegar
    private var estimate = 0.0
    private var speed = 0.0
    private var matched = false
    private var arrived = false
    private var lastGoodMillis: Long? = null
    // el último fix que movió el avance: el filtro y la ventana cuentan desde él, también tras un desvío
    private var lastMatchedMillis: Long? = null
    private var firstFixMillis: Long? = null
    private var position: GeoPoint? = null
    private var offRoute = false
    private var offSinceMillis: Long? = null
    private var offFixes = 0
    private var nearFixes = 0
    // tras fixes fuera de la ruta la velocidad filtrada no vale para predecir: el filtro arranca del fix
    private var resync = false

    var progress: NavigationProgress = progressAt(crossTrack = null, accuracy = null, weak = false)
        private set

    fun update(fix: LocationFix): TrackingUpdate {
        if (arrived) return TrackingUpdate(progress, emptyList())
        val since = lastGoodMillis ?: firstFixMillis ?: fix.timestampMillis.also { firstFixMillis = it }
        if (fix.accuracyMeters > thresholds.maxAccuracyMeters) {
            val weak = fix.timestampMillis - since >= thresholds.weakSignalAfter.inWholeMilliseconds
            // con la señal débil una racha ya no es seguida: nada se declara con fixes de antes del hueco
            if (weak) resetStreaks()
            progress = progress.copy(fixAccuracyMeters = fix.accuracyMeters, weakSignal = weak)
            return TrackingUpdate(progress, emptyList())
        }
        val (candidate, crossTrack) = match(fix)
        lastGoodMillis = fix.timestampMillis
        position = fix.point
        val allowance = min(fix.accuracyMeters.toDouble(), thresholds.offRouteAccuracyAllowanceMeters)
        val off = crossTrack > thresholds.offRouteMeters + allowance
        val cues = listOfNotNull(offRouteCue(off, crossTrack <= thresholds.backOnRouteMeters + allowance, fix.timestampMillis)).toMutableList()
        val onRoute = !off && !offRoute
        if (onRoute) {
            val firstMatch = !matched
            filter(candidate, fix.timestampMillis)
            along = max(along, estimate)
            matched = true
            lastMatchedMillis = fix.timestampMillis
            // al retomar a mitad de ruta, las maniobras ya hechas no se avisan
            if (firstMatch) for (k in 1 until last) if (along >= maneuverAt[k] + thresholds.passedMeters) reached[k] = PASSED
        } else {
            resync = true
        }
        cues += cues(fix, candidate, onRoute)
        progress = progressAt(crossTrack, fix.accuracyMeters, weak = false)
        return TrackingUpdate(progress, cues)
    }

    /** Sin ningún fix en NavigationThresholds.weakSignalAfter: el llamador lo cronometra (el tracker no tiene reloj). */
    fun signalLost(): NavigationProgress {
        if (!arrived) {
            resetStreaks()
            progress = progress.copy(weakSignal = true)
        }
        return progress
    }

    // las rachas solo cuentan fixes buenos seguidos; uno malo suelto ni suma ni corta
    private fun offRouteCue(off: Boolean, near: Boolean, millis: Long): NavigationCue? {
        if (!offRoute) {
            if (!off) {
                resetStreaks()
                return null
            }
            val since = offSinceMillis ?: millis.also { offSinceMillis = it }
            if (++offFixes < thresholds.offRouteMinFixes || millis - since < thresholds.offRouteAfter.inWholeMilliseconds) return null
            offRoute = true
            resetStreaks()
            return NavigationCue.OffRoute
        }
        if (!near) {
            nearFixes = 0
            return null
        }
        if (++nearFixes < thresholds.backOnRouteFixes) return null
        offRoute = false
        resetStreaks()
        return NavigationCue.BackOnRoute
    }

    private fun resetStreaks() {
        offSinceMillis = null
        offFixes = 0
        nearFixes = 0
    }

    private fun match(fix: LocationFix): Pair<Double, Double> {
        if (path.segmentCount == 0) return 0.0 to fix.point.distanceTo(path.points.first())
        val window = if (!matched) {
            0.0..path.length
        } else {
            val seconds = lastMatchedMillis?.let { (fix.timestampMillis - it).coerceAtLeast(0) / 1000.0 } ?: 0.0
            val ahead = min(thresholds.searchAheadMeters + thresholds.maxWalkingSpeedMetersPerSecond * seconds, thresholds.searchAheadMaxMeters)
            (along - thresholds.searchBehindMeters).coerceAtLeast(0.0)..(along + ahead)
        }
        var best = along to Double.MAX_VALUE
        for (segment in path.segmentAt(window.start)..path.segmentAt(window.endInclusive)) {
            val (position, distance) = path.project(fix.point, segment)
            val clamped = position.coerceIn(window)
            val actual = if (clamped == position) distance else fix.point.distanceTo(path.pointAt(clamped))
            if (actual < best.second) best = clamped to actual
        }
        return best
    }

    private fun filter(candidate: Double, millis: Long) {
        val seconds = lastMatchedMillis?.let { (millis - it) / 1000.0 } ?: 0.0
        if (!matched || resync || seconds <= 0) {
            if (!matched || resync) estimate = candidate
            resync = false
            return
        }
        val predicted = estimate + speed * seconds
        val residual = candidate - predicted
        estimate = predicted + thresholds.filterAlpha * residual
        speed = (speed + thresholds.filterBeta * residual / seconds).coerceIn(0.0, thresholds.maxWalkingSpeedMetersPerSecond)
    }

    // fuera de la ruta no hay avisos de maniobra; la llegada sí, salvo con el desvío declarado (pasarse del final está fuera)
    private fun cues(fix: LocationFix, candidate: Double, onRoute: Boolean): List<NavigationCue> {
        val cues = mutableListOf<NavigationCue>()
        fun reach(k: Int, level: Int, cue: NavigationCue) {
            if (reached[k] < level) {
                reached[k] = level
                cues += cue
            }
        }
        if (offRoute) return cues
        for (k in 1 until last) {
            if (!onRoute) break
            val toGo = maneuverAt[k] - along
            if (toGo > thresholds.approachMeters) break
            val maneuver = route.steps[k].maneuver
            reach(k, APPROACHING, ManeuverApproaching(k, maneuver))
            if (toGo <= thresholds.imminentMeters) reach(k, IMMINENT, ManeuverImminent(k, maneuver))
            if (-toGo >= thresholds.passedMeters) reach(k, PASSED, ManeuverPassed(k, maneuver))
        }
        // con el fix en bruto: pasarse del final lo deja en el final y es llegar sin esperar al filtro
        val remaining = path.length - max(along, candidate)
        val arrivedNow = remaining <= thresholds.arrivalRadiusMeters + min(fix.accuracyMeters.toDouble(), thresholds.arrivalAccuracyAllowanceMeters)
        if (last > 0 && (remaining <= thresholds.approachMeters || arrivedNow)) reach(last, APPROACHING, ManeuverApproaching(last, route.steps[last].maneuver))
        if (arrivedNow) {
            along = max(along, candidate)
            arrived = true
            cues += NavigationCue.Arrived
        }
        return cues
    }

    private fun progressAt(crossTrack: Double?, accuracy: Float?, weak: Boolean): NavigationProgress {
        // la maniobra siguiente cambia al darla por hecha (passedMeters después), igual que ManeuverPassed
        val next = (1..last).firstOrNull { along < maneuverAt[it] + thresholds.passedMeters } ?: last
        return NavigationProgress(
            stepIndex = (next - 1).coerceAtLeast(0),
            nextManeuver = route.steps[next],
            distanceToManeuverMeters = (maneuverAt[next] - along).coerceAtLeast(0.0),
            remainingMeters = (path.length - along).coerceAtLeast(0.0),
            remainingTime = remainingTime(),
            crossTrackMeters = crossTrack,
            fixAccuracyMeters = accuracy,
            weakSignal = weak,
            offRoute = offRoute,
            position = position,
        )
    }

    // la parte sin andar del paso actual más los pasos siguientes, con las duraciones del proveedor
    private fun remainingTime(): Duration {
        val current = (0 until last).lastOrNull { maneuverAt[it] <= along } ?: last
        val later = (current + 1..last).fold(Duration.ZERO) { sum, i -> sum + route.steps[i].duration }
        if (current == last) return later
        val length = maneuverAt[current + 1] - maneuverAt[current]
        val left = if (length <= 0) 0.0 else ((maneuverAt[current + 1] - along) / length).coerceIn(0.0, 1.0)
        return route.steps[current].duration * left + later
    }

    // cada maniobra está sobre la polilínea: el primer segmento que la contiene a partir de la anterior
    private fun locateManeuvers(): DoubleArray {
        val at = DoubleArray(route.steps.size)
        var from = 0
        for (i in 1..last) {
            if (i == last || path.segmentCount == 0) {
                at[i] = path.length
                continue
            }
            var best = at[i - 1] to Double.MAX_VALUE
            for (segment in from until path.segmentCount) {
                val (position, distance) = path.project(route.steps[i].start, segment)
                if (distance < best.second) {
                    best = position to distance
                    from = segment
                }
                if (distance < ON_PATH_METERS) break
            }
            at[i] = max(best.first, at[i - 1])
        }
        return at
    }

    private companion object {
        const val APPROACHING = 1
        const val IMMINENT = 2
        const val PASSED = 3
        const val ON_PATH_METERS = 1.0
    }
}
