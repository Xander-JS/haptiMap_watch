package com.xanderjs.haptimap.designsystem.atoms

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.material3.Text
import com.xanderjs.haptimap.designsystem.R
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * La instrucción de la próxima maniobra mientras se navega: body en onBackground, centrada, hasta 3 líneas.
 * Más visible que en el resumen de ruta (caption): aquí es lo que se lee de un vistazo al caminar.
 * dimens.centeredContentWidthFraction de ancho para no tocar el borde redondo.
 */
@Composable
fun InstructionText(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.fillMaxWidth(HaptiMapTheme.dimens.centeredContentWidthFraction),
        style = HaptiMapTheme.typography.body,
        color = HaptiMapTheme.colors.onBackground,
        textAlign = TextAlign.Center,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
    )
}

@HaptiPreviews
@Composable
private fun InstructionTextPreview() = PreviewSurface { InstructionText(stringResource(R.string.sample_instruction)) }
