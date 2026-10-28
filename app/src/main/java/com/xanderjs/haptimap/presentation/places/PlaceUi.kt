package com.xanderjs.haptimap.presentation.places

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.xanderjs.haptimap.R
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.PlaceCategory
import com.xanderjs.haptimap.core.model.PlaceSearchError
import com.xanderjs.haptimap.designsystem.molecules.PlaceRowItem
import com.xanderjs.haptimap.designsystem.organisms.StatusMessage
import com.xanderjs.haptimap.designsystem.organisms.StatusTone

// Traducción del dominio de búsqueda a valores del sistema de diseño. Vive en :app con la pantalla
// de Places debug; el paso 11 la lleva a :feature:search.

/** Lo que una búsqueda muestra mientras no hay resultados: cargando, o por qué no los hay (vacío incluido). */
sealed interface SearchStatus {
    data object Loading : SearchStatus
    data class Failed(val error: PlaceSearchError) : SearchStatus
}

@Composable
fun SearchStatusMessage(status: SearchStatus, modifier: Modifier = Modifier) = when (status) {
    SearchStatus.Loading -> StatusMessage(stringResource(R.string.search_loading), modifier, loading = true)
    is SearchStatus.Failed -> StatusMessage(stringResource(status.error.message), modifier, tone = status.error.tone)
}

/** Nombre visible de la categoría; el enum de core:model no conoce idiomas. */
@get:StringRes
val PlaceCategory.label: Int
    get() = when (this) {
        PlaceCategory.STORE -> R.string.category_store
        PlaceCategory.HARDWARE_STORE -> R.string.category_hardware_store
        PlaceCategory.RESTAURANT -> R.string.category_restaurant
        PlaceCategory.CAFE -> R.string.category_cafe
        PlaceCategory.PHARMACY -> R.string.category_pharmacy
        PlaceCategory.TRANSIT -> R.string.category_transit
        PlaceCategory.SUPERMARKET -> R.string.category_supermarket
    }

@Composable
fun Place.toRowItem() = PlaceRowItem(name, distanceMeters, category?.let { stringResource(it.label) })

@get:StringRes
private val PlaceSearchError.message: Int
    get() = when (this) {
        PlaceSearchError.NO_CONNECTION -> R.string.search_error_no_connection
        PlaceSearchError.NO_LOCATION -> R.string.search_error_no_location
        PlaceSearchError.NO_RESULTS -> R.string.search_error_no_results
        PlaceSearchError.API_QUOTA -> R.string.search_error_quota
        PlaceSearchError.API_INVALID_KEY -> R.string.search_error_invalid_key
        PlaceSearchError.API_ERROR -> R.string.search_error_api
    }

// vacío no es un fallo; sin red ni ubicación se puede esperar (como el aviso de la hora); lo demás es error
private val PlaceSearchError.tone: StatusTone
    get() = when (this) {
        PlaceSearchError.NO_RESULTS -> StatusTone.Muted
        PlaceSearchError.NO_CONNECTION, PlaceSearchError.NO_LOCATION -> StatusTone.Warning
        else -> StatusTone.Error
    }
