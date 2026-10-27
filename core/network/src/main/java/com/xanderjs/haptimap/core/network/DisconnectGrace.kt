package com.xanderjs.haptimap.core.network

import com.xanderjs.haptimap.core.model.ConnectivityStatus
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

/** Cortes más breves que esto (cambio Wi-Fi ↔ Bluetooth, red que se revalida) no se muestran. */
val DISCONNECT_GRACE: Duration = 3.seconds

/**
 * Retrasa Disconnected [grace]; si vuelve la conexión antes, el corte no se emite. La reconexión
 * y el primer valor (estado al empezar a recolectar) se emiten al instante.
 */
internal fun Flow<ConnectivityStatus>.withDisconnectGrace(grace: Duration = DISCONNECT_GRACE): Flow<ConnectivityStatus> =
    channelFlow {
        var first = true
        collectLatest { status ->
            if (status is ConnectivityStatus.Disconnected && !first) delay(grace)
            first = false
            send(status)
        }
    }.distinctUntilChanged()
