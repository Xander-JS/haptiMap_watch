package com.xanderjs.haptimap.designsystem.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.xanderjs.haptimap.designsystem.R
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * Icono sobre un disco relleno de [color] (rediseño Pulso): la maniobra, el POI, fuera de ruta y la llegada se leen
 * por la forma y el color del disco antes que por el texto. En ambiente, sin relleno: un anillo fino y el icono, en el
 * gris de ambiente y del mismo tamaño (pocos píxeles encendidos, la misma forma y nada se mueve al entrar y salir).
 */
@Composable
fun IconDisc(
    painter: Painter,
    contentDescription: String?,
    color: Color,
    onColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = HaptiMapTheme.dimens.maneuverDisc,
) {
    val ambient = HaptiMapTheme.ambient
    val shape = HaptiMapTheme.shapes.full
    Box(
        modifier = modifier.size(size).let { if (ambient) it.border(HaptiMapTheme.dimens.outlineWidth, color, shape) else it.background(color, shape) },
        contentAlignment = Alignment.Center,
    ) {
        HaptiIcon(painter, contentDescription, size = HaptiIconSize.Disc, tint = if (ambient) color else onColor)
    }
}

@HaptiPreviews
@Composable
private fun IconDiscPreview() = PreviewSurface {
    val colors = HaptiMapTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.s)) {
        IconDisc(painterResource(R.drawable.ic_turn_left), null, colors.primary, colors.onPrimary)
        IconDisc(painterResource(R.drawable.ic_exploration_museum), null, colors.tour, colors.onTour)
        IconDisc(painterResource(R.drawable.ic_arrive), null, colors.success, colors.onSuccess)
    }
}
