package com.xanderjs.haptimap.designsystem.atoms

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.xanderjs.haptimap.designsystem.R
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/** [outlineIcon]: la variante de ambiente (paso 12), el mismo contorno trazado y sin relleno. */
enum class ArrowDirection(
    @param:DrawableRes val icon: Int,
    @param:DrawableRes val outlineIcon: Int,
    @param:StringRes val description: Int,
) {
    LEFT(R.drawable.ic_turn_left, R.drawable.ic_turn_left_outline, R.string.direction_left),
    RIGHT(R.drawable.ic_turn_right, R.drawable.ic_turn_right_outline, R.string.direction_right),
    STRAIGHT(R.drawable.ic_straight, R.drawable.ic_straight_outline, R.string.direction_straight),
    ARRIVE(R.drawable.ic_arrive, R.drawable.ic_arrive_outline, R.string.direction_arrive),
}

/**
 * Flecha de maniobra: un HaptiIcon especializado con el color navigationArrow y descripción para TalkBack.
 * En un tema de ambiente, la variante de contorno.
 */
@Composable
fun DirectionArrow(
    direction: ArrowDirection,
    modifier: Modifier = Modifier,
    size: HaptiIconSize = HaptiIconSize.Large,
) {
    HaptiIcon(
        painter = painterResource(if (HaptiMapTheme.ambient) direction.outlineIcon else direction.icon),
        contentDescription = stringResource(direction.description),
        modifier = modifier,
        size = size,
        tint = HaptiMapTheme.colors.navigationArrow,
    )
}

@HaptiPreviews
@Composable
private fun DirectionArrowPreview() = PreviewSurface {
    Row(horizontalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.s)) {
        ArrowDirection.entries.forEach { DirectionArrow(direction = it) }
    }
}

@HaptiPreviews
@Composable
private fun DirectionArrowAmbientPreview() = PreviewSurface(ambient = true) {
    Row(horizontalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.s)) {
        ArrowDirection.entries.forEach { DirectionArrow(direction = it) }
    }
}
