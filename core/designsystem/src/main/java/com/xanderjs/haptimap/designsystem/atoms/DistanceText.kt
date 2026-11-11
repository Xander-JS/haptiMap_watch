package com.xanderjs.haptimap.designsystem.atoms

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.em
import androidx.wear.compose.material3.Text
import com.xanderjs.haptimap.designsystem.R
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme
import com.xanderjs.haptimap.designsystem.theme.appLanguage
import java.util.Locale

private const val METERS_PER_KM = 1000
private const val UNIT_SCALE = 0.5f

/** "850 m", o "1.2 km" / "1,2 km" a partir de 1 km, con el separador decimal del idioma de la app. */
@Composable
fun formatDistance(meters: Int): String = formatDistance(LocalContext.current, meters)

/** El mismo texto fuera de Compose: la notificación del servicio en primer plano (paso 10). */
fun formatDistance(context: Context, meters: Int): String {
    val locale = Locale.forLanguageTag(context.appLanguage())
    return if (meters < METERS_PER_KM) {
        String.format(locale, context.getString(R.string.distance_meters), meters)
    } else {
        String.format(locale, context.getString(R.string.distance_kilometers), meters.toFloat() / METERS_PER_KM)
    }
}

/**
 * Distancia grande y legible para la navegación; el texto sale de [formatDistance]. [compact] (paso 13): en
 * typography.distanceCompact, donde acompaña a un texto que también tiene que caber. La cifra manda; la unidad va
 * a la mitad de tamaño y en [unitColor] (el color del modo), para que el número se lea primero.
 */
@Composable
fun DistanceText(
    meters: Int,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    unitColor: Color = HaptiMapTheme.colors.navigationArrow,
) {
    val text = formatDistance(meters)
    val unitStart = text.lastIndexOf(' ') + 1
    Text(
        text = buildAnnotatedString {
            append(text)
            if (unitStart > 0) addStyle(SpanStyle(fontSize = UNIT_SCALE.em, color = unitColor), unitStart, text.length)
        },
        modifier = modifier,
        color = HaptiMapTheme.colors.onBackground,
        style = if (compact) HaptiMapTheme.typography.distanceCompact else HaptiMapTheme.typography.distance,
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
