package com.xanderjs.haptimap.designsystem.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.wear.compose.foundation.requestFocusOnHierarchyActive
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
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
import com.xanderjs.haptimap.designsystem.atoms.clockBandTop
import com.xanderjs.haptimap.designsystem.atoms.StatusTone
import com.xanderjs.haptimap.designsystem.organisms.StatusMessage
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * Plantilla de pantalla de un solo mensaje (cargando, error + reintentar): centrada en
 * vertical, donde la pantalla redonda es más ancha, y con un ancho de dimens.centeredContentWidthFraction
 * para que el círculo no corte el texto. Solo para contenido corto; lo demás va en [RoundScreenTemplate].
 * Si no cabe (letra del sistema grande, paso 15A: "Reintentar" quedaba cortado y sin forma de llegar) se desplaza
 * con el dedo y la corona, con su indicador; si cabe, queda centrado como siempre.
 */
@Composable
fun CenteredScreenTemplate(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    // se lee aquí por lo mismo que en RoundScreenTemplate: timeText se compone fuera de los providers de la pantalla
    val offline = LocalOffline.current
    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    ScreenScaffold(scrollState = scrollState, modifier = modifier, timeText = { HaptiTimeText(offline) }) { contentPadding ->
        val top = clockBandTop(offline)
        BoxWithConstraints(Modifier.fillMaxSize().padding(contentPadding).padding(top = top)) {
            val screen = maxHeight
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .requestFocusOnHierarchyActive()
                    .rotaryScrollable(RotaryScrollableDefaults.behavior(scrollState), focusRequester)
                    .verticalScroll(scrollState)
                    // al menos el alto visible: con contenido corto, Arrangement.Center lo centra como antes
                    .heightIn(min = screen),
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
}

@HaptiPreviews
@Composable
private fun CenteredScreenTemplatePreview() = HaptiMapTheme {
    AppScaffold(timeText = { HaptiTimeText() }) {
        CenteredScreenTemplate { StatusMessage(stringResource(R.string.sample_status_error), tone = StatusTone.Error) }
    }
}
