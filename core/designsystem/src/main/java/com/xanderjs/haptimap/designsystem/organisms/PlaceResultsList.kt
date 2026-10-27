package com.xanderjs.haptimap.designsystem.organisms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.designsystem.molecules.PlaceRow
import com.xanderjs.haptimap.designsystem.molecules.samplePlaces
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * Resultados de una búsqueda, en el orden recibido. Es una Column, no una lista perezosa: va como
 * un item dentro del RoundScreenTemplate, que ya hace el scroll, y nunca pasa de MAX_RESULTS (10).
 */
@Composable
fun PlaceResultsList(places: List<Place>, onPlaceClick: (Place) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.s)) {
        places.forEach { place -> PlaceRow(place = place, onClick = { onPlaceClick(place) }) }
    }
}

@HaptiPreviews
@Composable
private fun PlaceResultsListPreview() = PreviewSurface {
    PlaceResultsList(places = samplePlaces(), onPlaceClick = {})
}
