package com.xanderjs.haptimap.feature.navigationui

import com.xanderjs.haptimap.core.model.ConnectivityStatus
import com.xanderjs.haptimap.core.model.Route
import com.xanderjs.haptimap.core.navigation.NavigationSessionStore
import com.xanderjs.haptimap.core.navigation.activeRoute
import com.xanderjs.haptimap.core.network.ConnectivityMonitor
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/** La ruta activa de la sesión y si se sigue sin red (paso 06). */
data class ActiveRoute(val route: Route, val offline: Boolean)

/**
 * Sesión y conectividad siguen siendo estados independientes (paso 03): se combinan aquí, una vez,
 * para quien muestra una ruta. Null sin ruta activa (ver NavigationSession.activeRoute).
 */
class ObserveActiveRoute @Inject constructor(
    private val store: NavigationSessionStore,
    private val connectivity: ConnectivityMonitor,
) {
    operator fun invoke(): Flow<ActiveRoute?> =
        combine(store.state, connectivity.status) { session, status ->
            session.activeRoute?.let { ActiveRoute(it, status == ConnectivityStatus.Disconnected) }
        }.distinctUntilChanged()
}
