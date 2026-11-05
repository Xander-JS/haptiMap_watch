package com.xanderjs.haptimap.designsystem.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ScreenScaffold
import com.xanderjs.haptimap.designsystem.atoms.ArrowDirection
import com.xanderjs.haptimap.designsystem.atoms.HaptiTimeText
import com.xanderjs.haptimap.designsystem.atoms.LocalOffline
import com.xanderjs.haptimap.designsystem.molecules.ManeuverIndicator
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * Pantalla de ambiente / AOD (paso 12): [content] centrado en la zona segura, en el tema de ambiente (fondo negro,
 * un gris, trazos finos, sin rellenos ni animaciones), con los mismos componentes que la vista interactiva. Sin scroll
 * ni corona: en ambiente no se interactúa. Opaca, para ir encima de la pantalla interactiva.
 * La app pone además HaptiMapTheme(ambient = true) en la raíz: la hora de AppScaffold se compone fuera de esta plantilla.
 *
 * Sin desplazar píxeles a mano: con isBurnInProtectionRequired el sistema mueve la pantalla entera, y el padding
 * de ScreenScaffold ya deja los 10 px de margen que pide la guía de Wear.
 */
@Composable
fun AmbientScreenTemplate(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    // se lee aquí: Wear compone timeText dentro de AppScaffold, fuera de los providers de la pantalla (y de su tema)
    val offline = LocalOffline.current
    val round = LocalConfiguration.current.isScreenRound
    val dimens = HaptiMapTheme.dimens
    ScreenScaffold(
        modifier = modifier,
        timeText = { HaptiMapTheme(ambient = true) { HaptiTimeText(offline) } },
    ) { contentPadding ->
        HaptiMapTheme(ambient = true) {
            BoxWithConstraints(Modifier.fillMaxSize().background(HaptiMapTheme.colors.background)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = contentPadding.calculateTopPadding() + dimens.clockBandSafeTop,
                            bottom = safeBottom(maxHeight, contentPadding, round, dimens.centeredContentWidthFraction),
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.s, Alignment.CenterVertically),
                    content = content,
                )
            }
        }
    }
}

@HaptiPreviews
@Composable
private fun AmbientScreenTemplatePreview() = HaptiMapTheme(ambient = true) {
    AppScaffold(timeText = { HaptiTimeText() }) {
        AmbientScreenTemplate { ManeuverIndicator(direction = ArrowDirection.LEFT, distanceMeters = 50) }
    }
}
