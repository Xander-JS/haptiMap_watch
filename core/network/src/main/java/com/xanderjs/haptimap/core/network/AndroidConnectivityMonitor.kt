package com.xanderjs.haptimap.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import com.xanderjs.haptimap.core.model.ConnectivityStatus
import com.xanderjs.haptimap.core.model.Transport
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

/**
 * Conectividad real con NetworkCallback sobre la red por defecto. Un solo callback para toda la app
 * ([scope] es de aplicación); se desregistra [STOP_TIMEOUT] después de que deje de recolectarse.
 */
class AndroidConnectivityMonitor(context: Context, scope: CoroutineScope) : ConnectivityMonitor {

    private val manager = context.getSystemService(ConnectivityManager::class.java)

    override val status: StateFlow<ConnectivityStatus> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                trySend(caps.toStatus())
            }

            override fun onLost(network: Network) {
                trySend(ConnectivityStatus.Disconnected)
            }
        }
        send(currentStatus()) // si no hay red, el callback no llama a nada
        manager.registerDefaultNetworkCallback(callback)
        Log.d(TAG, "Network callback registered")
        awaitClose {
            manager.unregisterNetworkCallback(callback)
            Log.d(TAG, "Network callback unregistered")
        }
    }
        .withDisconnectGrace()
        .onEach { Log.d(TAG, "Status: $it") }
        .stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), currentStatus())

    private fun currentStatus(): ConnectivityStatus =
        manager.getNetworkCapabilities(manager.activeNetwork)?.toStatus() ?: ConnectivityStatus.Disconnected

    private fun NetworkCapabilities.toStatus(): ConnectivityStatus {
        // VALIDATED: la red llega de verdad a internet (no un portal cautivo ni un Wi-Fi sin salida)
        if (!hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
            !hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        ) return ConnectivityStatus.Disconnected
        val transport = when {
            hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Transport.WIFI
            hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> Transport.CELLULAR
            hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> Transport.BLUETOOTH
            else -> Transport.OTHER
        }
        return ConnectivityStatus.Connected(transport)
    }

    private companion object {
        const val TAG = "HaptiConnectivity"

        /** Cubre recreaciones de la Activity (rotación, cambio de idioma) sin volver a registrar el callback. */
        val STOP_TIMEOUT = 5.seconds.inWholeMilliseconds
    }
}
