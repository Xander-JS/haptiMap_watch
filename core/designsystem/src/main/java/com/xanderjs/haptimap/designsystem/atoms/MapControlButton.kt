package com.xanderjs.haptimap.designsystem.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xanderjs.haptimap.designsystem.R
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * Botón redondo solo-icono para controles sobre un mapa (paso 22), p. ej. recentrar. Píldora surface como
 * [NoticeLabel], con el área táctil mínima recomendada en Wear (48 dp) para pulsarlo sin precisión.
 * No hay un botón icono compacto en el sistema ([HaptiButton] es de ancho completo con texto), de ahí este átomo.
 */
@Composable
fun MapControlButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .background(HaptiMapTheme.colors.surface, HaptiMapTheme.shapes.full)
            .clickable(onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        HaptiIcon(painter = painterResource(iconRes), contentDescription = contentDescription, tint = HaptiMapTheme.colors.onSurface)
    }
}

@HaptiPreviews
@Composable
private fun MapControlButtonPreview() = PreviewSurface {
    MapControlButton(iconRes = R.drawable.ic_recenter, contentDescription = stringResource(R.string.map_recenter), onClick = {})
}
