package com.xanderjs.haptimap.designsystem.atoms

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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

private const val MINUTES_PER_HOUR = 60

/** "14 min", o "1 h 5 min" desde una hora. Redondea hacia arriba: mejor prometer un minuto de más que de menos. */
@Composable
fun formatDuration(duration: Duration): String {
    val locale = Locale.forLanguageTag(LocalContext.current.appLanguage())
    val minutes = ((duration.inWholeSeconds + 59) / 60).toInt()
    return if (minutes < MINUTES_PER_HOUR) {
        String.format(locale, stringResource(R.string.duration_minutes), minutes)
    } else {
        String.format(locale, stringResource(R.string.duration_hours_minutes), minutes / MINUTES_PER_HOUR, minutes % MINUTES_PER_HOUR)
    }
}

/** Distancia y tiempo estimado de una ruta en una línea: "1.2 km · 15 min". */
@Composable
fun RouteStatsText(distanceMeters: Int, duration: Duration, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.route_stats, formatDistance(distanceMeters), formatDuration(duration)),
        modifier = modifier,
        style = HaptiMapTheme.typography.body,
        color = HaptiMapTheme.colors.onSurfaceVariant,
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
}

@HaptiPreviews
@Composable
private fun RouteStatsTextPreview() = PreviewSurface {
    RouteStatsText(distanceMeters = 1240, duration = 75.minutes)
}
