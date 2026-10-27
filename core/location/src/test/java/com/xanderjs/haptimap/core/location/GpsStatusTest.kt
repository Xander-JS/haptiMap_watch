package com.xanderjs.haptimap.core.location

import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.GpsStatus
import com.xanderjs.haptimap.core.model.LOW_ACCURACY_THRESHOLD_METERS
import com.xanderjs.haptimap.core.model.LocationFix
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GpsStatusTest {

    private fun fix(accuracy: Float) = LocationFix(GeoPoint(40.4168, -3.7038), accuracy, 1.2f, 90f, 0L)

    private fun statuses(source: LocationSource) = runBlocking { source.gpsStatus().toList() }

    @Test
    fun `accurate fix is reported as Fix after Searching`() {
        val accurate = fix(accuracy = 5f)
        assertEquals(listOf(GpsStatus.Searching, GpsStatus.Fix(accurate)), statuses(FakeLocationSource(accurate)))
    }

    @Test
    fun `fix worse than the threshold is LowAccuracy, at the threshold is Fix`() {
        val atThreshold = fix(LOW_ACCURACY_THRESHOLD_METERS)
        val worse = fix(LOW_ACCURACY_THRESHOLD_METERS + 0.1f)
        assertEquals(
            listOf(GpsStatus.Searching, GpsStatus.Fix(atThreshold), GpsStatus.LowAccuracy(worse)),
            statuses(FakeLocationSource(atThreshold, worse)),
        )
    }

    @Test
    fun `without permission only NoPermission is emitted`() {
        assertEquals(listOf(GpsStatus.NoPermission), statuses(FakeLocationSource(fix(5f), hasPermission = false)))
    }

    @Test
    fun `location already off emits LocationDisabled immediately and never requests fixes`() {
        val source = FakeLocationSource(fix(5f), locationEnabled = false)
        assertEquals(listOf(GpsStatus.LocationDisabled), statuses(source))

        val flow = gpsStatusFlow({ true }, { false }, flowOf(), { error("fixes requested with location off") })
        assertEquals(listOf(GpsStatus.LocationDisabled), runBlocking { flow.toList() })
    }

    @Test
    fun `turning location on after a provider change starts searching`() {
        val enabled = mutableListOf(false, true).iterator()
        val accurate = fix(5f)
        val flow = gpsStatusFlow({ true }, { enabled.next() }, flowOf(Unit), { flowOf(accurate) })
        assertEquals(
            listOf(GpsStatus.LocationDisabled, GpsStatus.Searching, GpsStatus.Fix(accurate)),
            runBlocking { flow.toList() },
        )
    }
}
