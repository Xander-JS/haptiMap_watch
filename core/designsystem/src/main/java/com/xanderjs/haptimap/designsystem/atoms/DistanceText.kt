package com.xanderjs.haptimap.designsystem.atoms

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.Text
import com.xanderjs.haptimap.designsystem.R
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

private const val METERS_PER_KM = 1000

/** Distancia grande y legible: "20 m", o "1.5 km" / "1,5 km" según el idioma a partir de 1 km. */
@Composable
fun DistanceText(meters: Int, modifier: Modifier = Modifier) {
    val text = if (meters < METERS_PER_KM) {
        stringResource(R.string.distance_meters, meters)
    } else {
        stringResource(R.string.distance_kilometers, meters.toFloat() / METERS_PER_KM)
    }
    Text(
        text = text,
        modifier = modifier,
        color = HaptiMapTheme.colors.onBackground,
        style = HaptiMapTheme.typography.distance,
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
}

@HaptiPreviews
@Composable
private fun DistanceTextPreview() = PreviewSurface {
    Column {
        DistanceText(meters = 20)
        DistanceText(meters = 1500)
    }
}
