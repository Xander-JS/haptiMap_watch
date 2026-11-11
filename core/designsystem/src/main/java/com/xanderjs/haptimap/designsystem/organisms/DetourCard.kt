package com.xanderjs.haptimap.designsystem.organisms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.AppScaffold
import com.xanderjs.haptimap.designsystem.R
import com.xanderjs.haptimap.designsystem.atoms.HaptiEdgeButton
import com.xanderjs.haptimap.designsystem.atoms.IconDisc
import com.xanderjs.haptimap.designsystem.atoms.ModeTone
import com.xanderjs.haptimap.designsystem.atoms.HaptiTimeText
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.templates.GlanceScreenTemplate
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * Tarjeta del desvío a un POI del Modo Tour (paso 20): el disco de la categoría en el color del Tour, el nombre y un [status] (la distancia
 * al POI mientras se va, o el estado "ya llegaste"/"volviendo"). Se superpone a la navegación mientras dura el desvío:
 * las maniobras las da el háptico, así que la pantalla muestra a dónde se va y cómo cancelar, sin tapar lo esencial.
 * Cancelar ([cancelAction]) va en el botón del borde ([HaptiEdgeButton]), como [SuggestionCard]: siempre accesible en
 * la primera vista sin hacer scroll, en las tres formas de pantalla y en cualquier escala de letra.
 *
 * Organismo genérico, como [SuggestionCard]: recibe primitivos ya resueltos (textos, painter, hueco de acción), sin conocer
 * el dominio del Tour. El color de POI y la tipografía salen del tema, así un cambio se propaga a toda la app.
 */
@Composable
fun DetourCard(
    title: String,
    categoryIcon: Painter,
    categoryName: String,
    status: String,
    cancelAction: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HaptiMapTheme.colors
    Box(modifier.fillMaxSize().background(colors.background)) {
        GlanceScreenTemplate(
            // igual que la tarjeta de sugerencia: icono + nombre en una fila (una línea) y el estado debajo, centrado y
            // siempre encima del botón del borde en cualquier tamaño de letra y forma.
            // como la tarjeta de sugerencia: el disco del POI, el nombre en una línea y el estado debajo, centrado y
            // siempre encima del botón del borde en cualquier tamaño de letra y forma.
            glance = {
                IconDisc(categoryIcon, categoryName, colors.tour, colors.onTour)
                PoiTitle(title)
                PoiStatus(status)
            },
            edgeButton = cancelAction,
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
            cancelAction = { HaptiEdgeButton(text = stringResource(R.string.sample_button_secondary), onClick = {}, tone = ModeTone.Tour) },
        )
    }
}
