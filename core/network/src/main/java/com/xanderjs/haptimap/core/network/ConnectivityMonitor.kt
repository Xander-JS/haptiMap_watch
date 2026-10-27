package com.xanderjs.haptimap.core.network

import com.xanderjs.haptimap.core.model.ConnectivityState
import kotlinx.coroutines.flow.StateFlow

/** Contrato de la Connectivity Layer (sección 17). */
interface ConnectivityMonitor {
    val state: StateFlow<ConnectivityState>
}
