package com.xanderjs.haptimap.designsystem.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Text
import com.xanderjs.haptimap.designsystem.R
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

enum class HaptiButtonVariant { Primary, Secondary }

/**
 * Botón de la app. El texto llega ya resuelto con stringResource() desde quien lo usa y va centrado
 * en todo el ancho del botón: los botones de Wear en una lista ocupan el ancho, así que uno sin
 * fillMaxWidth también crece hasta el ancho disponible. Con [icon] (decorativo) el texto va junto al icono, a la izquierda.
 * [maxLines] corta con puntos suspensivos; TalkBack sigue leyendo el texto entero.
 */
@Composable
fun HaptiButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: HaptiButtonVariant = HaptiButtonVariant.Primary,
    enabled: Boolean = true,
    icon: Painter? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    val colors = HaptiMapTheme.colors
    val (container, content) = when (variant) {
        HaptiButtonVariant.Primary -> colors.primary to colors.onPrimary
        HaptiButtonVariant.Secondary -> colors.surface to colors.onSurface
    }
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = HaptiMapTheme.dimens.minTouchTarget),
        enabled = enabled,
        shape = HaptiMapTheme.shapes.full,
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
    ) {
        // misma rama sin icono que antes del paso 11: los botones existentes no cambian ni un píxel
        icon?.let {
            HaptiIcon(painter = it, contentDescription = null, tint = content)
            Spacer(Modifier.width(HaptiMapTheme.spacing.m))
        }
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            style = HaptiMapTheme.typography.label,
            textAlign = if (icon == null) TextAlign.Center else TextAlign.Start,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@HaptiPreviews
@Composable
private fun HaptiButtonPreview() = PreviewSurface {
    Column(verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.s)) {
        HaptiButton(text = stringResource(R.string.sample_button_primary), onClick = {})
        HaptiButton(text = stringResource(R.string.sample_button_secondary), onClick = {}, variant = HaptiButtonVariant.Secondary)
    }
}
