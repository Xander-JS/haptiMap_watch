package com.xanderjs.haptimap.designsystem.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
 * Opción elegible solo de texto, en una píldora a todo el ancho como el resto de filas de una lista redonda
 * (paso 25, ajustes). Sirve para selección única (una marcada por grupo) y múltiple (cada una por su cuenta):
 * [selected] la pinta con el color principal. [enabled] en false la deja apagada, para opciones aún no
 * disponibles (p. ej. el mapa continuo, paso 22). TalkBack lee el texto y si está seleccionada.
 */
@Composable
fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = HaptiMapTheme.colors
    val (container, content) = if (selected) colors.primary to colors.onPrimary else colors.surface to colors.onSurface
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = HaptiMapTheme.dimens.minTouchTarget)
            .semantics { this.selected = selected },
        shape = HaptiMapTheme.shapes.full,
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        border = if (selected) null else outlineBorder(),
        label = { Text(text = label, style = HaptiMapTheme.typography.label, maxLines = 2, overflow = TextOverflow.Ellipsis) },
    )
}

@HaptiPreviews
@Composable
private fun ChoiceChipPreview() = PreviewSurface {
    Column(verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.s)) {
        ChoiceChip(stringResource(R.string.sample_choice_selected), selected = true, onClick = {})
        ChoiceChip(stringResource(R.string.sample_choice_unselected), selected = false, onClick = {})
        ChoiceChip(stringResource(R.string.sample_choice_disabled), selected = false, onClick = {}, enabled = false)
    }
}
