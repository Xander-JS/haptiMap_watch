package com.xanderjs.haptimap.designsystem.atoms

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.Text
import com.xanderjs.haptimap.designsystem.R
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * Título de un grupo dentro de una pantalla (p. ej. cada ajuste, paso 25): caption en color secundario,
 * centrado, y TalkBack lo anuncia como encabezado para saltar entre grupos. Para rotular una lista de opciones,
 * no una pantalla (eso es [HeadingText]).
 */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth().padding(top = HaptiMapTheme.spacing.m).semantics { heading() },
        style = HaptiMapTheme.typography.caption,
        color = HaptiMapTheme.colors.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@HaptiPreviews
@Composable
private fun SectionLabelPreview() = PreviewSurface { SectionLabel(stringResource(R.string.sample_section_label)) }
