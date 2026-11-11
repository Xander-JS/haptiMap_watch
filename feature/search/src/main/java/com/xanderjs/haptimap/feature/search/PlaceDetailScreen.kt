package com.xanderjs.haptimap.feature.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xanderjs.haptimap.designsystem.atoms.HaptiEdgeButton
import com.xanderjs.haptimap.designsystem.organisms.ConfirmDialog
import com.xanderjs.haptimap.designsystem.templates.CenteredScreenTemplate
import com.xanderjs.haptimap.designsystem.templates.PlaceDetailPager

/**
 * Detalle de los resultados, un lugar por página, abriendo en el que se tocó en la lista. "Ir" lo elige como destino:
 * [onDestinationSelected] llega con la sesión en DestinationSelected (con una ruta activa, antes se confirma).
 * [onExit], si no hay resultados (el proceso se recreó). Comparte el ViewModel con la búsqueda y la lista.
 */
@Composable
fun PlaceDetailScreen(
    onDestinationSelected: () -> Unit,
    onExit: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (state.places.isEmpty()) {
        CenteredScreenTemplate {}
        LaunchedEffect(Unit) { onExit() }
        return
    }
    PlaceDetailContent(state, onGo = { if (viewModel.select(state.places[it])) onDestinationSelected() })
    ConfirmDialog(
        visible = state.pendingReplace != null,
        title = stringResource(R.string.search_replace_title),
        confirmLabel = stringResource(R.string.search_replace_confirm),
        dismissLabel = stringResource(R.string.search_replace_keep),
        onConfirm = { if (viewModel.confirmReplace()) onDestinationSelected() },
        onDismiss = viewModel::keepRoute,
    )
}

@Composable
internal fun PlaceDetailContent(state: SearchUiState, onGo: (Int) -> Unit) = PlaceDetailPager(
    items = state.places.map { it.toDetailItem() },
    initialPage = state.detailPage,
    goAction = { page -> HaptiEdgeButton(text = stringResource(R.string.place_go), onClick = { onGo(page) }) },
)
