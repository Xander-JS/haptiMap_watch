package com.xanderjs.haptimap.designsystem.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Text
import com.xanderjs.haptimap.designsystem.R
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

enum class HaptiButtonVariant { Primary, Secondary }

/** Botón de la app. El texto llega ya resuelto con stringResource() desde quien lo usa. */
@Composable
fun HaptiButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: HaptiButtonVariant = HaptiButtonVariant.Primary,
    enabled: Boolean = true,
) {
    val colors = HaptiMapTheme.colors
    val (container, content) = when (variant) {
        HaptiButtonVariant.Primary -> colors.primary to colors.onPrimary
        HaptiButtonVariant.Secondary -> colors.surface to colors.onSurface
    }
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = HaptiMapTheme.dimens.minTouchTarget),
        enabled = enabled,
        shape = HaptiMapTheme.shapes.full,
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
    ) {
        Text(text = text, style = HaptiMapTheme.typography.label)
    }
}

@HaptiPreviews
@Composable
private fun HaptiButtonPreview() = PreviewSurface {
    Column(verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.s)) {
        HaptiButton(text = stringResource(R.string.sample_button_primary), onClick = {})
        HaptiButton(text = stringResource(R.string.sample_button_secondary), onClick = {}, variant = HaptiButtonVariant.Secondary)
    }
}
