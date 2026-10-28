package com.xanderjs.haptimap.core.navigation

import com.xanderjs.haptimap.core.model.Maneuver.ARRIVAL
import com.xanderjs.haptimap.core.model.Maneuver.CONTINUE
import com.xanderjs.haptimap.core.model.Maneuver.TURN_LEFT
import com.xanderjs.haptimap.core.model.Maneuver.TURN_RIGHT
import com.xanderjs.haptimap.core.model.MeasuredPath
import com.xanderjs.haptimap.core.model.offset
import com.xanderjs.haptimap.core.navigation.NavigationCue.Arrived
import com.xanderjs.haptimap.core.navigation.NavigationCue.ManeuverApproaching
import com.xanderjs.haptimap.core.navigation.NavigationCue.ManeuverImminent
import com.xanderjs.haptimap.core.navigation.NavigationCue.ManeuverPassed
import java.util.Random
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteTrackerTest {

    private val straight = routeOf(Leg(CONTINUE, 90.0, 100.0))

    // salida al este, izquierda al norte, derecha al este, seguir recto en un cruce, llegada
    private val allManeuvers = routeOf(
        Leg(CONTINUE, 90.0, 60.0),
        Leg(TURN_LEFT, 0.0, 50.0),
        Leg(TURN_RIGHT, 90.0, 50.0),
        Leg(CONTINUE, 90.0, 40.0),
    )

    private val allManeuverCues = listOf(
        ManeuverApproaching(1, TURN_LEFT), ManeuverImminent(1, TURN_LEFT), ManeuverPassed(1, TURN_LEFT),
        ManeuverApproaching(2, TURN_RIGHT), ManeuverImminent(2, TURN_RIGHT), ManeuverPassed(2, TURN_RIGHT),
        ManeuverApproaching(3, CONTINUE), ManeuverImminent(3, CONTINUE), ManeuverPassed(3, CONTINUE),
        ManeuverApproaching(4, ARRIVAL), Arrived,
    )

    @Test
    fun `before any fix the progress is the start of the route`() {
        val progress = RouteTracker(allManeuvers).progress
        assertEquals(0, progress.stepIndex)
        assertEquals(TURN_LEFT, progress.nextManeuver.maneuver)
        assertEquals(60.0, progress.distanceToManeuverMeters, 0.5)
        assertEquals(200.0, progress.remainingMeters, 0.5)
        assertEquals((200 / WALK_SPEED).seconds.inWholeSeconds.toDouble(), progress.remainingTime.inWholeSeconds.toDouble(), 1.0)
        assertNull(progress.crossTrackMeters)
        assertFalse(progress.weakSignal)
    }

    @Test
    fun `straight walk advances steadily and arrives once`() {
        val updates = RouteTracker(straight).replay(walk(straight))
        val remaining = updates.map { it.progress.remainingMeters }
        assertEquals(remaining.sortedDescending(), remaining)
        assertEquals(98.6, updates[1].progress.remainingMeters, 0.5)
        assertEquals(updates[1].progress.remainingMeters, updates[1].progress.distanceToManeuverMeters, 0.0)
        assertTrue(updates.all { (it.progress.crossTrackMeters ?: 0.0) < 0.5 })
        assertEquals(listOf(ManeuverApproaching(1, ARRIVAL), Arrived), updates.cues)
    }

    @Test
    fun `every maneuver type is announced once per threshold and in order`() {
        val tracker = RouteTracker(allManeuvers)
        val updates = tracker.replay(walk(allManeuvers))
        assertEquals(allManeuverCues, updates.cues)
    }

    @Test
    fun `thresholds fire at 18, 6 and 5 m past the maneuver`() {
        val tracker = RouteTracker(allManeuvers)
        val path = MeasuredPath(allManeuvers.points)
        fun cuesAt(meters: Double) = tracker.update(fix(path.pointAt(meters), (meters * 1000).toLong())).cues
        assertEquals(emptyList<NavigationCue>(), cuesAt(41.5))
        assertEquals(listOf(ManeuverApproaching(1, TURN_LEFT)), cuesAt(42.5))
        assertEquals(emptyList<NavigationCue>(), cuesAt(53.5))
        assertEquals(listOf(ManeuverImminent(1, TURN_LEFT)), cuesAt(54.5))
        assertEquals(emptyList<NavigationCue>(), cuesAt(64.5))
        assertEquals(TURN_LEFT, tracker.progress.nextManeuver.maneuver)
        assertEquals(listOf(ManeuverPassed(1, TURN_LEFT)), cuesAt(65.5))
        assertEquals(1, tracker.progress.stepIndex)
        assertEquals(TURN_RIGHT, tracker.progress.nextManeuver.maneuver)
    }

    @Test
    fun `a fix that skips thresholds emits the missed cues in order`() {
        val tracker = RouteTracker(allManeuvers)
        val path = MeasuredPath(allManeuvers.points)
        tracker.update(fix(path.pointAt(30.0), 0))
        val cues = tracker.update(fix(path.pointAt(70.0), 3_000)).cues
        assertEquals(allManeuverCues.take(3), cues)
    }

    @Test
    fun `noisy fixes never move progress back and give the same cues`() {
        val random = Random(7)
        val noisy = walk(allManeuvers) { _, p -> p.offset(random.nextDouble() * 360, random.nextGaussian() * 4) }
        val updates = RouteTracker(allManeuvers).replay(noisy)
        val remaining = updates.map { it.progress.remainingMeters }
        assertEquals(remaining.sortedDescending(), remaining)
        assertEquals(allManeuverCues, updates.cues)
        assertTrue(updates.all { it.progress.crossTrackMeters!! < 15 })
    }

    @Test
    fun `poor-accuracy spikes are ignored for progress and cues`() {
        val tracker = RouteTracker(allManeuvers)
        val path = MeasuredPath(allManeuvers.points)
        tracker.update(fix(path.pointAt(40.0), 0))
        val before = tracker.progress
        // 60 m al norte, en la segunda calle, pero con 40 m de error
        val spike = tracker.update(fix(path.pointAt(40.0).offset(0.0, 60.0), 1_000, accuracy = 40f))
        assertEquals(emptyList<NavigationCue>(), spike.cues)
        assertEquals(before.remainingMeters, spike.progress.remainingMeters, 0.0)
        assertEquals(40f, spike.progress.fixAccuracyMeters)
        assertFalse(spike.progress.weakSignal)
    }

    @Test
    fun `spikes inside a noisy walk do not change the cues`() {
        val spiky = walk(allManeuvers).mapIndexed { i, f ->
            if (i % 10 == 5) f.copy(point = f.point.offset(0.0, 80.0), accuracyMeters = 45f) else f
        }
        assertEquals(allManeuverCues, RouteTracker(allManeuvers).replay(spiky).cues)
    }

    @Test
    fun `weak signal after 10 s without a good fix, cleared by the next good one`() {
        val tracker = RouteTracker(straight)
        val start = straight.points.first()
        tracker.update(fix(start, 0))
        assertFalse(tracker.update(fix(start, 9_999, accuracy = 30f)).progress.weakSignal)
        assertTrue(tracker.update(fix(start, 10_000, accuracy = 30f)).progress.weakSignal)
        assertTrue(tracker.update(fix(start, 15_000, accuracy = 50f)).progress.weakSignal)
        assertFalse(tracker.update(fix(start, 16_000)).progress.weakSignal)
    }

    @Test
    fun `weak signal from the start when no good fix ever arrives`() {
        val tracker = RouteTracker(straight)
        val start = straight.points.first()
        assertFalse(tracker.update(fix(start, 0, accuracy = 30f)).progress.weakSignal)
        assertTrue(tracker.update(fix(start, 10_000, accuracy = 30f)).progress.weakSignal)
    }

    @Test
    fun `signal lost without any fix sets weak signal until a good fix`() {
        val tracker = RouteTracker(straight)
        assertTrue(tracker.signalLost().weakSignal)
        assertFalse(tracker.update(fix(straight.points.first(), 0)).progress.weakSignal)
    }

    @Test
    fun `the weak signal threshold is configurable`() {
        val tracker = RouteTracker(straight, NavigationThresholds(weakSignalAfter = 3.seconds))
        val start = straight.points.first()
        tracker.update(fix(start, 0))
        assertTrue(tracker.update(fix(start, 3_000, accuracy = 30f)).progress.weakSignal)
    }

    @Test
    fun `standing still keeps progress and emits nothing new`() {
        val tracker = RouteTracker(allManeuvers)
        val point = MeasuredPath(allManeuvers.points).pointAt(50.0)
        val random = Random(3)
        val updates = (0 until 120).map { tracker.update(fix(point.offset(random.nextDouble() * 360, random.nextDouble() * 4), it * 1000L)) }
        assertEquals(listOf(ManeuverApproaching(1, TURN_LEFT)), updates.cues)
        val remaining = updates.map { it.progress.remainingMeters }
        assertTrue(remaining.max() - remaining.min() < 5)
        assertFalse(updates.last().progress.weakSignal)
    }

    @Test
    fun `a route that passes near itself does not jump to the other leg`() {
        // ida al este 100 m, 10 m al norte y vuelta al oeste: las dos calles a 10 m
        val hairpin = routeOf(Leg(CONTINUE, 90.0, 100.0), Leg(TURN_LEFT, 0.0, 10.0), Leg(TURN_LEFT, 270.0, 100.0))
        // los primeros 80 m de la ida con los fixes 6 m al norte: más cerca de la vuelta (4 m) que de la ida
        val outbound = walk(hairpin, to = 80.0) { i, p -> if (i == 0) p else p.offset(0.0, 6.0) }
        val tracker = RouteTracker(hairpin)
        val updates = tracker.replay(outbound)
        updates.forEachIndexed { i, update -> assertEquals(210 - i * WALK_SPEED, update.progress.remainingMeters, 1.0) }
        val rest = walk(hairpin, from = 80.0 + WALK_SPEED, startMillis = outbound.last().timestampMillis + 1000)
        assertEquals(
            listOf(
                ManeuverApproaching(1, TURN_LEFT), ManeuverApproaching(2, TURN_LEFT), ManeuverImminent(1, TURN_LEFT),
                // el fix a 105 m pasa el giro 1 y deja el 2 a 5 m: en un mismo fix, por orden de maniobra
                ManeuverPassed(1, TURN_LEFT), ManeuverImminent(2, TURN_LEFT), ManeuverPassed(2, TURN_LEFT),
                ManeuverApproaching(3, ARRIVAL), Arrived,
            ),
            (updates + tracker.replay(rest)).cues,
        )
    }

    @Test
    fun `arrives within the accuracy-aware radius of the end`() {
        val path = MeasuredPath(straight.points)
        // radio 10 m + precisión 5 m: a 16 m todavía no, a 14 m sí
        val tracker = RouteTracker(straight)
        assertFalse(Arrived in tracker.update(fix(path.pointAt(84.0), 0)).cues)
        assertTrue(Arrived in tracker.update(fix(path.pointAt(86.0), 1_000)).cues)
        // la precisión amplía el radio hasta 10 m más
        val loose = RouteTracker(straight)
        assertTrue(Arrived in loose.update(fix(path.pointAt(81.0), 0, accuracy = 15f)).cues)
    }

    @Test
    fun `arrives when overshooting the end`() {
        val path = MeasuredPath(straight.points)
        val tracker = RouteTracker(straight)
        tracker.update(fix(path.pointAt(70.0), 0))
        val past = straight.points.last().offset(90.0, 30.0)
        val update = tracker.update(fix(past, 10_000))
        assertEquals(listOf(ManeuverApproaching(1, ARRIVAL), Arrived), update.cues)
        assertEquals(0.0, update.progress.remainingMeters, 0.0)
        assertEquals(emptyList<NavigationCue>(), tracker.update(fix(past, 11_000)).cues)
    }

    @Test
    fun `resuming mid-route does not announce maneuvers already done`() {
        val path = MeasuredPath(allManeuvers.points)
        val tracker = RouteTracker(allManeuvers)
        // a 145 m: los giros (60 y 110 m) ya hechos, el cruce de 160 m a 15
        val update = tracker.update(fix(path.pointAt(145.0), 0))
        assertEquals(listOf(ManeuverApproaching(3, CONTINUE)), update.cues)
        assertEquals(55.0, update.progress.remainingMeters, 0.5)
        assertEquals(CONTINUE, update.progress.nextManeuver.maneuver)
        assertEquals(2, update.progress.stepIndex)
    }
}
