package com.xanderjs.haptimap.feature.navigationui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.AppScaffold
import com.android.tools.screenshot.PreviewTest
import com.xanderjs.haptimap.designsystem.atoms.HaptiTimeText
import com.xanderjs.haptimap.designsystem.preview.HaptiLargeFontPreviews
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

// Paso 22: las superposiciones del mapa continuo. El mapa en sí se sustituye por un marcador neutro: las capturas
// se renderizan con layoutlib y NO cargan teselas reales (sin red ni coste). Se cubren las 12 combinaciones de
// @HaptiPreviews (redondas peq./grande y cuadrada, claro/oscuro, EN/ES) y la letra más grande.
// Referencias: ./gradlew :feature:navigation-ui:updateDebugScreenshotTest; comprobar: validateDebugScreenshotTest.

/** Marcador neutro en lugar de las teselas: así la captura mide las superposiciones, no el mapa. */
@Composable
private fun Map(
    guidance: GuidanceUi = turn(),
    notice: NavigationNotice? = null,
    recenterVisible: Boolean = false,
) = HaptiMapTheme {
    AppScaffold(timeText = { HaptiTimeText() }) {
        NavigationMapContent(
            guidance = guidance,
            notice = notice,
            recenterVisible = recenterVisible,
            onRecenter = {},
            onStop = {},
            map = { Box(Modifier.matchParentSize().background(Color(0xFF3C4043))) },
        )
    }
}

@PreviewTest @HaptiPreviews @Composable
fun MapTurn() = Map()

/** Seguimiento pausado: el botón de recentrar visible a un lado. */
@PreviewTest @HaptiPreviews @Composable
fun MapRecenter() = Map(recenterVisible = true)

/** Con el aviso de señal débil sobre la maniobra. */
@PreviewTest @HaptiPreviews @Composable
fun MapWeakGps() = Map(notice = NavigationNotice.WEAK_GPS)

@PreviewTest @HaptiLargeFontPreviews @Composable
fun LargeFontMapTurn() = MapTurn()

@PreviewTest @HaptiLargeFontPreviews @Composable
fun LargeFontMapRecenter() = MapRecenter()
