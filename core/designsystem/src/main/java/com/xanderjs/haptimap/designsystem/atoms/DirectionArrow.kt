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

enum class ArrowDirection(@param:DrawableRes val icon: Int, @param:StringRes val description: Int) {
    LEFT(R.drawable.ic_turn_left, R.string.direction_left),
    RIGHT(R.drawable.ic_turn_right, R.string.direction_right),
    STRAIGHT(R.drawable.ic_straight, R.string.direction_straight),
}

/** Flecha de maniobra: un HaptiIcon especializado con el color navigationArrow y descripción para TalkBack. */
@Composable
fun DirectionArrow(
    direction: ArrowDirection,
    modifier: Modifier = Modifier,
    size: HaptiIconSize = HaptiIconSize.Large,
) {
    HaptiIcon(
        painter = painterResource(direction.icon),
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
