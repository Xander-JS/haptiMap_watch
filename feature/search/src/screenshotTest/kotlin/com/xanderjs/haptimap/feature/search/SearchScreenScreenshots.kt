package com.xanderjs.haptimap.feature.search

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.AppScaffold
import com.android.tools.screenshot.PreviewTest
import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.PlaceCategory
import com.xanderjs.haptimap.core.model.PlaceSearchError
import com.xanderjs.haptimap.designsystem.atoms.HaptiTimeText
import com.xanderjs.haptimap.designsystem.atoms.LocalOffline
import com.xanderjs.haptimap.designsystem.organisms.ConfirmDialogContent
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme
import androidx.compose.runtime.CompositionLocalProvider
import com.xanderjs.haptimap.designsystem.R as DsR

// Paso 11: la pantalla de búsqueda y la de resultados en las 12 combinaciones de @HaptiPreviews.
// Referencias: ./gradlew :feature:search:updateDebugScreenshotTest; comprobar: validateDebugScreenshotTest.

@Composable
private fun Screen(offline: Boolean = false, content: @Composable () -> Unit) = HaptiMapTheme {
    CompositionLocalProvider(LocalOffline provides offline) {
        AppScaffold(timeText = { HaptiTimeText() }) { content() }
    }
}

@Composable
private fun places() = listOf(
    Place("1", stringResource(DsR.string.sample_place_pharmacy), GeoPoint(0.0, 0.0), PlaceCategory.PHARMACY, stringResource(DsR.string.sample_address_pharmacy), 120),
    Place("2", stringResource(DsR.string.sample_place_cafe), GeoPoint(0.0, 0.0), PlaceCategory.CAFE, stringResource(DsR.string.sample_address_cafe), 850),
    Place("3", stringResource(DsR.string.sample_place_museum), GeoPoint(0.0, 0.0), null, null, 1240),
)

@Composable
private fun Home(state: SearchUiState, offline: Boolean = false) = Screen(offline) { SearchContent(state, {}, {}, {}) }

@Composable
private fun Results(state: SearchUiState) = Screen { SearchResultsContent(state, {}, {}) }

/** La primera vista al abrir la app: título, voz y las primeras categorías. */
@PreviewTest @HaptiPreviews @Composable
fun SearchHome() = Home(SearchUiState())

/** Navegando y sin red: la ruta se retoma arriba; el aviso dice que buscar necesita internet. */
@PreviewTest @HaptiPreviews @Composable
fun SearchHomeNavigatingOffline() = Home(
    SearchUiState(offline = true, activeRoute = ActiveRouteUi(stringResource(DsR.string.sample_place_museum), navigating = true)),
    offline = true,
)

/** Una ruta cargada sin empezar, la última categoría buscada resaltada y sin reconocedor de voz. */
@PreviewTest @HaptiPreviews @Composable
fun SearchHomeRouteReadyNoVoice() = Home(
    SearchUiState(
        query = SearchQuery.Category(PlaceCategory.STORE),
        activeRoute = ActiveRouteUi(stringResource(DsR.string.sample_place_museum), navigating = false),
        voiceUnavailable = true,
    ),
)

@PreviewTest @HaptiPreviews @Composable
fun SearchResultsCategory() = Results(SearchUiState(query = SearchQuery.Category(PlaceCategory.PHARMACY), places = places()))

/** Lo dictado como título, entre comillas. */
@PreviewTest @HaptiPreviews @Composable
fun SearchResultsVoice() = Results(SearchUiState(query = SearchQuery.Text(stringResource(DsR.string.sample_place_museum)), places = places()))

@PreviewTest @HaptiPreviews @Composable
fun SearchResultsLoading() = Results(SearchUiState(query = SearchQuery.Category(PlaceCategory.CAFE), status = SearchStatus.Loading))

/** Vacío: sin reintentar, se vuelve atrás para buscar otra cosa. */
@PreviewTest @HaptiPreviews @Composable
fun SearchResultsEmpty() = Results(SearchUiState(query = SearchQuery.Category(PlaceCategory.CAFE), status = SearchStatus.Failed(PlaceSearchError.NO_RESULTS)))

/** El mensaje más largo con su botón: debe caber en la primera vista sin scroll. */
@PreviewTest @HaptiPreviews @Composable
fun SearchResultsNoLocationRetry() = Results(SearchUiState(query = SearchQuery.Category(PlaceCategory.CAFE), status = SearchStatus.Failed(PlaceSearchError.NO_LOCATION)))

@PreviewTest @HaptiPreviews @Composable
fun SearchResultsNoPermission() = Results(SearchUiState(query = SearchQuery.Category(PlaceCategory.CAFE), status = SearchStatus.NoPermission))

/** Elegir un lugar con una ruta activa (ConfirmDialogContent: layoutlib no dibuja ventanas de diálogo). */
@PreviewTest @HaptiPreviews @Composable
fun SearchReplaceRoute() = PreviewSurface {
    ConfirmDialogContent(
        title = stringResource(R.string.search_replace_title),
        confirmLabel = stringResource(R.string.search_replace_confirm),
        dismissLabel = stringResource(R.string.search_replace_keep),
        onConfirm = {},
        onDismiss = {},
    )
}
