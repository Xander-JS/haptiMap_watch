package com.xanderjs.haptimap.designsystem.atoms

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.IconButtonDefaults
import com.xanderjs.haptimap.designsystem.R
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * Entrada por voz, la acción principal de la búsqueda: un botón redondo grande con el micrófono y sin texto (el icono se
 * entiende solo y deja sitio a las categorías). [label] ("Buscar por voz") es lo que lee TalkBack. Quien lo usa lanza
 * el reconocedor del sistema.
 */
@Composable
fun VoiceSearchButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = HaptiMapTheme.colors
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.size(HaptiMapTheme.dimens.iconButtonLarge),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
        ) {
            HaptiIcon(painterResource(R.drawable.ic_mic), contentDescription = label, size = HaptiIconSize.Disc, tint = colors.onPrimary)
        }
    }
}

@HaptiPreviews
@Composable
private fun VoiceSearchButtonPreview() = PreviewSurface {
    VoiceSearchButton(label = stringResource(R.string.sample_voice_search), onClick = {})
}
