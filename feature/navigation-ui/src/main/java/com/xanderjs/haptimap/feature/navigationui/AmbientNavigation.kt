package com.xanderjs.haptimap.feature.navigationui

import com.xanderjs.haptimap.designsystem.atoms.ArrowDirection

/**
 * Lo que muestra la vista de ambiente / AOD (paso 12): la primera vista de Eco sin lo que cambia con cada fix
 * (instrucción, lo que queda, la distancia a la ruta) y con la distancia en pasos gruesos. Al ser un valor
 * igual mientras no pasa nada, el StateFlow no emite y la pantalla de ambiente no se redibuja.
 */
sealed interface AmbientNavigationUi {
    data class Guidance(val direction: ArrowDirection, val distanceMeters: Int, val notice: NavigationNotice?) : AmbientNavigationUi
    data class OffRoute(val status: OffRouteStatus, val notice: NavigationNotice?) : AmbientNavigationUi
    data object Arrived : AmbientNavigationUi
}

/** null sin navegación: la app no pinta vista de ambiente propia. */
fun ambientNavigationUi(state: NavigationUiState): AmbientNavigationUi? = when (state) {
    is NavigationUiState.Navigating ->
        AmbientNavigationUi.Guidance(state.guidance.direction, approximateMeters(state.guidance.distanceMeters), state.notice)
    is NavigationUiState.OffRoute -> AmbientNavigationUi.OffRoute(state.status, state.notice)
    is NavigationUiState.Arrived -> AmbientNavigationUi.Arrived
    NavigationUiState.Ended -> null
}

/**
 * La distancia aproximada de ambiente, al paso más cercano: 10 m por debajo de 100 m, 50 m hasta 500 m, 100 m hasta
 * 1 km y 500 m desde ahí. Andando cambia cada ~7 s cerca del giro y cada pocos minutos lejos, no con cada fix.
 */
fun approximateMeters(meters: Int): Int {
    val step = when {
        meters < 100 -> 10
        meters < 500 -> 50
        meters < 1000 -> 100
        else -> 500
    }
    return (meters + step / 2) / step * step
}
