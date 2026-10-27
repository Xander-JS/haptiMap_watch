package com.xanderjs.haptimap.designsystem.atoms

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.Text
import com.xanderjs.haptimap.designsystem.R
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme
import com.xanderjs.haptimap.designsystem.theme.appLanguage
import java.util.Locale

private const val METERS_PER_KM = 1000

/** "850 m", o "1.2 km" / "1,2 km" a partir de 1 km, con el separador decimal del idioma de la app. */
@Composable
fun formatDistance(meters: Int): String {
    val locale = Locale.forLanguageTag(LocalContext.current.appLanguage())
    return if (meters < METERS_PER_KM) {
        String.format(locale, stringResource(R.string.distance_meters), meters)
    } else {
        String.format(locale, stringResource(R.string.distance_kilometers), meters.toFloat() / METERS_PER_KM)
    }
}

/** Distancia grande y legible para la navegación; el texto sale de [formatDistance]. */
@Composable
fun DistanceText(meters: Int, modifier: Modifier = Modifier) {
    Text(
        text = formatDistance(meters),
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
