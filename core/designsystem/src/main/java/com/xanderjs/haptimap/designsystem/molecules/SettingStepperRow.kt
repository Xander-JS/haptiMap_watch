package com.xanderjs.haptimap.designsystem.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Text
import com.xanderjs.haptimap.designsystem.atoms.outlineBorder
import com.xanderjs.haptimap.designsystem.R
import com.xanderjs.haptimap.designsystem.atoms.SectionLabel
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * Ajuste numérico con menos/valor/más (paso 25, p. ej. la espera de sugerencias en segundos): un rótulo de grupo
 * ([SectionLabel]) y una fila con dos botones redondos y el valor ya formateado en medio. Los botones se apagan en
 * los extremos del rango ([decrementEnabled]/[incrementEnabled]); llevan descripción para TalkBack ([decrementDescription]/
 * [incrementDescription]) porque el símbolo solo no se lee bien.
 */
@Composable
fun SettingStepperRow(
    title: String,
    value: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    decrementDescription: String,
    incrementDescription: String,
    modifier: Modifier = Modifier,
    decrementEnabled: Boolean = true,
    incrementEnabled: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.s)) {
        SectionLabel(title)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.m, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepButton(symbol = "−", description = decrementDescription, onClick = onDecrement, enabled = decrementEnabled)
            Text(text = value, style = HaptiMapTheme.typography.title, color = HaptiMapTheme.colors.onBackground)
            StepButton(symbol = "+", description = incrementDescription, onClick = onIncrement, enabled = incrementEnabled)
        }
    }
}

@Composable
private fun StepButton(symbol: String, description: String, onClick: () -> Unit, enabled: Boolean) {
    val colors = HaptiMapTheme.colors
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(HaptiMapTheme.dimens.minTouchTarget).clearAndSetSemantics { contentDescription = description },
        shape = HaptiMapTheme.shapes.full,
        colors = ButtonDefaults.buttonColors(containerColor = colors.surface, contentColor = colors.onSurface),
        border = outlineBorder(),
    ) {
        Text(text = symbol, style = HaptiMapTheme.typography.title)
    }
}

@HaptiPreviews
@Composable
private fun SettingStepperRowPreview() = PreviewSurface {
    SettingStepperRow(
        title = stringResource(R.string.sample_section_label),
        value = "7 s",
        onDecrement = {},
        onIncrement = {},
        decrementDescription = stringResource(R.string.sample_step_less),
        incrementDescription = stringResource(R.string.sample_step_more),
    )
}
