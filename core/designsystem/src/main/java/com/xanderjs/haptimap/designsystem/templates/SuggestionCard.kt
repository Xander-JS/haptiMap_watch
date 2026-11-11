package com.xanderjs.haptimap.designsystem.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
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
import com.xanderjs.haptimap.designsystem.atoms.HaptiEdgeButton
import com.xanderjs.haptimap.designsystem.atoms.IconDisc
import com.xanderjs.haptimap.designsystem.atoms.ModeTone
import com.xanderjs.haptimap.designsystem.atoms.HaptiTimeText
import com.xanderjs.haptimap.designsystem.molecules.SuggestionCountdown
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * Tarjeta de una sugerencia de POI a pantalla completa (paso 19, rediseño Pulso): el disco del POI en orquídea (el
 * color del Tour) dentro del anillo de la cuenta atrás, el nombre y el tiempo extra. La acción principal,
 * [acceptAction], va en el botón del borde ([HaptiEdgeButton], en tono Tour), que queda en la primera vista en las
 * tres formas (y es también lo que acepta el botón físico del reloj); omitir va bajo el borde, y si no se toca nada la
 * sugerencia se descarta sola al agotarse la cuenta atrás.
 *
 * Plantilla genérica (especializa [GlanceScreenTemplate], por eso vive en templates/ y no en organisms/): recibe primitivos ya resueltos (textos, painter, progreso, callbacks), sin conocer el dominio
 * del Tour, así que el color y la tipografía salen del tema y un cambio se propaga a toda la app. Fondo opaco: se
 * superpone a la navegación tapándola mientras dura. El disco lleva [categoryName] como descripción y el anillo
 * [countdownDescription] (los segundos), para TalkBack; el nombre va en el encabezado.
 */
@Composable
fun SuggestionCard(
    title: String,
    categoryIcon: Painter,
    categoryName: String,
    addedTime: String,
    progress: Float,
    countdownDescription: String,
    dismissLabel: String,
    onDismiss: () -> Unit,
    acceptAction: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HaptiMapTheme.colors
    Box(modifier.fillMaxSize().background(colors.background)) {
        GlanceScreenTemplate(
            // lo esencial (anillo con el POI y el nombre en una línea) va centrado y siempre encima del botón del borde,
            // en cualquier tamaño de letra y forma. El tiempo extra es un detalle: baja bajo el pliegue si no cabe.
            glance = {
                SuggestionCountdown(progress = progress, contentDescription = countdownDescription) {
                    IconDisc(categoryIcon, categoryName, colors.tour, colors.onTour, size = HaptiMapTheme.dimens.poiDisc)
                }
                PoiTitle(title)
            },
            details = listOf { PoiStatus(addedTime) },
            belowFold = {
                HaptiButton(text = dismissLabel, onClick = onDismiss, modifier = Modifier.fillMaxWidth(), variant = HaptiButtonVariant.Secondary)
            },
            edgeButton = acceptAction,
        )
    }
}

/** El nombre del POI en las tarjetas del Tour: una línea, encabezado para TalkBack. */
@Composable
internal fun PoiTitle(title: String) = Text(
    text = title,
    modifier = Modifier.fillMaxWidth(HaptiMapTheme.dimens.centeredContentWidthFraction).semantics { heading() },
    style = HaptiMapTheme.typography.title,
    color = HaptiMapTheme.colors.onBackground,
    textAlign = TextAlign.Center,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
)

/** La línea de estado de las tarjetas del Tour (tiempo extra, distancia al POI), en el color del Tour. */
@Composable
internal fun PoiStatus(text: String) = Text(
    text = text,
    style = HaptiMapTheme.typography.label,
    color = HaptiMapTheme.colors.tour,
    textAlign = TextAlign.Center,
    maxLines = 2,
    overflow = TextOverflow.Ellipsis,
)

@HaptiPreviews
@Composable
private fun SuggestionCardPreview() = HaptiMapTheme {
    AppScaffold(timeText = { HaptiTimeText() }) {
        SuggestionCard(
            title = stringResource(R.string.sample_place_museum),
            categoryIcon = painterResource(R.drawable.ic_exploration_museum),
            categoryName = stringResource(R.string.sample_category_cafe),
            addedTime = "+3 min",
            progress = 0.6f,
            countdownDescription = "5 s",
            dismissLabel = stringResource(R.string.sample_confirm_no),
            onDismiss = {},
            acceptAction = { HaptiEdgeButton(text = stringResource(R.string.sample_button_primary), onClick = {}, tone = ModeTone.Tour) },
        )
    }
}
