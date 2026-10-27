package com.xanderjs.haptimap.designsystem.organisms

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.Text
import com.xanderjs.haptimap.core.model.PlaceSearchError
import com.xanderjs.haptimap.designsystem.R
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/** Lo que una búsqueda muestra mientras no hay resultados: cargando, o por qué no los hay (vacío incluido). */
sealed interface SearchStatus {
    data object Loading : SearchStatus
    data class Failed(val error: PlaceSearchError) : SearchStatus
}

/** Estado de una búsqueda en el centro de la pantalla. TalkBack lo anuncia al cambiar. */
@Composable
fun StatusMessage(status: SearchStatus, modifier: Modifier = Modifier) {
    val colors = HaptiMapTheme.colors
    Column(
        modifier = modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.m),
    ) {
        val (text, color) = when (status) {
            SearchStatus.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(HaptiMapTheme.dimens.iconMedium),
                    colors = ProgressIndicatorDefaults.colors(indicatorColor = colors.primary, trackColor = Color.Transparent),
                )
                stringResource(R.string.search_loading) to colors.onBackground
            }
            is SearchStatus.Failed -> stringResource(status.error.message) to status.error.color()
        }
        Text(text, style = HaptiMapTheme.typography.body, color = color, textAlign = TextAlign.Center)
    }
}

/** Único mapeo de PlaceSearchError a texto visible. */
@get:StringRes
val PlaceSearchError.message: Int
    get() = when (this) {
        PlaceSearchError.NO_CONNECTION -> R.string.search_error_no_connection
        PlaceSearchError.NO_LOCATION -> R.string.search_error_no_location
        PlaceSearchError.NO_RESULTS -> R.string.search_error_no_results
        PlaceSearchError.API_QUOTA -> R.string.search_error_quota
        PlaceSearchError.API_INVALID_KEY -> R.string.search_error_invalid_key
        PlaceSearchError.API_ERROR -> R.string.search_error_api
    }

// vacío no es un fallo; sin red es un aviso (como el de la hora); lo demás es error
@Composable
private fun PlaceSearchError.color(): Color = when (this) {
    PlaceSearchError.NO_RESULTS -> HaptiMapTheme.colors.onSurfaceVariant
    PlaceSearchError.NO_CONNECTION, PlaceSearchError.NO_LOCATION -> HaptiMapTheme.colors.warning
    else -> HaptiMapTheme.colors.error
}

@HaptiPreviews
@Composable
private fun StatusMessageLoadingPreview() = PreviewSurface { StatusMessage(SearchStatus.Loading) }

@HaptiPreviews
@Composable
private fun StatusMessageEmptyPreview() = PreviewSurface { StatusMessage(SearchStatus.Failed(PlaceSearchError.NO_RESULTS)) }

@HaptiPreviews
@Composable
private fun StatusMessageErrorPreview() = PreviewSurface { StatusMessage(SearchStatus.Failed(PlaceSearchError.API_INVALID_KEY)) }
