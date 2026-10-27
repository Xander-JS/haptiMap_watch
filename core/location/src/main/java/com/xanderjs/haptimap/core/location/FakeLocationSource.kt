package com.xanderjs.haptimap.core.location

import com.xanderjs.haptimap.core.model.GpsStatus
import com.xanderjs.haptimap.core.model.LocationFix
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow

/** Implementación para tests y previews: emite [fixes] pasando por la misma lógica de estado que la real. */
class FakeLocationSource(
    private vararg val fixes: LocationFix,
    private val hasPermission: Boolean = true,
    private val locationEnabled: Boolean = true,
) : LocationSource {
    override fun gpsStatus(intervalMillis: Long): Flow<GpsStatus> =
        gpsStatusFlow({ hasPermission }, { locationEnabled }, emptyFlow(), { fixes.asFlow() })
}
