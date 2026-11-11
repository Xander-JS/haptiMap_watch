package com.xanderjs.haptimap.designsystem.atoms

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * La acción principal de una pantalla, pegada al borde inferior (EdgeButton de Wear M3, paso 13): siempre en la
 * primera vista, en la franja curva que el contenido no usa. En el color de [tone]: volt, o orquídea en las acciones del Tour.
 * Solo dentro de una plantilla que lo admita (GlanceScreenTemplate(edgeButton = …)).
 *
 * La etiqueta (una o dos palabras, DESIGN_SYSTEM §19) sigue el tamaño de letra del sistema; si con letra grande no
 * cabe en el ancho del botón, que fija la forma, se reduce hasta su tamaño sin escalar en vez de cortarse (paso 15A:
 * "Siguiente" salía "Sigue…" en la cuadrada a 1,24).
 */
@Composable
fun HaptiEdgeButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, tone: ModeTone = ModeTone.Focus) {
    val container = tone.color
    val content = tone.onColor
    EdgeButton(
        onClick = onClick,
        modifier = modifier,
        buttonSize = EdgeButtonSize.Small,
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
    ) {
        val label = HaptiMapTheme.typography.label
        val unscaled = (label.fontSize.value / LocalDensity.current.fontScale.coerceAtLeast(1f)).sp
        BasicText(
            text = text,
            style = label.copy(color = content, textAlign = TextAlign.Center),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            autoSize = TextAutoSize.StepBased(minFontSize = unscaled, maxFontSize = label.fontSize),
        )
    }
}
