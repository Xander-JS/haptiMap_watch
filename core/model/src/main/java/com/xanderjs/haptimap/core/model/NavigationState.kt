package com.xanderjs.haptimap.core.model

/** Estados de la máquina de estados principal (sección 20). */
enum class NavigationState {
    IDLE,
    SEARCHING,
    DESTINATION_SELECTED,
    ROUTE_LOADING,
    NAVIGATION_ACTIVE,
    OFF_ROUTE,
    RECALCULATING,
    POI_DETECTED,
    POI_SUGGESTION,
    DETOUR,
    ARRIVAL,
    COMPLETED,
}
