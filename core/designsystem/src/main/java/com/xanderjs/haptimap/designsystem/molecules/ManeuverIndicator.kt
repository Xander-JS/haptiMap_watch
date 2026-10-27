package com.xanderjs.haptimap.designsystem.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import com.xanderjs.haptimap.designsystem.atoms.ArrowDirection
import com.xanderjs.haptimap.designsystem.atoms.DirectionArrow
import com.xanderjs.haptimap.designsystem.atoms.DistanceText
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/** Próxima maniobra: flecha + distancia. TalkBack lo lee como una sola frase ("Gira a la izquierda, 20 m"). */
@Composable
fun ManeuverIndicator(
    direction: ArrowDirection,
    distanceMeters: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.s),
    ) {
        DirectionArrow(direction = direction)
        DistanceText(meters = distanceMeters)
    }
}

@HaptiPreviews
@Composable
private fun ManeuverIndicatorPreview() = PreviewSurface {
    ManeuverIndicator(direction = ArrowDirection.LEFT, distanceMeters = 20)
}
