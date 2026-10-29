package com.xanderjs.haptimap.designsystem.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.xanderjs.haptimap.designsystem.R
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * Vuelta a la ruta en curso desde otra pantalla: un HaptiButton secundario de una sola línea, con la flecha
 * si se está navegando o el marcador de destino si la ruta está cargada sin empezar. Un destino largo se
 * corta con puntos suspensivos para no empujar lo que viene debajo; TalkBack lee [text] entero.
 */
@Composable
fun ActiveRouteButton(text: String, navigating: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    HaptiButton(
        text = text,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        variant = HaptiButtonVariant.Secondary,
        icon = painterResource(if (navigating) R.drawable.ic_straight else R.drawable.ic_arrive),
        maxLines = 1,
    )
}

@HaptiPreviews
@Composable
private fun ActiveRouteButtonPreview() = PreviewSurface {
    Column(verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.s)) {
        ActiveRouteButton(text = stringResource(R.string.sample_place_museum), navigating = true, onClick = {})
        ActiveRouteButton(text = stringResource(R.string.sample_instruction_long), navigating = false, onClick = {})
    }
}
