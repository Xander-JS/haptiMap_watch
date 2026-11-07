package com.xanderjs.haptimap.designsystem.organisms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Text
import com.xanderjs.haptimap.designsystem.R
import com.xanderjs.haptimap.designsystem.atoms.HaptiButton
import com.xanderjs.haptimap.designsystem.atoms.HaptiButtonVariant
import com.xanderjs.haptimap.designsystem.atoms.HaptiEdgeButton
import com.xanderjs.haptimap.designsystem.atoms.HaptiIcon
import com.xanderjs.haptimap.designsystem.atoms.HaptiTimeText
import com.xanderjs.haptimap.designsystem.atoms.HeadingText
import com.xanderjs.haptimap.designsystem.molecules.SuggestionCountdown
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.templates.GlanceScreenTemplate
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * Tarjeta de una sugerencia de POI a pantalla completa (paso 19): categoría (icono), nombre, tiempo extra estimado
 * y una cuenta atrás visible. La acción principal, [acceptAction], va en el botón del borde ([HaptiEdgeButton]),
 * que queda en la primera vista en las tres formas (y es también lo que acepta el botón físico del reloj); omitir va
 * bajo el borde, y si no se toca nada la sugerencia se descarta sola al agotarse la cuenta atrás.
 *
 * Organismo genérico: recibe primitivos ya resueltos (textos, painter, progreso, callbacks), sin conocer el dominio
 * del Tour, así que el color de POI y la tipografía salen del tema y un cambio se propaga a toda la app. La acción
 * principal es un hueco (quien lo usa pone su HaptiEdgeButton con su etiqueta de recurso, como el resto de la app).
 * Fondo opaco: se superpone a la navegación tapándola mientras dura. El icono lleva [categoryName] como descripción
 * para que TalkBack anuncie la categoría; el nombre va en el encabezado.
 */
@Composable
fun SuggestionCard(
    title: String,
    categoryIcon: Painter,
    categoryName: String,
    addedTime: String,
    secondsRemaining: Int,
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
            glance = {
                HaptiIcon(painter = categoryIcon, contentDescription = categoryName, tint = colors.poiHighlight)
                HeadingText(title)
                Text(
                    text = addedTime,
                    style = HaptiMapTheme.typography.label,
                    color = colors.poiHighlight,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                SuggestionCountdown(secondsRemaining = secondsRemaining, progress = progress, contentDescription = countdownDescription)
            },
            belowFold = {
                HaptiButton(text = dismissLabel, onClick = onDismiss, modifier = Modifier.fillMaxWidth(), variant = HaptiButtonVariant.Secondary)
            },
            edgeButton = acceptAction,
        )
    }
}

@HaptiPreviews
@Composable
private fun SuggestionCardPreview() = HaptiMapTheme {
    AppScaffold(timeText = { HaptiTimeText() }) {
        SuggestionCard(
            title = stringResource(R.string.sample_place_museum),
            categoryIcon = painterResource(R.drawable.ic_exploration_museum),
            categoryName = stringResource(R.string.sample_category_cafe),
            addedTime = "+3 min",
            secondsRemaining = 5,
            progress = 0.6f,
            countdownDescription = "5 s",
            dismissLabel = stringResource(R.string.sample_confirm_no),
            onDismiss = {},
            acceptAction = { HaptiEdgeButton(text = stringResource(R.string.sample_button_primary), onClick = {}) },
        )
    }
}
