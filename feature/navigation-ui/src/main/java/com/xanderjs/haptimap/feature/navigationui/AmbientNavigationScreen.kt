package com.xanderjs.haptimap.feature.navigationui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xanderjs.haptimap.designsystem.templates.AmbientScreenTemplate

/**
 * Vista de ambiente / AOD de la navegación (paso 12). La app la pone encima de cualquier pantalla mientras el reloj
 * está en ambiente; sin navegación no pinta nada. Solo se redibuja cuando cambia [AmbientNavigationUi].
 */
@Composable
fun AmbientNavigationScreen(viewModel: NavigationViewModel = hiltViewModel()) {
    val state by viewModel.ambientState.collectAsStateWithLifecycle()
    state?.let { AmbientNavigationContent(it) }
}

/** La primera vista de Eco en el tema de ambiente: mismas piezas, mismo sitio. */
@Composable
internal fun AmbientNavigationContent(state: AmbientNavigationUi) = AmbientScreenTemplate {
    when (state) {
        is AmbientNavigationUi.Guidance -> NavigatingGlance(state.direction, state.distanceMeters, state.notice)
        is AmbientNavigationUi.OffRoute -> OffRouteGlance(state.status, state.notice)
        AmbientNavigationUi.Arrived -> ArrivedGlance()
    }
}
