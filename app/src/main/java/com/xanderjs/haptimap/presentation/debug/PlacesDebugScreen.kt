package com.xanderjs.haptimap.presentation.debug

import android.app.Activity
import android.app.RemoteInput
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.input.RemoteInputIntentHelper
import com.xanderjs.haptimap.R
import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.PlaceCategory
import com.xanderjs.haptimap.core.model.PlaceSearchError
import com.xanderjs.haptimap.designsystem.atoms.CategoryChip
import com.xanderjs.haptimap.designsystem.atoms.HaptiButton
import com.xanderjs.haptimap.designsystem.atoms.HaptiButtonVariant
import com.xanderjs.haptimap.designsystem.atoms.HaptiTimeText
import com.xanderjs.haptimap.designsystem.organisms.PlaceResultsList
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.templates.RoundScreenTemplate
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme
import com.xanderjs.haptimap.feature.search.SearchStatus
import com.xanderjs.haptimap.feature.search.SearchStatusMessage
import com.xanderjs.haptimap.feature.search.icon
import com.xanderjs.haptimap.feature.search.label
import com.xanderjs.haptimap.feature.search.toRowItem
import com.xanderjs.haptimap.designsystem.R as DsR

private const val QUERY_KEY = "query"

/**
 * Pantalla temporal (paso 04): categorías y búsqueda por texto contra Places reales. El texto entra
 * por el RemoteInput del sistema (teclado/voz); la entrada por voz definitiva es del paso 11.
 */
@Composable
fun PlacesDebugScreen(onPlaceSelected: () -> Unit, viewModel: PlacesDebugViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val inputLabel = stringResource(R.string.places_debug_text_search)
    val textInput = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            RemoteInput.getResultsFromIntent(result.data)?.getCharSequence(QUERY_KEY)?.let { viewModel.searchText(it.toString()) }
        }
    }
    PlacesDebugContent(
        state = state,
        onCategory = viewModel::searchCategory,
        onTextSearch = {
            val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
            RemoteInputIntentHelper.putRemoteInputsExtra(intent, listOf(RemoteInput.Builder(QUERY_KEY).setLabel(inputLabel).build()))
            textInput.launch(intent)
        },
        onPlace = {
            viewModel.selectPlace(it)
            onPlaceSelected()
        },
    )
}

@Composable
private fun PlacesDebugContent(
    state: PlacesDebugUiState,
    onCategory: (PlaceCategory) -> Unit,
    onTextSearch: () -> Unit,
    onPlace: (Place) -> Unit,
) {
    RoundScreenTemplate {
        item { Text(stringResource(R.string.places_debug_title), style = HaptiMapTheme.typography.title, color = HaptiMapTheme.colors.primary) }
        // el resultado va arriba: tras pulsar una categoría se ve sin desplazarse
        state.status?.let { item { SearchStatusMessage(it) } }
        if (state.status == null && state.places.isNotEmpty()) item { PlaceResultsList(items = state.places.map { it.toRowItem() }, onItemClick = { onPlace(state.places[it]) }) }
        item {
            HaptiButton(
                text = stringResource(R.string.places_debug_text_search),
                onClick = onTextSearch,
                modifier = Modifier.fillMaxWidth(),
                variant = HaptiButtonVariant.Secondary,
            )
        }
        PlaceCategory.entries.forEach { category ->
            item {
                CategoryChip(
                    label = stringResource(category.label),
                    icon = painterResource(category.icon),
                    selected = category == state.selectedCategory,
                    onClick = { onCategory(category) },
                )
            }
        }
    }
}

@Composable
private fun previewPlaces() = listOf(
    Place("1", stringResource(DsR.string.sample_place_pharmacy), GeoPoint(0.0, 0.0), PlaceCategory.PHARMACY, null, 120),
    Place("2", stringResource(DsR.string.sample_place_cafe), GeoPoint(0.0, 0.0), PlaceCategory.CAFE, null, 850),
    Place("3", stringResource(DsR.string.sample_place_museum), GeoPoint(0.0, 0.0), null, null, 1240),
)

@HaptiPreviews
@Composable
private fun PlacesDebugResultsPreview() = HaptiMapTheme {
    AppScaffold(timeText = { HaptiTimeText() }) {
        PlacesDebugContent(PlacesDebugUiState(PlaceCategory.PHARMACY, previewPlaces()), {}, {}, {})
    }
}

@HaptiPreviews
@Composable
private fun PlacesDebugErrorPreview() = HaptiMapTheme {
    AppScaffold(timeText = { HaptiTimeText() }) {
        PlacesDebugContent(PlacesDebugUiState(PlaceCategory.CAFE, status = SearchStatus.Failed(PlaceSearchError.NO_CONNECTION)), {}, {}, {})
    }
}
