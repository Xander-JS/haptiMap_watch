package com.xanderjs.haptimap.designsystem.atoms

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.Text
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * La acción principal de una pantalla, pegada al borde inferior (EdgeButton de Wear M3, paso 13): siempre en la
 * primera vista, en la franja curva que el contenido no usa. Colores de la variante principal de [HaptiButton].
 * Solo dentro de una plantilla que lo admita (GlanceScreenTemplate(edgeButton = …)).
 */
@Composable
fun HaptiEdgeButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = HaptiMapTheme.colors
    EdgeButton(
        onClick = onClick,
        modifier = modifier,
        buttonSize = EdgeButtonSize.Small,
        colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
    ) {
        Text(text, style = HaptiMapTheme.typography.label, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
