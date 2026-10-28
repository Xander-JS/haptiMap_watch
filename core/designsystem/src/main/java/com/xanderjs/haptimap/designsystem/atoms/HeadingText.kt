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

/** Encabezado de una pantalla (p. ej. el destino de una ruta): centrado, hasta 2 líneas, y TalkBack lo anuncia como título. */
@Composable
fun HeadingText(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.fillMaxWidth().semantics { heading() },
        style = HaptiMapTheme.typography.title,
        color = HaptiMapTheme.colors.onBackground,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@HaptiPreviews
@Composable
private fun HeadingTextPreview() = PreviewSurface { HeadingText(stringResource(R.string.sample_place_museum)) }
