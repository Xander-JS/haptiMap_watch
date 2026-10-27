package com.xanderjs.haptimap.core.network

import com.xanderjs.haptimap.core.model.ConnectivityStatus
import com.xanderjs.haptimap.core.model.Transport
import kotlinx.coroutines.flow.MutableStateFlow

/** Para tests y previews: el estado se cambia a mano con [status]. */
class FakeConnectivityMonitor(
    initial: ConnectivityStatus = ConnectivityStatus.Connected(Transport.WIFI),
) : ConnectivityMonitor {
    override val status = MutableStateFlow(initial)
}
