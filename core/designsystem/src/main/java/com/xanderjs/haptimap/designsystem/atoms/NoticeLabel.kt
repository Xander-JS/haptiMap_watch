package com.xanderjs.haptimap.designsystem.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.Text
import com.xanderjs.haptimap.designsystem.R
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * Aviso corto dentro del contenido: [text] ya resuelto, en el color de [tone], en una píldora surface.
 * No es interactivo. Lo que significa cada aviso (p. ej. sin red con una ruta cargada) lo decide la pantalla.
 * TalkBack lo anuncia al aparecer.
 */
@Composable
fun NoticeLabel(text: String, tone: StatusTone, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier
            .semantics { liveRegion = LiveRegionMode.Polite }
            .background(HaptiMapTheme.colors.surface, HaptiMapTheme.shapes.full)
            .padding(horizontal = HaptiMapTheme.spacing.l, vertical = HaptiMapTheme.spacing.s),
        style = HaptiMapTheme.typography.caption,
        color = tone.color,
        textAlign = TextAlign.Center,
    )
}

@HaptiPreviews
@Composable
private fun NoticeLabelPreview() = PreviewSurface {
    Column(verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.s)) {
        StatusTone.entries.forEach { NoticeLabel(stringResource(R.string.sample_notice), it) }
    }
}
