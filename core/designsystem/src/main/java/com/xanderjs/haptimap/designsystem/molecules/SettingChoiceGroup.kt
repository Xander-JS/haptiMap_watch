package com.xanderjs.haptimap.designsystem.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.Text
import com.xanderjs.haptimap.designsystem.R
import com.xanderjs.haptimap.designsystem.atoms.ChoiceChip
import com.xanderjs.haptimap.designsystem.atoms.SectionLabel
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/** Una opción de [SettingChoiceGroup]: texto ya resuelto, si está marcada y si se puede elegir. */
@Immutable
data class ChoiceOption(
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
)

/**
 * Un ajuste con sus opciones en una lista de píldoras (paso 25): un rótulo de grupo ([SectionLabel]) y una
 * [ChoiceChip] por opción. Genérico: sirve para selección única (una marcada) o múltiple (varias), lo decide
 * quien lo usa con [ChoiceOption.selected]. [note] añade una aclaración bajo las opciones (p. ej. "próximamente").
 */
@Composable
fun SettingChoiceGroup(
    title: String,
    options: List<ChoiceOption>,
    modifier: Modifier = Modifier,
    note: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.s)) {
        SectionLabel(title)
        options.forEach { option ->
            ChoiceChip(label = option.label, selected = option.selected, onClick = option.onClick, enabled = option.enabled)
        }
        note?.let {
            Text(
                text = it,
                modifier = Modifier.fillMaxWidth().padding(horizontal = HaptiMapTheme.spacing.m),
                style = HaptiMapTheme.typography.caption,
                color = HaptiMapTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@HaptiPreviews
@Composable
private fun SettingChoiceGroupPreview() = PreviewSurface {
    SettingChoiceGroup(
        title = stringResource(R.string.sample_section_label),
        options = listOf(
            ChoiceOption(stringResource(R.string.sample_choice_unselected), selected = false, onClick = {}),
            ChoiceOption(stringResource(R.string.sample_choice_selected), selected = true, onClick = {}),
            ChoiceOption(stringResource(R.string.sample_choice_disabled), selected = false, onClick = {}, enabled = false),
        ),
        note = stringResource(R.string.sample_choice_note),
    )
}
