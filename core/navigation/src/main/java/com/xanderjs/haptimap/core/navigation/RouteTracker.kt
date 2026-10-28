package com.xanderjs.haptimap.core.navigation

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
 */
class RouteTracker(private val route: Route, private val thresholds: NavigationThresholds = NavigationThresholds()) {
    private val path = MeasuredPath(route.points)
    private val last = route.steps.lastIndex

    // posición a lo largo de la ruta de la maniobra de cada paso; la 0 es la salida y la última, el final
    private val maneuverAt = locateManeuvers()
    private val reached = IntArray(route.steps.size)
    private var along = 0.0
    private var matched = false
    private var arrived = false
    private var lastGoodMillis: Long? = null
    private var firstFixMillis: Long? = null

    var progress: NavigationProgress = progressAt(crossTrack = null, accuracy = null, weak = false)
        private set

    fun update(fix: LocationFix): TrackingUpdate {
        if (arrived) return TrackingUpdate(progress, emptyList())
        val since = lastGoodMillis ?: firstFixMillis ?: fix.timestampMillis.also { firstFixMillis = it }
        if (fix.accuracyMeters > thresholds.maxAccuracyMeters) {
            val weak = fix.timestampMillis - since >= thresholds.weakSignalAfter.inWholeMilliseconds
            progress = progress.copy(fixAccuracyMeters = fix.accuracyMeters, weakSignal = weak)
            return TrackingUpdate(progress, emptyList())
        }
        val (candidate, crossTrack) = match(fix)
        val firstMatch = !matched
        along = max(along, candidate)
        matched = true
        lastGoodMillis = fix.timestampMillis
        // al retomar a mitad de ruta, las maniobras ya hechas no se avisan
        if (firstMatch) for (k in 1 until last) if (along >= maneuverAt[k] + thresholds.passedMeters) reached[k] = PASSED
        val cues = cues(fix)
        progress = progressAt(crossTrack, fix.accuracyMeters, weak = false)
        return TrackingUpdate(progress, cues)
    }

    /** Sin ningún fix en NavigationThresholds.weakSignalAfter: el llamador lo cronometra (el tracker no tiene reloj). */
    fun signalLost(): NavigationProgress {
        if (!arrived) progress = progress.copy(weakSignal = true)
        return progress
    }

    private fun match(fix: LocationFix): Pair<Double, Double> {
        if (path.segmentCount == 0) return 0.0 to fix.point.distanceTo(path.points.first())
        val window = if (!matched) {
            0.0..path.length
        } else {
            val seconds = lastGoodMillis?.let { (fix.timestampMillis - it).coerceAtLeast(0) / 1000.0 } ?: 0.0
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

    private fun cues(fix: LocationFix): List<NavigationCue> {
        val cues = mutableListOf<NavigationCue>()
        fun reach(k: Int, level: Int, cue: NavigationCue) {
            if (reached[k] < level) {
                reached[k] = level
                cues += cue
            }
        }
        for (k in 1 until last) {
            val toGo = maneuverAt[k] - along
            if (toGo > thresholds.approachMeters) break
            val maneuver = route.steps[k].maneuver
            reach(k, APPROACHING, ManeuverApproaching(k, maneuver))
            if (toGo <= thresholds.imminentMeters) reach(k, IMMINENT, ManeuverImminent(k, maneuver))
            if (-toGo >= thresholds.passedMeters) reach(k, PASSED, ManeuverPassed(k, maneuver))
        }
        val remaining = path.length - along
        // pasarse del final deja el avance en el final: también es llegar
        val arrivedNow = remaining <= thresholds.arrivalRadiusMeters + min(fix.accuracyMeters.toDouble(), thresholds.arrivalAccuracyAllowanceMeters)
        if (last > 0 && (remaining <= thresholds.approachMeters || arrivedNow)) reach(last, APPROACHING, ManeuverApproaching(last, route.steps[last].maneuver))
        if (arrivedNow) {
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
