package com.xanderjs.haptimap.designsystem.atoms

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.Text
import com.xanderjs.haptimap.designsystem.R
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * Un párrafo que explica algo (permiso, tutorial háptico): body, centrado, sin límite de líneas, al ancho de
 * contenido centrado para que no lo corte el círculo.
 */
@Composable
fun BodyText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(HaptiMapTheme.dimens.centeredContentWidthFraction),
        style = HaptiMapTheme.typography.body,
        color = HaptiMapTheme.colors.onBackground,
        textAlign = TextAlign.Center,
    )
}

@HaptiPreviews
@Composable
private fun BodyTextPreview() = PreviewSurface { BodyText(stringResource(R.string.sample_status_error)) }
