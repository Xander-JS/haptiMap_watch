package com.xanderjs.haptimap.designsystem.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import com.xanderjs.haptimap.designsystem.atoms.ArrowDirection
import com.xanderjs.haptimap.designsystem.atoms.DistanceText
import com.xanderjs.haptimap.designsystem.atoms.HapticPulse
import com.xanderjs.haptimap.designsystem.atoms.IconDisc
import com.xanderjs.haptimap.designsystem.atoms.ModeTone
import com.xanderjs.haptimap.designsystem.atoms.PulseRings
import com.xanderjs.haptimap.designsystem.atoms.color
import com.xanderjs.haptimap.designsystem.atoms.onColor
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * Próxima maniobra (rediseño Pulso): la flecha en un disco del color del modo ([tone]), con su anillo, y la distancia
 * grande debajo. Cuando suena un aviso real ([pulse]) el anillo late a su ritmo una vez ([PulseRings]). En ambiente,
 * solo la flecha de contorno y la distancia. TalkBack lo lee como una sola frase ("Gira a la izquierda, 20 m").
 */
@Composable
fun ManeuverIndicator(
    direction: ArrowDirection,
    distanceMeters: Int,
    modifier: Modifier = Modifier,
    tone: ModeTone = ModeTone.Focus,
    pulse: HapticPulse? = null,
) {
    val dimens = HaptiMapTheme.dimens
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.s),
    ) {
        Box(Modifier.size(dimens.maneuverDisc + dimens.maneuverRingGap * 2), contentAlignment = Alignment.Center) {
            PulseRings(pulse, tone.color, Modifier.matchParentSize())
            IconDisc(
                painter = painterResource(if (HaptiMapTheme.ambient) direction.outlineIcon else direction.icon),
                contentDescription = stringResource(direction.description),
                color = tone.color,
                onColor = tone.onColor,
            )
        }
        DistanceText(meters = distanceMeters, unitColor = tone.color)
    }
}

@HaptiPreviews
@Composable
private fun ManeuverIndicatorPreview() = PreviewSurface {
    Row(horizontalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.m)) {
        ManeuverIndicator(direction = ArrowDirection.LEFT, distanceMeters = 20)
        ManeuverIndicator(direction = ArrowDirection.RIGHT, distanceMeters = 20, tone = ModeTone.Tour)
    }
}
