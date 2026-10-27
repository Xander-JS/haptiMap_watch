package com.xanderjs.haptimap.core.network

import com.xanderjs.haptimap.core.model.ConnectivityStatus
import kotlinx.coroutines.flow.StateFlow

/** Contrato de la Connectivity Layer (sección 17): un único flujo caliente para toda la app. */
interface ConnectivityMonitor {
    val status: StateFlow<ConnectivityStatus>
}
