package com.xanderjs.haptimap.designsystem.atoms

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.material3.Text
import com.xanderjs.haptimap.designsystem.R
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * Encabezado de una pantalla (p. ej. el destino de una ruta): centrado, hasta [maxLines] líneas (2), y TalkBack lo anuncia como título.
 * Ocupa dimens.centeredContentWidthFraction del ancho: va arriba, donde el círculo es más estrecho, y a todo el ancho
 * un nombre de dos líneas se cortaba por los lados.
 */
@Composable
fun HeadingText(text: String, modifier: Modifier = Modifier, maxLines: Int = 2) {
    Text(
        text,
        modifier = modifier.fillMaxWidth(HaptiMapTheme.dimens.centeredContentWidthFraction).semantics { heading() },
        style = HaptiMapTheme.typography.title,
        color = HaptiMapTheme.colors.onBackground,
        textAlign = TextAlign.Center,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@HaptiPreviews
@Composable
private fun HeadingTextPreview() = PreviewSurface { HeadingText(stringResource(R.string.sample_place_museum)) }
