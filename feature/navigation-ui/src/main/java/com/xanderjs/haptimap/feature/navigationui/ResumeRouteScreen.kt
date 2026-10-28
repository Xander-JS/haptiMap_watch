package com.xanderjs.haptimap.feature.navigationui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xanderjs.haptimap.designsystem.atoms.HaptiButton
import com.xanderjs.haptimap.designsystem.atoms.HaptiButtonVariant
import com.xanderjs.haptimap.designsystem.atoms.HeadingText
import com.xanderjs.haptimap.designsystem.organisms.StatusMessage
import com.xanderjs.haptimap.designsystem.templates.CenteredScreenTemplate
import com.xanderjs.haptimap.designsystem.templates.GlanceScreenTemplate

/**
 * Pregunta al arrancar si retomar la ruta guardada. [onResumed] llega con la sesión ya en RouteReady;
 * [onDiscarded], con la caché borrada, o en cuanto se sabe que no hay ruta que retomar.
 */
@Composable
fun ResumeRouteScreen(onResumed: () -> Unit, onDiscarded: () -> Unit, viewModel: ResumeRouteViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (val current = state) {
        // casi siempre la lectura es instantánea: solo el fondo, sin un "Cargando…" que parpadee
        ResumeRouteUiState.Pending -> ResumeRoutePendingContent()
        ResumeRouteUiState.Loading -> ResumeRouteLoadingContent()
        ResumeRouteUiState.NoRoute -> {
            // lo que se veía sigue el instante que tarda en navegar
            ResumeRoutePendingContent()
            LaunchedEffect(Unit) { onDiscarded() }
        }
        is ResumeRouteUiState.Ready -> ResumeRouteReady(current, viewModel, onResumed, onDiscarded)
    }
}

/** Los primeros SLOW_LOAD_THRESHOLD de la lectura: el fondo y la hora, nada más. */
@Composable
internal fun ResumeRoutePendingContent() = CenteredScreenTemplate {}

/** Si la lectura de la ruta guardada tarda. */
@Composable
internal fun ResumeRouteLoadingContent() = CenteredScreenTemplate {
    StatusMessage(stringResource(R.string.resume_route_loading), loading = true)
}

@Composable
private fun ResumeRouteReady(state: ResumeRouteUiState.Ready, viewModel: ResumeRouteViewModel, onResumed: () -> Unit, onDiscarded: () -> Unit) {
    ResumeRouteContent(
        destinationName = state.route.destination.name,
        onResume = {
            viewModel.resume()
            onResumed()
        },
        onDiscard = {
            viewModel.discard()
            onDiscarded()
        },
    )
}

/** GlanceScreenTemplate: el destino y la pregunta siempre; cada botón en la primera vista solo si cabe entero. */
@Composable
internal fun ResumeRouteContent(destinationName: String, onResume: () -> Unit, onDiscard: () -> Unit) {
    GlanceScreenTemplate(
        glance = {
            HeadingText(destinationName)
            StatusMessage(stringResource(R.string.resume_route_prompt))
        },
        details = listOf(
            @Composable { HaptiButton(text = stringResource(R.string.resume_route_resume), onClick = onResume, modifier = Modifier.fillMaxWidth()) },
            @Composable {
                HaptiButton(
                    text = stringResource(R.string.resume_route_discard),
                    onClick = onDiscard,
                    modifier = Modifier.fillMaxWidth(),
                    variant = HaptiButtonVariant.Secondary,
                )
            },
        ),
    )
}
