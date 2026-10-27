package com.xanderjs.haptimap.core.model

/** Acceso a internet del reloj (sección 17). Independiente de la sesión de navegación. */
sealed interface ConnectivityStatus {
    /** Red con internet validado; cualquier transporte cuenta como conectado. */
    data class Connected(val transport: Transport) : ConnectivityStatus
    data object Disconnected : ConnectivityStatus
}

/** BLUETOOTH = internet a través del teléfono emparejado. */
enum class Transport { WIFI, CELLULAR, BLUETOOTH, OTHER }
