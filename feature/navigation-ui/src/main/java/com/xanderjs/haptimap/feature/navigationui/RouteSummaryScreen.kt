package com.xanderjs.haptimap.feature.navigationui

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xanderjs.haptimap.core.model.RouteError
import com.xanderjs.haptimap.designsystem.atoms.HaptiButton
import com.xanderjs.haptimap.designsystem.atoms.HaptiButtonVariant
import com.xanderjs.haptimap.designsystem.atoms.HeadingText
import com.xanderjs.haptimap.designsystem.atoms.InstructionText
import com.xanderjs.haptimap.designsystem.atoms.RouteStatsText
import com.xanderjs.haptimap.designsystem.molecules.ManeuverIndicator
import com.xanderjs.haptimap.designsystem.templates.CenteredScreenTemplate
import com.xanderjs.haptimap.designsystem.templates.GlanceScreenTemplate

/** Resumen de la ruta al destino elegido. [onStart] llega tras pasar la sesión a NavigationActive. */
@Composable
fun RouteSummaryScreen(onStart: () -> Unit, viewModel: RouteSummaryViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val start = {
        viewModel.start()
        onStart()
    }
    // la navegación empieza diga lo que diga: sin el permiso solo faltan la notificación y el icono en la esfera
    // (paso 10). Denegado dos veces, Android (11+) ya no muestra el diálogo y devuelve false al momento
    val notificationPermission = rememberLauncherForActivityResult(RequestPermission()) { start() }
    RouteSummaryContent(
        state = state,
        onStart = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && context.checkSelfPermission(POST_NOTIFICATIONS) != PERMISSION_GRANTED) {
                notificationPermission.launch(POST_NOTIFICATIONS)
            } else {
                start()
            }
        },
        onRetry = viewModel::loadRoute,
    )
}

/** La pantalla en un estado dado; las previews y capturas de todos los estados están en src/screenshotTest. */
@Composable
internal fun RouteSummaryContent(state: RouteSummaryUiState, onStart: () -> Unit, onRetry: () -> Unit) {
    if (state.route != null) {
        val summary = state.route.toSummary()
        // GlanceScreenTemplate: destino y totales siempre; luego, por prioridad, Iniciar (la acción de la
        // pantalla) y el anticipo de la primera maniobra, cada uno solo si cabe entero
        GlanceScreenTemplate(
            glance = {
                HeadingText(state.route.destination.name)
                if (state.offline) RouteOfflineNotice()
                RouteStatsText(distanceMeters = summary.remainingMeters, duration = summary.remainingTime)
            },
            details = listOfNotNull(
                @Composable { HaptiButton(text = stringResource(R.string.route_start), onClick = onStart, modifier = Modifier.fillMaxWidth()) },
                @Composable { ManeuverIndicator(direction = summary.direction, distanceMeters = summary.distanceMeters) },
                summary.instruction?.let { instruction -> @Composable { InstructionText(instruction) } },
            ),
        )
        return
    }
    // cargando o error: un mensaje corto, centrado donde la pantalla redonda no lo corta
    CenteredScreenTemplate {
        state.status?.let { status ->
            RouteStatusMessage(status)
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
