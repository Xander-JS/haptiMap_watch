package com.xanderjs.haptimap.designsystem.organisms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Text
import com.xanderjs.haptimap.designsystem.R
import com.xanderjs.haptimap.designsystem.atoms.HaptiButton
import com.xanderjs.haptimap.designsystem.atoms.HaptiButtonVariant
import com.xanderjs.haptimap.designsystem.atoms.HaptiIcon
import com.xanderjs.haptimap.designsystem.atoms.HaptiTimeText
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.templates.GlanceScreenTemplate
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * Tarjeta del desvío a un POI del Modo Tour (paso 20): categoría (icono), nombre del POI y un [status] (la distancia
 * al POI mientras se va, o el estado "ya llegaste"/"volviendo"). Se superpone a la navegación mientras dura el desvío:
 * las maniobras las da el háptico, así que la pantalla muestra a dónde se va y cómo cancelar, sin tapar lo esencial.
 * Cancelar ([onCancel]) va bajo el borde, como "Omitir" en [SuggestionCard]: una acción deliberada, no en el borde.
 *
 * Organismo genérico, como [SuggestionCard]: recibe primitivos ya resueltos (textos, painter, callback), sin conocer
 * el dominio del Tour. El color de POI y la tipografía salen del tema, así un cambio se propaga a toda la app.
 */
@Composable
fun DetourCard(
    title: String,
    categoryIcon: Painter,
    categoryName: String,
    status: String,
    cancelLabel: String,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HaptiMapTheme.colors
    Box(modifier.fillMaxSize().background(colors.background)) {
        GlanceScreenTemplate(
            // igual que la tarjeta de sugerencia: icono + nombre en una fila (una línea) y el estado debajo, centrado y
            // siempre encima del borde en cualquier tamaño de letra; cancelar queda bajo el pliegue.
            glance = {
                Row(
                    modifier = Modifier.fillMaxWidth(HaptiMapTheme.dimens.centeredContentWidthFraction),
                    horizontalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.s, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HaptiIcon(painter = categoryIcon, contentDescription = categoryName, tint = colors.poiHighlight)
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f, fill = false).semantics { heading() },
                        style = HaptiMapTheme.typography.title,
                        color = colors.onBackground,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = status,
                    style = HaptiMapTheme.typography.label,
                    color = colors.poiHighlight,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            belowFold = {
                HaptiButton(text = cancelLabel, onClick = onCancel, modifier = Modifier.fillMaxWidth(), variant = HaptiButtonVariant.Secondary)
            },
        )
    }
}

@HaptiPreviews
@Composable
private fun DetourCardPreview() = HaptiMapTheme {
    AppScaffold(timeText = { HaptiTimeText() }) {
        DetourCard(
            title = stringResource(R.string.sample_place_cafe),
            categoryIcon = painterResource(R.drawable.ic_category_restaurant),
            categoryName = stringResource(R.string.sample_category_cafe),
            status = "120 m",
            cancelLabel = stringResource(R.string.sample_confirm_no),
            onCancel = {},
        )
    }
}
