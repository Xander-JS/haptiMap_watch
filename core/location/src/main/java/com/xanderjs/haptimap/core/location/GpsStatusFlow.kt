package com.xanderjs.haptimap.core.location

import com.xanderjs.haptimap.core.model.GpsStatus
import com.xanderjs.haptimap.core.model.LocationFix
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * Lógica de estado del GPS, sin Android, compartida por la implementación real y la fake.
 * [fixes] solo se recolecta con permiso y ubicación activada; al apagarla se cancela y se liberan las actualizaciones.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun gpsStatusFlow(
    hasPermission: () -> Boolean,
    isLocationEnabled: () -> Boolean,
    providerChanges: Flow<Unit>,
    fixes: () -> Flow<LocationFix>,
): Flow<GpsStatus> = flow {
    if (!hasPermission()) {
        emit(GpsStatus.NoPermission)
        return@flow
    }
    emitAll(
        providerChanges
            .onStart { emit(Unit) } // estado inicial: si ya está apagada, LocationDisabled sin esperar al broadcast
            .map { isLocationEnabled() }
            .distinctUntilChanged()
            .flatMapLatest { enabled ->
                if (enabled) fixes().map(GpsStatus::from).onStart { emit(GpsStatus.Searching) }
                else flowOf(GpsStatus.LocationDisabled)
            },
    )
}
