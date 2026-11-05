package com.xanderjs.haptimap.feature.navigationui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xanderjs.haptimap.designsystem.templates.AmbientScreenTemplate
import kotlin.time.Duration

/**
 * Vista de ambiente / AOD de la navegación (paso 12). La app la pone encima de cualquier pantalla mientras el reloj
 * está en ambiente; sin navegación no pinta nada. Solo se redibuja cuando cambia [AmbientNavigationUi].
 */
@Composable
fun AmbientNavigationScreen(viewModel: NavigationViewModel = hiltViewModel()) {
    val state by viewModel.ambientState.collectAsStateWithLifecycle()
    state?.let { AmbientNavigationContent(it) }
}

/**
 * La primera vista de Eco en el tema de ambiente: mismas piezas, mismo sitio. Los detalles de Eco reservan su sitio
 * sin pintarse; lo que cambia con cada fix va con un valor fijo de una línea, como en Eco.
 */
@Composable
internal fun AmbientNavigationContent(state: AmbientNavigationUi) = when (state) {
    is AmbientNavigationUi.Guidance -> AmbientScreenTemplate(
        glance = { NavigatingGlance(state.direction, state.distanceMeters, state.notice) },
        reservedDetails = navigatingDetails(state.instruction, remainingMeters = 0, remainingTime = Duration.ZERO),
    )
    is AmbientNavigationUi.OffRoute -> AmbientScreenTemplate(
        glance = { OffRouteGlance(state.status, state.notice) },
        // shortcut: la distancia a la ruta se reserva como "0 m", una línea; si en Eco ocupara dos, la vista bajaría
        reservedDetails = offRouteDetails(state.status, metersFromRoute = if (state.showsMetersFromRoute) 0 else null, onRetry = {}),
    )
    is AmbientNavigationUi.Arrived -> AmbientScreenTemplate(
        glance = { ArrivedGlance() },
        reservedDetails = arrivedDetails(state.destinationName, onNewSearch = {}),
    )
}
