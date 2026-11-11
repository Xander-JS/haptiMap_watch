package com.xanderjs.haptimap.designsystem.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import com.xanderjs.haptimap.designsystem.atoms.ArrowDirection
import com.xanderjs.haptimap.designsystem.atoms.DirectionArrow
import com.xanderjs.haptimap.designsystem.atoms.DistanceText
import com.xanderjs.haptimap.designsystem.atoms.HaptiIconSize
import com.xanderjs.haptimap.designsystem.atoms.ModeTone
import com.xanderjs.haptimap.designsystem.atoms.color
import com.xanderjs.haptimap.designsystem.atoms.outlineBorder
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * Próxima maniobra sobre el mapa continuo (paso 22): flecha + distancia en horizontal, en una píldora surface que
 * resalta sobre las teselas. Compacta (banner superior) frente al [ManeuverIndicator] vertical de la vista Eco; de
 * ahí que sea un componente propio en lugar de reusar ese. Reutiliza los átomos [DirectionArrow] y [DistanceText].
 * TalkBack lo lee como una sola frase ("Gira a la izquierda, 20 m").
 */
@Composable
fun MapManeuverCard(
    direction: ArrowDirection,
    distanceMeters: Int,
    modifier: Modifier = Modifier,
    tone: ModeTone = ModeTone.Focus,
) {
    Row(
        modifier = modifier
            .semantics(mergeDescendants = true) {}
            .background(HaptiMapTheme.colors.surface, HaptiMapTheme.shapes.full)
            .border(outlineBorder(), HaptiMapTheme.shapes.full)
            .padding(horizontal = HaptiMapTheme.spacing.l, vertical = HaptiMapTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.m),
    ) {
        DirectionArrow(direction = direction, size = HaptiIconSize.Medium, tint = tone.color)
        DistanceText(meters = distanceMeters, compact = true, unitColor = tone.color)
    }
}

@HaptiPreviews
@Composable
private fun MapManeuverCardPreview() = PreviewSurface {
    MapManeuverCard(direction = ArrowDirection.LEFT, distanceMeters = 45)
}
