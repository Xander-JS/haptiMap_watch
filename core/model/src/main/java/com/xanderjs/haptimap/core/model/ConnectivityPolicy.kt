package com.xanderjs.haptimap.core.model

/** Qué se puede hacer con cada estado de conectividad (sección 17). Lo consultan quienes piden datos de red. */
object ConnectivityPolicy {
    fun canSearch(status: ConnectivityStatus) = status is ConnectivityStatus.Connected
    fun canRequestRoute(status: ConnectivityStatus) = status is ConnectivityStatus.Connected
    fun canFetchPois(status: ConnectivityStatus) = status is ConnectivityStatus.Connected

    /** Perder la conexión nunca interrumpe una ruta ya cargada. */
    @Suppress("UNUSED_PARAMETER")
    fun canContinueLoadedRoute(status: ConnectivityStatus) = true
}
