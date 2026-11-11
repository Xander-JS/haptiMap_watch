package com.xanderjs.haptimap.feature.search

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xanderjs.haptimap.designsystem.atoms.HaptiButton
import com.xanderjs.haptimap.designsystem.atoms.HaptiButtonVariant
import com.xanderjs.haptimap.designsystem.atoms.HeadingText
import com.xanderjs.haptimap.designsystem.organisms.PlaceResultsList
import com.xanderjs.haptimap.designsystem.templates.CenteredScreenTemplate
import com.xanderjs.haptimap.designsystem.templates.RoundScreenTemplate

/**
 * Resultados de la última búsqueda. [viewModel] es el de [SearchScreen] (la app lo comparte por la entrada de
 * la pila). Tocar un lugar abre su detalle ([onOpenDetail]), donde se elige como destino; [onExit], si no hay
 * búsqueda (el proceso se recreó y el ViewModel empezó de cero).
 */
@Composable
fun SearchResultsScreen(
    onOpenDetail: () -> Unit,
    onExit: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (state.query == null) {
        CenteredScreenTemplate {}
        LaunchedEffect(Unit) { onExit() }
        return
    }
    SearchResultsContent(
        state = state,
        onPlace = {
            viewModel.openDetail(it)
            onOpenDetail()
        },
        onRetry = viewModel::retry,
    )
}

/** Un mensaje (cargando, vacío, error) va centrado y sin scroll; los resultados, en lista bajo el título. */
@Composable
internal fun SearchResultsContent(state: SearchUiState, onPlace: (Int) -> Unit, onRetry: () -> Unit) {
    val status = state.status
    if (status != null) {
        CenteredScreenTemplate {
            SearchStatusMessage(status)
            if (status.canRetry) {
                HaptiButton(
                    text = stringResource(R.string.search_retry),
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                    variant = HaptiButtonVariant.Secondary,
                )
            }
        }
        return
    }
    RoundScreenTemplate {
        state.query?.let { query -> item { HeadingText(query.title()) } }
        item { PlaceResultsList(items = state.places.map { it.toRowItem() }, onItemClick = onPlace) }
    }
}
