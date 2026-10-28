package com.xanderjs.haptimap.designsystem.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ScreenScaffold
import com.xanderjs.haptimap.designsystem.R
import com.xanderjs.haptimap.designsystem.atoms.HaptiTimeText
import com.xanderjs.haptimap.designsystem.atoms.LocalOffline
import com.xanderjs.haptimap.designsystem.organisms.StatusTone
import com.xanderjs.haptimap.designsystem.organisms.StatusMessage
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * Plantilla de pantalla de un solo mensaje (cargando, error + reintentar): sin scroll, centrada en
 * vertical, donde la pantalla redonda es más ancha, y con un ancho de dimens.centeredContentWidthFraction
 * para que el círculo no corte el texto. Solo para contenido corto; lo demás va en [RoundScreenTemplate].
 */
@Composable
fun CenteredScreenTemplate(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    // se lee aquí por lo mismo que en RoundScreenTemplate: timeText se compone fuera de los providers de la pantalla
    val offline = LocalOffline.current
    ScreenScaffold(modifier = modifier, timeText = { HaptiTimeText(offline) }) { contentPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(contentPadding).padding(top = HaptiMapTheme.dimens.clockBandSafeTop),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(HaptiMapTheme.dimens.centeredContentWidthFraction),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.m),
                content = content,
            )
        }
    }
}

@HaptiPreviews
@Composable
private fun CenteredScreenTemplatePreview() = HaptiMapTheme {
    AppScaffold(timeText = { HaptiTimeText() }) {
        CenteredScreenTemplate { StatusMessage(stringResource(R.string.sample_status_error), tone = StatusTone.Error) }
    }
}
