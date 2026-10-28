package com.xanderjs.haptimap.feature.navigationui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xanderjs.haptimap.core.model.RouteError
import com.xanderjs.haptimap.designsystem.atoms.HaptiButton
import com.xanderjs.haptimap.designsystem.atoms.HaptiButtonVariant
import com.xanderjs.haptimap.designsystem.atoms.HeadingText
import com.xanderjs.haptimap.designsystem.organisms.RouteStatus
import com.xanderjs.haptimap.designsystem.organisms.RouteSummary
import com.xanderjs.haptimap.designsystem.organisms.StatusMessage
import com.xanderjs.haptimap.designsystem.templates.CenteredScreenTemplate
import com.xanderjs.haptimap.designsystem.templates.RoundScreenTemplate

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

/** La pantalla en un estado dado; las previews y capturas de todos los estados están en src/screenshotTest. */
@Composable
internal fun RouteSummaryContent(state: RouteSummaryUiState, onStart: () -> Unit, onRetry: () -> Unit) {
    if (state.route != null) {
        RoundScreenTemplate {
            item { HeadingText(state.route.destination.name) }
            item { RouteSummary(route = state.route, onStart = onStart) }
        }
        return
    }
    // cargando o error: un mensaje corto, centrado donde la pantalla redonda no lo corta
    CenteredScreenTemplate {
        state.status?.let { status ->
            StatusMessage(status)
            // sin clave no hay nada que reintentar: es de la compilación
            if (status is RouteStatus.Failed && status.error != RouteError.MISSING_KEY) {
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
