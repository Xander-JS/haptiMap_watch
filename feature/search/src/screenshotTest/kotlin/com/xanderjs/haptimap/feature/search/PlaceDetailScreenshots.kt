package com.xanderjs.haptimap.feature.search

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.AppScaffold
import com.android.tools.screenshot.PreviewTest
import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.PlaceCategory
import com.xanderjs.haptimap.designsystem.atoms.HaptiTimeText
import com.xanderjs.haptimap.designsystem.preview.HaptiLargeFontPreviews
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme
import com.xanderjs.haptimap.designsystem.R as DsR

// Detalle de un lugar (tras la lista de resultados), en las 12 combinaciones y con la letra más grande.
// Regla de la primera vista: el disco, el nombre, la distancia y "Ir" se ven enteros sin scroll (PlaceDetailFirstViewTest).

@Composable
private fun places() = listOf(
    Place("1", stringResource(DsR.string.sample_place_pharmacy), GeoPoint(0.0, 0.0), PlaceCategory.PHARMACY, stringResource(DsR.string.sample_address_pharmacy), 120),
    Place("2", stringResource(DsR.string.sample_place_cafe), GeoPoint(0.0, 0.0), PlaceCategory.CAFE, stringResource(DsR.string.sample_address_cafe), 850),
    Place("3", stringResource(DsR.string.sample_place_museum), GeoPoint(0.0, 0.0), null, stringResource(DsR.string.sample_address_cafe), 1240),
    Place("4", stringResource(DsR.string.sample_place_museum), GeoPoint(0.0, 0.0), PlaceCategory.STORE, null, 2300),
    Place("5", stringResource(DsR.string.sample_place_long), GeoPoint(0.0, 0.0), PlaceCategory.TRANSIT, stringResource(DsR.string.sample_address_pharmacy), 640),
)

@Composable
private fun Detail(page: Int) = HaptiMapTheme {
    AppScaffold(timeText = { HaptiTimeText() }) { PlaceDetailContent(SearchUiState(places = places(), detailPage = page), onGo = {}) }
}

/** Primera página, con categoría y dirección: el indicador marca el primer punto. */
@PreviewTest @HaptiPreviews @Composable
fun PlaceDetailFirst() = Detail(0)

/** Una página del medio: se abre en el lugar tocado, no en el primero. */
@PreviewTest @HaptiPreviews @Composable
fun PlaceDetailMiddle() = Detail(1)

/** Sin categoría (búsqueda por texto): el pin de lugar ocupa el disco y la página se ve completa. */
@PreviewTest @HaptiPreviews @Composable
fun PlaceDetailNoCategory() = Detail(2)

/** Sin dirección: queda el tiempo a pie como único detalle. */
@PreviewTest @HaptiPreviews @Composable
fun PlaceDetailNoAddress() = Detail(3)

/** El nombre más largo (en español aún más): dos líneas y puntos suspensivos, el botón sigue a la vista. */
@PreviewTest @HaptiPreviews @Composable
fun PlaceDetailLongName() = Detail(4)

@PreviewTest @HaptiLargeFontPreviews @Composable
fun LargeFontPlaceDetailFirst() = Detail(0)

@PreviewTest @HaptiLargeFontPreviews @Composable
fun LargeFontPlaceDetailNoCategory() = Detail(2)

@PreviewTest @HaptiLargeFontPreviews @Composable
fun LargeFontPlaceDetailLongName() = Detail(4)
