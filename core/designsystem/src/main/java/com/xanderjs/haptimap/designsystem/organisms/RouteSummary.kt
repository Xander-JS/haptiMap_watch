package com.xanderjs.haptimap.designsystem.organisms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.material3.Text
import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.Maneuver
import com.xanderjs.haptimap.core.model.Route
import com.xanderjs.haptimap.core.model.RouteStep
import com.xanderjs.haptimap.designsystem.R
import com.xanderjs.haptimap.designsystem.atoms.HaptiButton
import com.xanderjs.haptimap.designsystem.atoms.HeadingText
import com.xanderjs.haptimap.designsystem.atoms.RouteStatsText
import com.xanderjs.haptimap.designsystem.atoms.arrow
import com.xanderjs.haptimap.designsystem.molecules.ManeuverIndicator
import com.xanderjs.haptimap.designsystem.molecules.samplePlaces
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Resumen de una ruta lista: "distancia · tiempo", primera maniobra con su distancia, la instrucción
 * de Google si la hay y el botón para empezar. El nombre del destino va aparte, en un [HeadingText]:
 * TransformingLazyColumn centra un primer elemento más alto que la pantalla, así que el título corto
 * tiene que ser el primer elemento de la lista.
 */
@Composable
fun RouteSummary(route: Route, onStart: () -> Unit, modifier: Modifier = Modifier) {
    val colors = HaptiMapTheme.colors
    // el paso 0 es la salida: la primera maniobra es la del paso 1, a la distancia del paso 0
    val first = route.steps.first()
    val (next, meters) = if (route.steps.size > 1) route.steps[1] to first.distanceMeters else first to route.distanceMeters
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.m),
    ) {
        RouteStatsText(distanceMeters = route.distanceMeters, duration = route.duration)
        ManeuverIndicator(direction = next.maneuver.arrow, distanceMeters = meters)
        next.instruction?.let {
            Text(
                it,
                style = HaptiMapTheme.typography.caption,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HaptiButton(text = stringResource(R.string.route_start), onClick = onStart, modifier = Modifier.fillMaxWidth())
    }
}

/** Ruta de ejemplo para previews y catálogo: salida, giro a la izquierda a 120 m y llegada. */
@Composable
fun sampleRoute(): Route {
    val point = GeoPoint(0.0, 0.0)
    fun step(maneuver: Maneuver, meters: Int, instruction: String?) =
        RouteStep(maneuver, meters, meters.seconds, listOf(point), point, point, instruction, null)
    return Route(
        destination = samplePlaces().last(),
        distanceMeters = 1240,
        duration = 15.minutes,
        points = listOf(point),
        steps = listOf(
            step(Maneuver.CONTINUE, 120, null),
            step(Maneuver.TURN_LEFT, 1120, stringResource(R.string.sample_instruction)),
            step(Maneuver.ARRIVAL, 0, null),
        ),
    )
}

@HaptiPreviews
@Composable
private fun RouteSummaryPreview() = PreviewSurface {
    val route = sampleRoute()
    Column(verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.s)) {
        HeadingText(route.destination.name)
        RouteSummary(route = route, onStart = {})
    }
}
