package com.xanderjs.haptimap.designsystem.atoms

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

@HaptiPreviews
@Composable
private fun CategoryChipPreview() = PreviewSurface {
    Column(verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.s)) {
        CategoryChip(label = stringResource(R.string.sample_category_pharmacy), selected = true, onClick = {})
        CategoryChip(label = stringResource(R.string.sample_category_cafe), selected = false, onClick = {})
    }
}
