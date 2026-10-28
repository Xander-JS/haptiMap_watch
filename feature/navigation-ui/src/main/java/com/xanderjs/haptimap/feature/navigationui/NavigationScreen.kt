package com.xanderjs.haptimap.feature.navigationui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xanderjs.haptimap.designsystem.atoms.ArrowDirection
import com.xanderjs.haptimap.designsystem.atoms.DirectionArrow
import com.xanderjs.haptimap.designsystem.atoms.HaptiButton
import com.xanderjs.haptimap.designsystem.atoms.HaptiButtonVariant
import com.xanderjs.haptimap.designsystem.atoms.HeadingText
import com.xanderjs.haptimap.designsystem.atoms.InstructionText
import com.xanderjs.haptimap.designsystem.atoms.RouteStatsText
import com.xanderjs.haptimap.designsystem.atoms.StatusTone
import com.xanderjs.haptimap.designsystem.molecules.ManeuverIndicator
import com.xanderjs.haptimap.designsystem.organisms.ConfirmDialog
import com.xanderjs.haptimap.designsystem.organisms.ConfirmDialogContent
import com.xanderjs.haptimap.designsystem.organisms.StatusMessage
import com.xanderjs.haptimap.designsystem.templates.CenteredScreenTemplate
import com.xanderjs.haptimap.designsystem.templates.RoundScreenTemplate

/**
 * Navegación activa (paso 07) y su llegada. [onStopped] llega con la sesión ya en Idle; [onNewSearch],
 * con la sesión en Completed.
 */
@Composable
fun NavigationScreen(onStopped: () -> Unit, onNewSearch: () -> Unit, viewModel: NavigationViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    NavigationContent(
        state = state,
        onStop = viewModel::requestStop,
        onConfirmStop = {
            viewModel.confirmStop()
            onStopped()
        },
        onDismissStop = viewModel::dismissStop,
        onNewSearch = {
            viewModel.finish()
            onNewSearch()
        },
    )
}

/** La pantalla en un estado dado; las capturas de todos los estados están en src/screenshotTest. */
@Composable
internal fun NavigationContent(
    state: NavigationUiState,
    onStop: () -> Unit,
    onConfirmStop: () -> Unit,
    onDismissStop: () -> Unit,
    onNewSearch: () -> Unit,
) = when (state) {
    NavigationUiState.Ended -> CenteredScreenTemplate {}
    is NavigationUiState.Navigating -> {
        NavigatingContent(state, onStop)
        ConfirmDialog(
            visible = state.confirmingStop,
            title = stringResource(R.string.navigation_stop_title),
            confirmLabel = stringResource(R.string.navigation_stop_confirm),
            dismissLabel = stringResource(R.string.navigation_stop_dismiss),
            onConfirm = onConfirmStop,
            onDismiss = onDismissStop,
        )
    }
    is NavigationUiState.Arrived -> ArrivedContent(state.destinationName, onNewSearch)
}

/**
 * Elementos sueltos y no un organismo: el primero de la lista tiene que ser corto (DESIGN_SYSTEM.md §9),
 * y la versión Eco/AOD del paso 12 se queda con la flecha y la distancia. Arriba, un solo aviso y lo que
 * queda: debajo de la instrucción se cortaba a media línea en la redonda grande y la cuadrada. Detener,
 * al final, con scroll: no se pulsa sin querer al mirar el reloj.
 */
@Composable
private fun NavigatingContent(state: NavigationUiState.Navigating, onStop: () -> Unit) {
    val guidance = state.guidance
    RoundScreenTemplate {
        state.notice?.let { item { NavigationNoticeLabel(it) } }
        item { RouteStatsText(distanceMeters = guidance.remainingMeters, duration = guidance.remainingTime) }
        item { ManeuverIndicator(direction = guidance.direction, distanceMeters = guidance.distanceMeters) }
        guidance.instruction?.let { item { InstructionText(it) } }
        item {
            HaptiButton(
                text = stringResource(R.string.navigation_stop),
                onClick = onStop,
                modifier = Modifier.fillMaxWidth(),
                variant = HaptiButtonVariant.Secondary,
            )
        }
    }
}

/** El diálogo de Detener sin su ventana, para las capturas. */
@Composable
internal fun StopConfirmationContent() = ConfirmDialogContent(
    title = stringResource(R.string.navigation_stop_title),
    confirmLabel = stringResource(R.string.navigation_stop_confirm),
    dismissLabel = stringResource(R.string.navigation_stop_dismiss),
    onConfirm = {},
    onDismiss = {},
)

/** Fin claro: el pin de llegada, "Has llegado", el destino y la vuelta a buscar. Con scroll por si el nombre es largo. */
@Composable
private fun ArrivedContent(destinationName: String, onNewSearch: () -> Unit) {
    RoundScreenTemplate {
        item { DirectionArrow(direction = ArrowDirection.ARRIVE) }
        item { HeadingText(stringResource(R.string.navigation_arrived)) }
        item { StatusMessage(destinationName, tone = StatusTone.Muted) }
        item { HaptiButton(text = stringResource(R.string.navigation_new_search), onClick = onNewSearch, modifier = Modifier.fillMaxWidth()) }
    }
}
