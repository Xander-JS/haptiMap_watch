package com.xanderjs.haptimap.core.navigation

import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.LocationFix
import com.xanderjs.haptimap.core.model.Maneuver.ARRIVAL
import com.xanderjs.haptimap.core.model.Maneuver.CONTINUE
import com.xanderjs.haptimap.core.model.Maneuver.TURN_LEFT
import com.xanderjs.haptimap.core.model.MeasuredPath
import com.xanderjs.haptimap.core.model.offset
import com.xanderjs.haptimap.core.navigation.NavigationCue.BackOnRoute
import com.xanderjs.haptimap.core.navigation.NavigationCue.ManeuverApproaching
import com.xanderjs.haptimap.core.navigation.NavigationCue.ManeuverImminent
import com.xanderjs.haptimap.core.navigation.NavigationCue.ManeuverPassed
import com.xanderjs.haptimap.core.navigation.NavigationCue.OffRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Paso 08: detección de desvíos en RouteTracker, reproduciendo recorridos.
class OffRouteTest {

    // 300 m al este; el giro a 200 m deja ver si un desvío da avisos de maniobra
    private val route = routeOf(Leg(CONTINUE, 90.0, 200.0), Leg(TURN_LEFT, 0.0, 100.0))
    private val path = MeasuredPath(route.points)

    /** Fixes de 1 s en [from]..[to] metros a la izquierda (norte) del punto a [along] m, desde [startMillis]. */
    private fun sideways(along: Double, from: Double, to: Double, startMillis: Long, accuracy: Float = 5f): List<LocationFix> {
        val base = path.pointAt(along)
        val count = (kotlin.math.abs(to - from) / WALK_SPEED).toInt()
        return (0..count).map { i ->
            val meters = from + (to - from) * i / count
            fix(base.offset(0.0, meters), startMillis + i * 1000L, accuracy)
        }
    }

    private fun still(point: GeoPoint, seconds: IntRange, accuracy: Float = 5f) = seconds.map { fix(point, it * 1000L, accuracy) }

    /** El tracker tras andar hasta [along] m por la ruta; devuelve también el último instante. */
    private fun walkedTo(along: Double, thresholds: NavigationThresholds = NavigationThresholds()): Pair<RouteTracker, Long> {
        val tracker = RouteTracker(route, thresholds)
        val fixes = walk(route, to = along)
        tracker.replay(fixes)
        return tracker to fixes.last().timestampMillis
    }

    @Test
    fun `off route after 5 s beyond 20 m plus the fix accuracy`() {
        val (tracker, t) = walkedTo(100.0)
        val point = path.pointAt(100.0).offset(0.0, 26.0)
        val start = (t / 1000).toInt() + 1
        val updates = tracker.replay(still(point, start..start + 5))
        // fuera desde el primero (t + 1 s): declarado 5 s después, en el sexto
        assertEquals(listOf(OffRoute), updates.cues)
        assertTrue(OffRoute in updates[5].cues)
        assertTrue(updates[5].progress.offRoute)
        assertFalse(updates[4].progress.offRoute)
        assertEquals(point, updates[5].progress.position)
    }

    @Test
    fun `just inside the threshold is never off route`() {
        val (tracker, t) = walkedTo(100.0)
        val start = (t / 1000).toInt() + 1
        val updates = tracker.replay(still(path.pointAt(100.0).offset(0.0, 24.0), start..start + 60))
        assertTrue(updates.none { it.progress.offRoute })
        assertEquals(emptyList<NavigationCue>(), updates.cues)
    }

    @Test
    fun `a poorer fix widens the threshold up to 10 m`() {
        val (tracker, t) = walkedTo(100.0)
        val start = (t / 1000).toInt() + 1
        // 15 m de precisión: el umbral es 30 m, 28 m está dentro
        val updates = tracker.replay(still(path.pointAt(100.0).offset(0.0, 28.0), start..start + 30, accuracy = 15f))
        assertTrue(updates.none { it.progress.offRoute })
    }

    @Test
    fun `a short excursion does not declare off route`() {
        val (tracker, t) = walkedTo(100.0)
        val start = (t / 1000).toInt() + 1
        val point = path.pointAt(100.0)
        // 4 s fuera y de vuelta, dos veces: cada racha se corta antes de los 5 s
        val fixes = still(point.offset(0.0, 30.0), start..start + 4) + still(point, start + 5..start + 5) +
            still(point.offset(0.0, 30.0), start + 6..start + 10) + still(point, start + 11..start + 12)
        assertTrue(tracker.replay(fixes).none { it.progress.offRoute })
    }

    @Test
    fun `it also needs at least 3 good fixes`() {
        val (tracker, t) = walkedTo(100.0)
        val point = path.pointAt(100.0).offset(0.0, 40.0)
        val s = t + 1000
        // dos fixes con 10 s entre ellos (y sin llegar a señal débil por un fix malo entre medias)
        assertFalse(tracker.update(fix(point, s)).progress.offRoute)
        assertFalse(tracker.update(fix(point, s + 9_000)).progress.offRoute)
        assertEquals(listOf(OffRoute), tracker.update(fix(point, s + 9_500)).cues)
    }

    @Test
    fun `poor-accuracy fixes neither count nor break the streak`() {
        val (tracker, t) = walkedTo(100.0)
        val start = (t / 1000).toInt() + 1
        val far = path.pointAt(100.0).offset(0.0, 60.0)
        // 60 m fuera pero con 25 m de error: nunca cuentan
        assertTrue(tracker.replay(still(far, start..start + 8, accuracy = 25f)).none { it.progress.offRoute })
        // fixes buenos fuera con malos intercalados: la racha sigue
        val mixed = (start + 9..start + 15).map { s -> fix(far, s * 1000L, if (s % 2 == 0) 25f else 5f) }
        val updates = tracker.replay(mixed)
        assertEquals(listOf(OffRoute), updates.cues)
    }

    @Test
    fun `a weak signal resets the streak and nothing is declared while it lasts`() {
        val (tracker, t) = walkedTo(100.0)
        val start = (t / 1000).toInt() + 1
        val point = path.pointAt(100.0).offset(0.0, 40.0)
        tracker.replay(still(point, start..start + 3))
        // 11 s de fixes malos: señal débil
        val weak = tracker.replay(still(point, start + 4..start + 14, accuracy = 30f))
        assertTrue(weak.last().progress.weakSignal)
        assertTrue(weak.none { it.progress.offRoute })
        // vuelve la señal: la racha empieza de cero, 5 s más
        val back = tracker.replay(still(point, start + 15..start + 20))
        assertEquals(listOf(false, false, false, false, false, true), back.map { it.progress.offRoute })
    }

    @Test
    fun `off-route fixes do not move progress or announce maneuvers`() {
        val (tracker, t) = walkedTo(150.0)
        val before = tracker.progress.remainingMeters
        val start = (t / 1000).toInt() + 1
        // en paralelo a 30 m al sur (el giro de los 200 m va al norte), pasando por delante del giro
        val parallel = (0..60).map { i -> fix(path.pointAt(150.0).offset(90.0, i * WALK_SPEED).offset(180.0, 30.0), (start + i) * 1000L) }
        val updates = tracker.replay(parallel)
        assertEquals(listOf(OffRoute), updates.cues)
        assertTrue(updates.all { it.progress.remainingMeters == before })
    }

    @Test
    fun `rejoining gives BackOnRoute once and the walk continues from there`() {
        val (tracker, t) = walkedTo(150.0)
        val start = t + 1000
        val out = sideways(150.0, 0.0, 40.0, start)
        val back = sideways(150.0, 40.0, 0.0, out.last().timestampMillis + 1000)
        val rest = walk(route, from = 150.0 + WALK_SPEED, startMillis = back.last().timestampMillis + 1000)
        val updates = tracker.replay(out + back + rest)
        assertEquals(
            listOf(OffRoute, BackOnRoute, ManeuverApproaching(1, TURN_LEFT), ManeuverImminent(1, TURN_LEFT), ManeuverPassed(1, TURN_LEFT), ManeuverApproaching(2, ARRIVAL), NavigationCue.Arrived),
            updates.cues,
        )
        // vuelve dentro de 12 m + precisión, dos fixes seguidos
        val rejoined = updates.indexOfFirst { BackOnRoute in it.cues }
        assertTrue(updates[rejoined].progress.crossTrackMeters!! <= 17.0)
        assertTrue(updates[rejoined - 1].progress.crossTrackMeters!! <= 17.0)
        // el filtro arranca de nuevo: el avance no salta con la velocidad de antes del desvío
        val after = updates[out.size + back.size].progress.remainingMeters
        assertEquals(300 - 150 - WALK_SPEED, after, 2.0)
    }

    @Test
    fun `hysteresis keeps it off route between the rejoin and the leave thresholds`() {
        val (tracker, t) = walkedTo(100.0)
        val start = (t / 1000).toInt() + 1
        val base = path.pointAt(100.0)
        tracker.replay(still(base.offset(0.0, 40.0), start..start + 5))
        // a 20 m: ya no estaría fuera, pero aún no ha vuelto
        assertTrue(tracker.replay(still(base.offset(0.0, 20.0), start + 6..start + 30)).all { it.progress.offRoute })
        assertEquals(listOf(BackOnRoute), tracker.replay(still(base.offset(0.0, 5.0), start + 31..start + 32)).cues)
    }

    @Test
    fun `cutting across to the other leg of a route that passes near itself is off route`() {
        // ida al este 100 m, 40 m al norte y vuelta al oeste: las dos calles a 40 m
        val hairpin = routeOf(Leg(CONTINUE, 90.0, 100.0), Leg(TURN_LEFT, 0.0, 40.0), Leg(TURN_LEFT, 270.0, 100.0))
        val hairpinPath = MeasuredPath(hairpin.points)
        val tracker = RouteTracker(hairpin)
        val fixes = walk(hairpin, to = 30.0)
        tracker.replay(fixes)
        val remaining = tracker.progress.remainingMeters
        // cruza al norte hasta la vuelta y sigue por ella hacia el oeste
        val corner = hairpinPath.pointAt(30.0)
        val cross = (1..29).map { i -> fix(corner.offset(0.0, i * WALK_SPEED), fixes.last().timestampMillis + i * 1000L) }
        val west = (1..20).map { i -> fix(corner.offset(0.0, 40.0).offset(270.0, i * WALK_SPEED), cross.last().timestampMillis + i * 1000L) }
        val updates = tracker.replay(cross + west)
        assertEquals(listOf(OffRoute), updates.cues)
        assertTrue(updates.last().progress.offRoute)
        // los primeros metros del cruce aún están en la ruta; nunca salta a la vuelta (quedarían ~70 m)
        assertEquals(remaining, updates.last().progress.remainingMeters, 5.0)
    }

    @Test
    fun `walking back along the route ends up off route`() {
        // regla de sentido contrario aplazada al paso 14: la ventana mira 15 m atrás, así que acaba fuera
        val (tracker, t) = walkedTo(150.0)
        val backwards = (1..60).map { i -> fix(path.pointAt(150.0 - i * WALK_SPEED), t + i * 1000L) }
        val updates = tracker.replay(backwards)
        val declared = updates.indexOfFirst { OffRoute in it.cues }
        val walkedBack = (declared + 1) * WALK_SPEED
        assertTrue("declared after $walkedBack m", walkedBack in 40.0..55.0)
    }
}
