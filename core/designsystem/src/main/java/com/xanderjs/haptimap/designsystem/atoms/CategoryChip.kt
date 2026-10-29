package com.xanderjs.haptimap.designsystem.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Text
import com.xanderjs.haptimap.designsystem.R
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * Categoría de búsqueda: icono + nombre en una píldora a todo el ancho, como el resto de filas de una lista
 * redonda. Seleccionada = color de acento (el mismo que resalta POIs). El icono es decorativo: TalkBack lee el nombre.
 */
@Composable
fun CategoryChip(
    label: String,
    icon: Painter,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HaptiMapTheme.colors
    val content = if (selected) colors.onAccent else colors.onSurface
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = HaptiMapTheme.dimens.minTouchTarget)
            .semantics { this.selected = selected },
        shape = HaptiMapTheme.shapes.full,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) colors.accent else colors.surface,
            contentColor = content,
        ),
        icon = { HaptiIcon(painter = icon, contentDescription = null, tint = content) },
        label = { Text(text = label, style = HaptiMapTheme.typography.label, maxLines = 2, overflow = TextOverflow.Ellipsis) },
    )
}

@HaptiPreviews
@Composable
private fun CategoryChipPreview() = PreviewSurface {
    Column(verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.s)) {
        CategoryChip(stringResource(R.string.sample_category_pharmacy), painterResource(R.drawable.ic_category_pharmacy), selected = true, onClick = {})
        CategoryChip(stringResource(R.string.sample_category_cafe), painterResource(R.drawable.ic_category_cafe), selected = false, onClick = {})
    }
}
