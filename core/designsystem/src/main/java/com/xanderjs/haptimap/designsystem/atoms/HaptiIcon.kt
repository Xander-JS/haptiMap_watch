package com.xanderjs.haptimap.designsystem.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.wear.compose.material3.Icon
import com.xanderjs.haptimap.designsystem.R
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

enum class HaptiIconSize { Small, Medium, Large }

/** Icono con tamaños del token dimens. [tint] debe ser un color semántico de HaptiMapTheme.colors. */
@Composable
fun HaptiIcon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: HaptiIconSize = HaptiIconSize.Medium,
    tint: Color = HaptiMapTheme.colors.onSurface,
) {
    val dimens = HaptiMapTheme.dimens
    val sizeDp = when (size) {
        HaptiIconSize.Small -> dimens.iconSmall
        HaptiIconSize.Medium -> dimens.iconMedium
        HaptiIconSize.Large -> dimens.iconLarge
    }
    Icon(painter = painter, contentDescription = contentDescription, modifier = modifier.size(sizeDp), tint = tint)
}

@HaptiPreviews
@Composable
private fun HaptiIconPreview() = PreviewSurface {
    Row(horizontalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.m), verticalAlignment = Alignment.CenterVertically) {
        HaptiIconSize.entries.forEach {
            HaptiIcon(painter = painterResource(R.drawable.ic_straight), contentDescription = null, size = it)
        }
    }
}
