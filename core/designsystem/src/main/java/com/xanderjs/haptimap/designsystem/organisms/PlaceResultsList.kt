package com.xanderjs.haptimap.designsystem.organisms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xanderjs.haptimap.designsystem.molecules.PlaceRow
import com.xanderjs.haptimap.designsystem.molecules.PlaceRowItem
import com.xanderjs.haptimap.designsystem.molecules.samplePlaceRows
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * Resultados de una búsqueda, en el orden recibido; [onItemClick] recibe el índice pulsado. Es una
 * Column, no una lista perezosa: va como un item dentro del RoundScreenTemplate, que ya hace el
 * scroll, y nunca pasa de MAX_RESULTS (10).
 */
@Composable
fun PlaceResultsList(items: List<PlaceRowItem>, onItemClick: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.s)) {
        items.forEachIndexed { index, item -> PlaceRow(item = item, onClick = { onItemClick(index) }) }
    }
}

@HaptiPreviews
@Composable
private fun PlaceResultsListPreview() = PreviewSurface {
    PlaceResultsList(items = samplePlaceRows(), onItemClick = {})
}
