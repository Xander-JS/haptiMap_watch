package com.xanderjs.haptimap.feature.navigationui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.AppScaffold
import com.xanderjs.haptimap.core.model.RouteError
import com.xanderjs.haptimap.designsystem.atoms.HaptiButton
import com.xanderjs.haptimap.designsystem.atoms.HaptiButtonVariant
import com.xanderjs.haptimap.designsystem.atoms.HaptiTimeText
import com.xanderjs.haptimap.designsystem.organisms.RouteStatus
import com.xanderjs.haptimap.designsystem.organisms.RouteSummary
import com.xanderjs.haptimap.designsystem.organisms.StatusMessage
import com.xanderjs.haptimap.designsystem.organisms.sampleRoute
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.templates.RoundScreenTemplate
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/** Resumen de la ruta al destino elegido. [onStart] llega tras pasar la sesión a NavigationActive. */
@Composable
fun RouteSummaryScreen(onStart: () -> Unit, viewModel: RouteSummaryViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RouteSummaryContent(
        state = state,
        onStart = {
            viewModel.start()
            onStart()
        },
        onRetry = viewModel::loadRoute,
    )
}

@Composable
private fun RouteSummaryContent(state: RouteSummaryUiState, onStart: () -> Unit, onRetry: () -> Unit) {
    RoundScreenTemplate {
        state.route?.let { item { RouteSummary(route = it, onStart = onStart) } }
        state.status?.let { status ->
            item { StatusMessage(status) }
            // sin clave no hay nada que reintentar: es de la compilación
            if (status is RouteStatus.Failed && status.error != RouteError.MISSING_KEY) {
                item {
                    HaptiButton(
                        text = stringResource(R.string.route_retry),
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth(),
                        variant = HaptiButtonVariant.Secondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun Preview(state: RouteSummaryUiState) = HaptiMapTheme {
    AppScaffold(timeText = { HaptiTimeText() }) { RouteSummaryContent(state, {}, {}) }
}

@HaptiPreviews
@Composable
private fun RouteSummaryReadyPreview() = Preview(RouteSummaryUiState(status = null, route = sampleRoute()))

@HaptiPreviews
@Composable
private fun RouteSummaryLoadingPreview() = Preview(RouteSummaryUiState(RouteStatus.Loading))

@HaptiPreviews
@Composable
private fun RouteSummaryOfflinePreview() = Preview(RouteSummaryUiState(RouteStatus.Failed(RouteError.NO_CONNECTION)))

@HaptiPreviews
@Composable
private fun RouteSummaryMissingKeyPreview() = Preview(RouteSummaryUiState(RouteStatus.Failed(RouteError.MISSING_KEY)))

@HaptiPreviews
@Composable
private fun RouteSummaryNoRoutePreview() = Preview(RouteSummaryUiState(RouteStatus.Failed(RouteError.NO_ROUTE)))

@HaptiPreviews
@Composable
private fun RouteSummaryApiErrorPreview() = Preview(RouteSummaryUiState(RouteStatus.Failed(RouteError.API_ERROR)))
