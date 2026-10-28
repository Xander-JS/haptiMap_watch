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
import com.xanderjs.haptimap.designsystem.R
import com.xanderjs.haptimap.designsystem.atoms.ArrowDirection
import com.xanderjs.haptimap.designsystem.atoms.HaptiButton
import com.xanderjs.haptimap.designsystem.atoms.HeadingText
import com.xanderjs.haptimap.designsystem.atoms.RouteStatsText
import com.xanderjs.haptimap.designsystem.molecules.ManeuverIndicator
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Lo que muestra [RouteSummary]: totales de la ruta y la próxima maniobra ([direction] a
 * [maneuverDistanceMeters]) con la instrucción ya traducida, si la hay.
 */
data class RouteSummaryData(
    val distanceMeters: Int,
    val duration: Duration,
    val direction: ArrowDirection,
    val maneuverDistanceMeters: Int,
    val instruction: String?,
)

/**
 * Resumen de una ruta lista: "distancia · tiempo", próxima maniobra con su distancia, la instrucción
 * si la hay y el botón para empezar. El nombre del destino va aparte, en un [HeadingText]:
 * TransformingLazyColumn centra un primer elemento más alto que la pantalla, así que el título corto
 * tiene que ser el primer elemento de la lista.
 */
@Composable
fun RouteSummary(summary: RouteSummaryData, onStart: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.m),
    ) {
        RouteStatsText(distanceMeters = summary.distanceMeters, duration = summary.duration)
        ManeuverIndicator(direction = summary.direction, distanceMeters = summary.maneuverDistanceMeters)
        summary.instruction?.let {
            Text(
                it,
                style = HaptiMapTheme.typography.caption,
                color = HaptiMapTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HaptiButton(text = stringResource(R.string.route_start), onClick = onStart, modifier = Modifier.fillMaxWidth())
    }
}

/** Resumen de ejemplo para previews y catálogo: 1,2 km, 15 min, girar a la izquierda en 120 m. */
@Composable
fun sampleRouteSummary() = RouteSummaryData(1240, 15.minutes, ArrowDirection.LEFT, 120, stringResource(R.string.sample_instruction))

@HaptiPreviews
@Composable
private fun RouteSummaryPreview() = PreviewSurface {
    Column(verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.s)) {
        HeadingText(stringResource(R.string.sample_place_museum))
        RouteSummary(summary = sampleRouteSummary(), onStart = {})
    }
}
