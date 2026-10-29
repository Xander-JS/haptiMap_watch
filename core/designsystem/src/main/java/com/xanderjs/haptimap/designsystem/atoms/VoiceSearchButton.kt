package com.xanderjs.haptimap.designsystem.atoms

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.xanderjs.haptimap.designsystem.R
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface

/**
 * Entrada por voz: un HaptiButton primario con el micrófono, a todo el ancho. Quien lo usa lanza el
 * reconocedor del sistema; [label] dice qué se puede decir ("Buscar por voz").
 */
@Composable
fun VoiceSearchButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    HaptiButton(
        text = label,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        icon = painterResource(R.drawable.ic_mic),
    )
}

@HaptiPreviews
@Composable
private fun VoiceSearchButtonPreview() = PreviewSurface {
    VoiceSearchButton(label = stringResource(R.string.sample_voice_search), onClick = {})
}
