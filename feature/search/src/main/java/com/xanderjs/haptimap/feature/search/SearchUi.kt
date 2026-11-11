package com.xanderjs.haptimap.feature.search

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.PlaceCategory
import com.xanderjs.haptimap.core.model.PlaceSearchError
import com.xanderjs.haptimap.designsystem.atoms.StatusTone
import com.xanderjs.haptimap.designsystem.molecules.PlaceRowItem
import com.xanderjs.haptimap.designsystem.organisms.StatusMessage
import com.xanderjs.haptimap.designsystem.templates.PlaceDetailItem
import com.xanderjs.haptimap.designsystem.R as DsR

// Traducción del dominio de búsqueda a valores del sistema de diseño (desde el paso 04 en :app).

/** Lo que una búsqueda muestra mientras no hay resultados: cargando, o por qué no los hay (vacío incluido). */
sealed interface SearchStatus {
    data object Loading : SearchStatus
    data class Failed(val error: PlaceSearchError) : SearchStatus

    /** Sin permiso de ubicación: no es un error de Places, y reintentar no lo arregla. */
    data object NoPermission : SearchStatus
}

/**
 * Reintentar sirve para la red, el GPS, la cuota o un fallo de la API. No sin permiso ni con la clave mal
 * (volver a pedir no lo arregla), ni sin resultados (lo útil es volver y buscar otra cosa).
 */
val SearchStatus.canRetry: Boolean
    get() = this is SearchStatus.Failed && error != PlaceSearchError.API_INVALID_KEY && error != PlaceSearchError.NO_RESULTS

@Composable
fun SearchStatusMessage(status: SearchStatus, modifier: Modifier = Modifier) = when (status) {
    SearchStatus.Loading -> StatusMessage(stringResource(R.string.search_loading), modifier, loading = true)
    is SearchStatus.Failed -> StatusMessage(stringResource(status.error.message), modifier, tone = status.error.tone)
    SearchStatus.NoPermission -> StatusMessage(stringResource(R.string.search_error_no_permission), modifier, tone = StatusTone.Warning)
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

/** Icono de la categoría, del juego de iconos del sistema de diseño (Material, relleno, monocromo). */
@get:DrawableRes
val PlaceCategory.icon: Int
    get() = when (this) {
        PlaceCategory.STORE -> DsR.drawable.ic_category_store
        PlaceCategory.HARDWARE_STORE -> DsR.drawable.ic_category_hardware_store
        PlaceCategory.RESTAURANT -> DsR.drawable.ic_category_restaurant
        PlaceCategory.CAFE -> DsR.drawable.ic_category_cafe
        PlaceCategory.PHARMACY -> DsR.drawable.ic_category_pharmacy
        PlaceCategory.TRANSIT -> DsR.drawable.ic_category_transit
        PlaceCategory.SUPERMARKET -> DsR.drawable.ic_category_supermarket
    }

@Composable
fun Place.toRowItem() = PlaceRowItem(name, distanceMeters, category?.let { stringResource(it.label) }, address)

/** El detalle de un lugar (pantalla tras la lista). Sin categoría, el pin de lugar: la página se ve completa igual. */
@Composable
fun Place.toDetailItem(): PlaceDetailItem {
    val minutes = walkingMinutes(distanceMeters)
    return PlaceDetailItem(
        name = name,
        icon = painterResource(category?.icon ?: DsR.drawable.ic_arrive),
        categoryName = category?.let { stringResource(it.label) },
        distanceMeters = distanceMeters,
        address = address,
        walkingTime = stringResource(R.string.place_walk_time, minutes),
        walkingTimeDescription = pluralStringResource(R.plurals.place_walk_time_description, minutes, minutes),
    )
}

/**
 * Minutos a pie, en local y sin pedir nada a la red: la distancia en línea recta a 80 m/min (≈ 4,8 km/h), redondeada
 * hacia arriba y al menos 1. La pantalla lo marca como aproximado ("~").
 * shortcut: la línea recta subestima el camino real; si se pide la ruta antes del detalle, usar su duración.
 */
internal fun walkingMinutes(meters: Int): Int = ((meters + WALK_METERS_PER_MINUTE - 1) / WALK_METERS_PER_MINUTE).coerceAtLeast(1)

private const val WALK_METERS_PER_MINUTE = 80

/** Título de la lista de resultados: la categoría, o lo que se dijo entre comillas. */
@Composable
fun SearchQuery.title(): String = when (this) {
    is SearchQuery.Category -> stringResource(category.label)
    is SearchQuery.Text -> stringResource(R.string.search_results_text, text)
}

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
