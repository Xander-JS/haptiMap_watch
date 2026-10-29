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
import com.xanderjs.haptimap.designsystem.R
import com.xanderjs.haptimap.designsystem.atoms.formatDistance
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * Lo que muestra una fila: [categoryLabel] ya traducido, o null si el lugar no tiene categoría; [address]
 * corta ("Calle Mayor, 5"), o null si Places no la da.
 */
data class PlaceRowItem(val name: String, val distanceMeters: Int, val categoryLabel: String?, val address: String? = null)

/**
 * Un lugar elegible: nombre arriba, "categoría · distancia" debajo y la dirección corta al final. Todo
 * el botón es el área táctil, y TalkBack lo lee como una sola frase. Sin categoría (búsqueda por texto)
 * solo la distancia.
 */
@Composable
fun PlaceRow(item: PlaceRowItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = HaptiMapTheme.colors
    val distance = formatDistance(item.distanceMeters)
    val detail = item.categoryLabel?.let { stringResource(R.string.place_detail, it, distance) } ?: distance
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
            Text(item.name, style = HaptiMapTheme.typography.label, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        secondaryLabel = {
            Column {
                Text(detail, style = HaptiMapTheme.typography.caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
                item.address?.let { Text(it, style = HaptiMapTheme.typography.caption, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        },
    )
}

/** Filas de ejemplo para previews y catálogo; los textos salen de recursos. */
@Composable
fun samplePlaceRows(): List<PlaceRowItem> = listOf(
    PlaceRowItem(stringResource(R.string.sample_place_pharmacy), 120, stringResource(R.string.sample_category_pharmacy), stringResource(R.string.sample_address_pharmacy)),
    PlaceRowItem(stringResource(R.string.sample_place_cafe), 850, stringResource(R.string.sample_category_cafe), stringResource(R.string.sample_address_cafe)),
    PlaceRowItem(stringResource(R.string.sample_place_museum), 1240, null),
)

@HaptiPreviews
@Composable
private fun PlaceRowPreview() = PreviewSurface {
    Column(verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.s)) {
        samplePlaceRows().take(2).forEach { PlaceRow(item = it, onClick = {}) }
    }
}
