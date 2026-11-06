package com.xanderjs.haptimap.designsystem.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ScreenScaffold
import com.xanderjs.haptimap.designsystem.atoms.ArrowDirection
import com.xanderjs.haptimap.designsystem.atoms.HaptiTimeText
import com.xanderjs.haptimap.designsystem.atoms.LocalOffline
import com.xanderjs.haptimap.designsystem.atoms.clockBandTop
import com.xanderjs.haptimap.designsystem.molecules.ManeuverIndicator
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * Pantalla de ambiente / AOD (paso 12): [glance] en el tema de ambiente (fondo negro, un gris, trazos finos, sin
 * rellenos ni animaciones), con los mismos componentes que la vista interactiva. Sin scroll ni corona: en ambiente
 * no se interactúa. Opaca, para ir encima de la pantalla interactiva.
 * La app pone además HaptiMapTheme(ambient = true) en la raíz: la hora de AppScaffold se compone fuera de esta plantilla.
 *
 * [glance] queda en el mismo sitio que en [GlanceScreenTemplate]: misma zona segura y mismo cálculo; los
 * [reservedDetails] (los details de la vista interactiva) se miden y no se pintan, solo reservan su sitio.
 * Lo que cambie con cada fix debe ir con un valor fijo de la misma altura, o la pantalla se redibujaría.
 *
 * Sin desplazar píxeles a mano: con isBurnInProtectionRequired el sistema mueve la pantalla entera, y el padding
 * de ScreenScaffold ya deja los 10 px de margen que pide la guía de Wear.
 */
@Composable
fun AmbientScreenTemplate(
    glance: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    reservedDetails: List<@Composable () -> Unit> = emptyList(),
) {
    // se lee aquí: Wear compone timeText dentro de AppScaffold, fuera de los providers de la pantalla (y de su tema)
    val offline = LocalOffline.current
    val round = LocalConfiguration.current.isScreenRound
    val dimens = HaptiMapTheme.dimens
    val layoutDirection = LocalLayoutDirection.current
    ScreenScaffold(
        modifier = modifier,
        timeText = { HaptiMapTheme(ambient = true) { HaptiTimeText(offline) } },
    ) { contentPadding ->
        HaptiMapTheme(ambient = true) {
            BoxWithConstraints(Modifier.fillMaxSize().background(HaptiMapTheme.colors.background)) {
                val screenHeight = maxHeight
                val top = contentPadding.calculateTopPadding() + clockBandTop(offline)
                val safeBottom = safeBottom(screenHeight, contentPadding, round, dimens.centeredContentWidthFraction)
                // la misma zona que GlanceScreenTemplate
                Box(
                    Modifier.fillMaxSize().padding(
                        start = contentPadding.calculateStartPadding(layoutDirection),
                        top = top,
                        end = contentPadding.calculateEndPadding(layoutDirection),
                    ),
                ) {
                    FirstView(
                        safeHeight = screenHeight - top - safeBottom,
                        visibleHeight = screenHeight - top,
                        glance = glance,
                        details = reservedDetails,
                        placeDetails = false,
                    )
                }
            }
        }
    }
}

@HaptiPreviews
@Composable
private fun AmbientScreenTemplatePreview() = HaptiMapTheme(ambient = true) {
    AppScaffold(timeText = { HaptiTimeText() }) {
        AmbientScreenTemplate(glance = { ManeuverIndicator(direction = ArrowDirection.LEFT, distanceMeters = 50) })
    }
}
