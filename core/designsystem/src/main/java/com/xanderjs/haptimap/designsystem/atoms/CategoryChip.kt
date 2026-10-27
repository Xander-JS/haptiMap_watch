package com.xanderjs.haptimap.designsystem.atoms

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Text
import com.xanderjs.haptimap.core.model.PlaceCategory
import com.xanderjs.haptimap.designsystem.R
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/** Chip de categoría de búsqueda. Seleccionado = color de acento (el mismo que resalta POIs). */
@Composable
fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HaptiMapTheme.colors
    Button(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = HaptiMapTheme.dimens.minTouchTarget)
            .semantics { this.selected = selected },
        shape = HaptiMapTheme.shapes.full,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) colors.accent else colors.surface,
            contentColor = if (selected) colors.onAccent else colors.onSurface,
        ),
    ) {
        Text(text = label, style = HaptiMapTheme.typography.label)
    }
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

@HaptiPreviews
@Composable
private fun CategoryChipPreview() = PreviewSurface {
    Column(verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.s)) {
        CategoryChip(label = stringResource(PlaceCategory.PHARMACY.label), selected = true, onClick = {})
        CategoryChip(label = stringResource(PlaceCategory.CAFE.label), selected = false, onClick = {})
    }
}
