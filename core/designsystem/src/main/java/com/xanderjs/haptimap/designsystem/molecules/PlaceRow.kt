package com.xanderjs.haptimap.designsystem.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Text
import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.PlaceCategory
import com.xanderjs.haptimap.designsystem.R
import com.xanderjs.haptimap.designsystem.atoms.formatDistance
import com.xanderjs.haptimap.designsystem.atoms.label
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * Un lugar elegible: nombre arriba y "categoría · distancia" debajo. Todo el botón es el área
 * táctil, y TalkBack lo lee como una sola frase. Sin categoría (búsqueda por texto) solo la distancia.
 */
@Composable
fun PlaceRow(place: Place, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = HaptiMapTheme.colors
    val distance = formatDistance(place.distanceMeters)
    val detail = place.category?.let { stringResource(R.string.place_detail, stringResource(it.label), distance) } ?: distance
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = HaptiMapTheme.dimens.minTouchTarget),
        shape = HaptiMapTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.surface,
            contentColor = colors.onSurface,
            secondaryContentColor = colors.onSurfaceVariant,
        ),
        label = {
            Text(place.name, style = HaptiMapTheme.typography.label, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        secondaryLabel = {
            Text(detail, style = HaptiMapTheme.typography.caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
    )
}

/** Lugares de ejemplo para previews y catálogo; los nombres salen de recursos. */
@Composable
fun samplePlaces(): List<Place> = listOf(
    Place("1", stringResource(R.string.sample_place_pharmacy), GeoPoint(0.0, 0.0), PlaceCategory.PHARMACY, null, 120),
    Place("2", stringResource(R.string.sample_place_cafe), GeoPoint(0.0, 0.0), PlaceCategory.CAFE, null, 850),
    Place("3", stringResource(R.string.sample_place_museum), GeoPoint(0.0, 0.0), null, null, 1240),
)

@HaptiPreviews
@Composable
private fun PlaceRowPreview() = PreviewSurface {
    Column(verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.s)) {
        samplePlaces().take(2).forEach { PlaceRow(place = it, onClick = {}) }
    }
}
