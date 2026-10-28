package com.xanderjs.haptimap.core.location

import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.GpsStatus
import com.xanderjs.haptimap.core.model.distanceTo
import com.xanderjs.haptimap.core.model.offset
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SimulatedWalkTest {

    private val start = GeoPoint(40.4168, -3.7038)
    private val route = listOf(start, start.offset(90.0, 10.0))

    @Test
    fun `walks the route at the given speed, one fix per interval, then stays at the end`() = runTest {
        val fixes = simulatedWalk(route, WalkSimulation(speedMetersPerSecond = 2.0), nowMillis = { currentTime }).take(8).toList()
        assertEquals((0..7).map { it * 1000L }, fixes.map { it.timestampMillis })
        assertEquals(listOf(0.0, 2.0, 4.0, 6.0, 8.0, 10.0, 10.0, 10.0), fixes.map { Math.round(start.distanceTo(it.point) * 10) / 10.0 })
        assertEquals(90f, fixes[1].bearingDegrees!!, 0.1f)
        assertEquals(2f, fixes[1].speedMetersPerSecond)
        assertNull(fixes.last().bearingDegrees)
        assertEquals(0f, fixes.last().speedMetersPerSecond)
    }

    @Test
    fun `noise is seeded and stays around the route`() = runTest {
        val noisy = WalkSimulation(speedMetersPerSecond = 0.0, noiseMeters = 3.0, seed = 42)
        val a = simulatedWalk(route, noisy, nowMillis = { currentTime }).take(50).toList()
        val b = simulatedWalk(route, noisy, nowMillis = { currentTime }).take(50).toList()
        assertEquals(a.map { it.point }, b.map { it.point })
        val distances = a.map { start.distanceTo(it.point) }
        assertTrue(distances.any { it > 1 })
        assertTrue(distances.average() < 6)
    }

    @Test
    fun `spikes are the last fixes of each period, with poor accuracy and far off`() = runTest {
        val spiky = WalkSimulation(speedMetersPerSecond = 0.0, spikeEvery = 5, spikeLength = 2, spikeAccuracyMeters = 45f, spikeOffsetMeters = 50.0)
        val fixes = simulatedWalk(route, spiky, nowMillis = { currentTime }).take(10).toList()
        assertEquals(listOf(5f, 5f, 5f, 45f, 45f, 5f, 5f, 5f, 45f, 45f), fixes.map { it.accuracyMeters })
        assertEquals(50.0, start.distanceTo(fixes[3].point), 0.1)
    }

    @Test
    fun `as gps status it starts searching and reports spikes as low accuracy`() = runTest {
        val statuses = simulatedGpsStatus(route, WalkSimulation(spikeEvery = 2), nowMillis = { currentTime }).take(3).toList()
        assertEquals(GpsStatus.Searching, statuses[0])
        assertTrue(statuses[1] is GpsStatus.Fix)
        assertTrue(statuses[2] is GpsStatus.LowAccuracy)
    }
}
