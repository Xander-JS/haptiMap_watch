package com.xanderjs.haptimap.designsystem.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.AnimatedPage
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.HorizontalPageIndicator
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.Text
import com.xanderjs.haptimap.designsystem.R
import com.xanderjs.haptimap.designsystem.atoms.DistanceText
import com.xanderjs.haptimap.designsystem.atoms.HaptiEdgeButton
import com.xanderjs.haptimap.designsystem.atoms.HaptiTimeText
import com.xanderjs.haptimap.designsystem.atoms.HeadingText
import com.xanderjs.haptimap.designsystem.atoms.IconDisc
import com.xanderjs.haptimap.designsystem.atoms.formatDistance
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * Lo que muestra el detalle de un lugar, ya resuelto (textos traducidos, icono, metros): la plantilla no conoce el
 * dominio. [categoryName] null = el lugar no tiene categoría (búsqueda por texto); [address] null = Places no la da.
 * [walkingTime] es lo que se ve ("~12 min a pie") y [walkingTimeDescription] lo que lee TalkBack (sin la tilde).
 */
data class PlaceDetailItem(
    val name: String,
    val icon: Painter,
    val categoryName: String?,
    val distanceMeters: Int,
    val address: String?,
    val walkingTime: String,
    val walkingTimeDescription: String,
)

/**
 * Detalle de un lugar a pantalla completa (especializa [GlanceScreenTemplate], como [SuggestionCard]). En la primera
 * vista, siempre: el icono de la categoría en un disco azul (el color de tu ruta: este lugar puede ser tu destino)
 * con la distancia a su lado, y el nombre debajo. Debajo, por prioridad, la dirección y el tiempo a pie; bajan con scroll si no
 * caben. La acción principal ([goAction], un [HaptiEdgeButton] "Ir") va en el borde.
 *
 * TalkBack lee la página como una sola frase (nombre, categoría, distancia, dirección, tiempo a pie y [position],
 * "Lugar 2 de 6"); los textos sueltos no se leen dos veces.
 */
@Composable
fun PlaceDetailCard(item: PlaceDetailItem, position: String, goAction: @Composable () -> Unit, modifier: Modifier = Modifier) {
    val colors = HaptiMapTheme.colors
    val round = LocalConfiguration.current.isScreenRound
    val phrase = listOfNotNull(item.name, item.categoryName, formatDistance(item.distanceMeters), item.address, item.walkingTimeDescription)
        .joinToString(", ") + ". " + position
    GlanceScreenTemplate(
        modifier = modifier,
        glance = {
            Column(
                modifier = Modifier.clearAndSetSemantics {
                    contentDescription = phrase
                    heading()
                },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.xs),
            ) {
                // disco y distancia en una fila: apilados con el nombre no cabían encima de "Ir" en la redonda pequeña
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.m)) {
                    IconDisc(item.icon, null, colors.primary, colors.onPrimary, size = HaptiMapTheme.dimens.poiDisc)
                    DistanceText(item.distanceMeters, style = HaptiMapTheme.typography.distanceMedium)
                }
                // en la cuadrada el botón del borde ocupa más alto: con dos líneas el nombre quedaba bajo "Ir"
                HeadingText(item.name, maxLines = if (round) 2 else 1)
            }
        },
        details = listOfNotNull(item.address?.let { address -> @Composable { DetailLine(address) } }, @Composable { DetailLine(item.walkingTime) }),
        edgeButton = goAction,
    )
}

/**
 * Los lugares de una búsqueda, uno por página (HorizontalPagerScaffold de Wear M3, con su indicador de página), abriendo
 * en [initialPage]. Sin flechas: el indicador ya dice que hay más, y las flechas quitarían área táctil en la parte más
 * estrecha de la pantalla redonda. En la primera página, deslizar desde el borde izquierdo sigue volviendo a la lista
 * (PagerDefaults.gestureInclusion reserva ese borde para el swipe-to-dismiss). [goAction] recibe la página.
 */
@Composable
fun PlaceDetailPager(
    items: List<PlaceDetailItem>,
    initialPage: Int,
    goAction: @Composable (page: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(initialPage = initialPage.coerceIn(0, (items.size - 1).coerceAtLeast(0))) { items.size }
    val colors = HaptiMapTheme.colors
    HorizontalPagerScaffold(
        pagerState = pagerState,
        modifier = modifier,
        pageIndicator = { HorizontalPageIndicator(pagerState, selectedColor = colors.onBackground, unselectedColor = colors.outline, backgroundColor = colors.background) },
    ) {
        HorizontalPager(state = pagerState) { page ->
            AnimatedPage(pageIndex = page, pagerState = pagerState) {
                PlaceDetailCard(
                    item = items[page],
                    position = stringResource(R.string.place_position, page + 1, items.size),
                    goAction = { goAction(page) },
                )
            }
        }
    }
}

/** Una línea de detalle: centrada, secundaria, hasta 2 líneas; ya va en la frase de la página, TalkBack no la repite. */
@Composable
private fun DetailLine(text: String) = Text(
    text = text,
    modifier = Modifier.fillMaxWidth(HaptiMapTheme.dimens.centeredContentWidthFraction).clearAndSetSemantics {},
    style = HaptiMapTheme.typography.caption,
    color = HaptiMapTheme.colors.onSurfaceVariant,
    textAlign = TextAlign.Center,
    maxLines = 2,
    overflow = TextOverflow.Ellipsis,
)

/** Lugares de ejemplo para previews y catálogo; los textos salen de recursos. */
@Composable
fun samplePlaceDetails(): List<PlaceDetailItem> = listOf(
    PlaceDetailItem(
        stringResource(R.string.sample_place_pharmacy), painterResource(R.drawable.ic_category_pharmacy),
        stringResource(R.string.sample_category_pharmacy), 120, stringResource(R.string.sample_address_pharmacy),
        stringResource(R.string.sample_walk_time), stringResource(R.string.sample_walk_time_description),
    ),
    PlaceDetailItem(
        stringResource(R.string.sample_place_museum), painterResource(R.drawable.ic_arrive), null, 1240, null,
        stringResource(R.string.sample_walk_time), stringResource(R.string.sample_walk_time_description),
    ),
)

@HaptiPreviews
@Composable
private fun PlaceDetailPagerPreview() = HaptiMapTheme {
    AppScaffold(timeText = { HaptiTimeText() }) {
        PlaceDetailPager(
            items = samplePlaceDetails(),
            initialPage = 0,
            goAction = { HaptiEdgeButton(text = stringResource(R.string.sample_button_go), onClick = {}) },
        )
    }
}
