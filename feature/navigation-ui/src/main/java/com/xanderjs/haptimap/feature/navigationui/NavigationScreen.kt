package com.xanderjs.haptimap.feature.navigationui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xanderjs.haptimap.core.model.DisplayMode
import com.xanderjs.haptimap.designsystem.R as DsR
import com.xanderjs.haptimap.designsystem.atoms.ArrowDirection
import com.xanderjs.haptimap.designsystem.atoms.HapticPulse
import com.xanderjs.haptimap.designsystem.atoms.IconDisc
import com.xanderjs.haptimap.designsystem.atoms.ModeTone
import com.xanderjs.haptimap.designsystem.atoms.HaptiButton
import com.xanderjs.haptimap.designsystem.atoms.HaptiButtonVariant
import com.xanderjs.haptimap.designsystem.atoms.LocalOffline
import com.xanderjs.haptimap.designsystem.atoms.formatDistance
import com.xanderjs.haptimap.designsystem.atoms.HeadingText
import com.xanderjs.haptimap.designsystem.atoms.InstructionText
import com.xanderjs.haptimap.designsystem.atoms.RouteStatsText
import com.xanderjs.haptimap.designsystem.atoms.StatusTone
import com.xanderjs.haptimap.designsystem.molecules.ManeuverIndicator
import com.xanderjs.haptimap.designsystem.organisms.ConfirmDialog
import com.xanderjs.haptimap.designsystem.organisms.ConfirmDialogContent
import com.xanderjs.haptimap.designsystem.organisms.StatusMessage
import com.xanderjs.haptimap.designsystem.templates.CenteredScreenTemplate
import com.xanderjs.haptimap.designsystem.templates.GlanceScreenTemplate
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme
import kotlin.time.Duration

/**
 * Navegación activa (paso 07), fuera de ruta (paso 08) y la llegada. [onStopped] llega con la sesión ya en Idle; [onNewSearch],
 * con la sesión en Completed.
 */
@Composable
fun NavigationScreen(onStopped: () -> Unit, onNewSearch: () -> Unit, viewModel: NavigationViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val displayMode by viewModel.displayMode.collectAsStateWithLifecycle()
    val pulse by viewModel.pulse.collectAsStateWithLifecycle()
    val confirmStop = {
        viewModel.confirmStop()
        onStopped()
    }
    // mapa continuo (paso 22) solo navegando y si está elegido; fuera de ruta, la llegada y el final siguen en Eco.
    // Conmutar el modo en ajustes cambia la vista sin reiniciar el trayecto.
    //
    // UNA sola instancia del mapa por trayecto (una sola carga facturada): mientras esté elegido se mantiene montado
    // TODO el trayecto, también en ambiente. En ambiente no se desmonta —su lifecycle lo pausa (onStop, deja de
    // dibujar y gasta menos) y la vista AOD lo tapa— así no se vuelve a facturar una carga al despertar. Sin red sigue
    // montado pero tapado por Eco en vivo. Se suelta (sale de composición → onDestroy) al terminar el trayecto o al
    // pasar a Eco; la presión de memoria la gestiona el SO (el MapView suelta sus recursos GL en onStop) y, en última
    // instancia, el reinicio por muerte del proceso (que recrea una sola carga).
    val navigating = state as? NavigationUiState.Navigating
    if (navigating != null && displayMode == DisplayMode.CONTINUOUS_MAP) {
        NavigationMapScreen(navigating = navigating, offline = LocalOffline.current, onStop = viewModel::requestStop)
        StopDialog(navigating.confirmingStop, confirmStop, viewModel::dismissStop)
    } else {
        NavigationContent(
            state = state,
            pulse = pulse,
            onStop = viewModel::requestStop,
            onRetry = viewModel::retry,
            onConfirmStop = confirmStop,
            onDismissStop = viewModel::dismissStop,
            onNewSearch = {
                viewModel.finish()
                onNewSearch()
            },
        )
    }
}

/** La pantalla en un estado dado; las capturas de todos los estados están en src/screenshotTest. */
@Composable
internal fun NavigationContent(
    state: NavigationUiState,
    onStop: () -> Unit,
    onRetry: () -> Unit,
    onConfirmStop: () -> Unit,
    onDismissStop: () -> Unit,
    onNewSearch: () -> Unit,
    pulse: HapticPulse? = null,
) = when (state) {
    NavigationUiState.Ended -> CenteredScreenTemplate {}
    is NavigationUiState.Navigating -> {
        NavigatingContent(state, onStop, pulse)
        StopDialog(state.confirmingStop, onConfirmStop, onDismissStop)
    }
    is NavigationUiState.OffRoute -> {
        OffRouteContent(state, onRetry, onStop)
        StopDialog(state.confirmingStop, onConfirmStop, onDismissStop)
    }
    is NavigationUiState.Arrived -> ArrivedContent(state.destinationName, onNewSearch)
}

/**
 * Vista Eco (modo A, paso 12). Primera vista: aviso, flecha y distancia; después la instrucción y lo que queda, cada
 * uno solo si cabe entero, si no justo debajo del borde (GlanceScreenTemplate). Detener, siempre debajo del borde: no
 * se pulsa al mirar. La vista de ambiente pinta la misma primera vista ([NavigatingGlance]).
 */
@Composable
internal fun NavigatingContent(state: NavigationUiState.Navigating, onStop: () -> Unit, pulse: HapticPulse? = null) {
    val guidance = state.guidance
    val tone = if (state.tour) ModeTone.Tour else ModeTone.Focus
    GlanceScreenTemplate(
        glance = { NavigatingGlance(guidance.direction, guidance.distanceMeters, state.notice, tone, pulse) },
        details = navigatingDetails(guidance.instruction, guidance.remainingMeters, guidance.remainingTime),
        belowFold = {
            HaptiButton(
                text = stringResource(R.string.navigation_stop),
                onClick = onStop,
                modifier = Modifier.fillMaxWidth(),
                variant = HaptiButtonVariant.Secondary,
            )
        },
    )
}

/**
 * Los detalles navegando, por prioridad: la instrucción antes que lo que queda. También los usa ambiente para
 * reservar su sitio (AmbientScreenTemplate), así la flecha no se mueve al entrar y salir.
 */
internal fun navigatingDetails(instruction: String?, remainingMeters: Int, remainingTime: Duration): List<@Composable () -> Unit> =
    listOfNotNull(
        instruction?.let { @Composable { InstructionText(it) } },
        @Composable { RouteStatsText(distanceMeters = remainingMeters, duration = remainingTime) },
    )

/**
 * La primera vista navegando, en Eco y en ambiente: el aviso si lo hay, la flecha en el color del modo y la distancia.
 * [pulse]: el último aviso háptico, para que el anillo lata a su ritmo (en ambiente no se anima).
 */
@Composable
internal fun NavigatingGlance(
    direction: ArrowDirection,
    distanceMeters: Int,
    notice: NavigationNotice?,
    tone: ModeTone = ModeTone.Focus,
    pulse: HapticPulse? = null,
) {
    notice?.let { NavigationNoticeLabel(it) }
    ManeuverIndicator(direction = direction, distanceMeters = distanceMeters, tone = tone, pulse = pulse)
}

@Composable
private fun StopDialog(visible: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) = ConfirmDialog(
    visible = visible,
    title = stringResource(R.string.navigation_stop_title),
    confirmLabel = stringResource(R.string.navigation_stop_confirm),
    dismissLabel = stringResource(R.string.navigation_stop_dismiss),
    onConfirm = onConfirm,
    onDismiss = onDismiss,
)

/**
 * Fuera de ruta (paso 08). Primera vista: el aviso de señal débil si lo hay, el icono (salvo buscando,
 * donde el giro de carga ocupa su sitio), "Fuera de ruta" y una línea de estado. Después, Reintentar tras
 * un error y la distancia a la ruta. Sin flecha: sin el rumbo del reloj no se sabe hacia dónde volver (paso 14).
 */
@Composable
private fun OffRouteContent(state: NavigationUiState.OffRoute, onRetry: () -> Unit, onStop: () -> Unit) {
    GlanceScreenTemplate(
        glance = { OffRouteGlance(state.status, state.notice) },
        details = offRouteDetails(state.status, state.metersFromRoute, onRetry),
        belowFold = {
            HaptiButton(
                text = stringResource(R.string.navigation_stop),
                onClick = onStop,
                modifier = Modifier.fillMaxWidth(),
                variant = HaptiButtonVariant.Secondary,
            )
        },
    )
}

/** Los detalles fuera de ruta, por prioridad: la acción antes que el dato. También reservan su sitio en ambiente. */
internal fun offRouteDetails(status: OffRouteStatus, metersFromRoute: Int?, onRetry: () -> Unit): List<@Composable () -> Unit> =
    listOfNotNull(
        if (status == OffRouteStatus.FAILED) {
            @Composable { HaptiButton(text = stringResource(R.string.route_retry), onClick = onRetry, modifier = Modifier.fillMaxWidth()) }
        } else {
            null
        },
        metersFromRoute?.let { meters ->
            @Composable { StatusMessage(stringResource(R.string.navigation_from_route, formatDistance(meters)), tone = StatusTone.Muted) }
        },
    )

/** La primera vista fuera de ruta, en Eco y en ambiente (donde StatusMessage no pinta el spinner). */
@Composable
internal fun OffRouteGlance(status: OffRouteStatus, notice: NavigationNotice?) {
    notice?.let { NavigationNoticeLabel(it) }
    if (status != OffRouteStatus.RECALCULATING) {
        // rojo: el color de "problema"; el icono va en el color del fondo, que contrasta con él en los dos temas
        val colors = HaptiMapTheme.colors
        IconDisc(painterResource(DsR.drawable.ic_off_route), contentDescription = null, color = colors.offRoute, onColor = colors.background)
    }
    HeadingText(stringResource(R.string.navigation_off_route))
    OffRouteStatusMessage(status)
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

/** Fin claro: el pin y "Has llegado" siempre; el destino y la vuelta a buscar si caben enteros (GlanceScreenTemplate). */
@Composable
private fun ArrivedContent(destinationName: String, onNewSearch: () -> Unit) {
    GlanceScreenTemplate(
        glance = { ArrivedGlance() },
        details = arrivedDetails(destinationName, onNewSearch),
    )
}

/** Los detalles de la llegada: el destino y la vuelta a buscar. También reservan su sitio en ambiente. */
internal fun arrivedDetails(destinationName: String, onNewSearch: () -> Unit): List<@Composable () -> Unit> = listOf(
    @Composable { StatusMessage(destinationName, tone = StatusTone.Muted) },
    @Composable { HaptiButton(text = stringResource(R.string.navigation_new_search), onClick = onNewSearch, modifier = Modifier.fillMaxWidth()) },
)

/** La primera vista de la llegada, en Eco y en ambiente. */
@Composable
internal fun ArrivedGlance() {
    // disco pequeño (el del POI): con el de la maniobra, "Nueva búsqueda" no cabía en la primera vista
    val colors = HaptiMapTheme.colors
    IconDisc(
        painterResource(if (HaptiMapTheme.ambient) DsR.drawable.ic_arrive_outline else DsR.drawable.ic_arrive),
        contentDescription = null,
        color = colors.success,
        onColor = colors.onSuccess,
        size = HaptiMapTheme.dimens.poiDisc,
    )
    HeadingText(stringResource(R.string.navigation_arrived))
}
