package com.xanderjs.haptimap.feature.navigationui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.wear.compose.material3.AppScaffold
import com.android.tools.screenshot.PreviewTest
import com.xanderjs.haptimap.designsystem.atoms.ArrowDirection
import com.xanderjs.haptimap.designsystem.atoms.HaptiTimeText
import com.xanderjs.haptimap.designsystem.atoms.LocalOffline
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

// Paso 12: la vista de ambiente / AOD en cada estado de la navegación, en las 12 combinaciones de @HaptiPreviews.
// Con el tema de ambiente en la raíz, como en MainActivity: claro y oscuro salen iguales (AOD es oscuro siempre).
// La vista Eco interactiva es NavigationScreen: sus estados están en NavigationScreenshots.

@Composable
private fun Ambient(state: AmbientNavigationUi, offline: Boolean = false) = HaptiMapTheme(ambient = true) {
    CompositionLocalProvider(LocalOffline provides offline) {
        AppScaffold(timeText = { HaptiTimeText() }) { AmbientNavigationContent(state) }
    }
}

@PreviewTest @HaptiPreviews @Composable
fun AmbientNavigating() = Ambient(AmbientNavigationUi.Guidance(ArrowDirection.LEFT, 50, null))

/** Sin red: "Sin red" en la hora y el aviso "Funciona sin red" sin píldora. */
@PreviewTest @HaptiPreviews @Composable
fun AmbientNavigatingOffline() = Ambient(AmbientNavigationUi.Guidance(ArrowDirection.RIGHT, 150, NavigationNotice.OFFLINE), offline = true)

/** La distancia más ancha: 12,5 km. */
@PreviewTest @HaptiPreviews @Composable
fun AmbientNavigatingLongDistance() = Ambient(AmbientNavigationUi.Guidance(ArrowDirection.STRAIGHT, 12_500, NavigationNotice.WEAK_GPS))

@PreviewTest @HaptiPreviews @Composable
fun AmbientToArrival() = Ambient(AmbientNavigationUi.Guidance(ArrowDirection.ARRIVE, 30, null))

@PreviewTest @HaptiPreviews @Composable
fun AmbientOffRoute() = Ambient(AmbientNavigationUi.OffRoute(OffRouteStatus.HEAD_BACK, null))

/** Buscando otra ruta: sin icono ni spinner, solo el texto. */
@PreviewTest @HaptiPreviews @Composable
fun AmbientRecalculating() = Ambient(AmbientNavigationUi.OffRoute(OffRouteStatus.RECALCULATING, null))

@PreviewTest @HaptiPreviews @Composable
fun AmbientOffRouteOffline() = Ambient(AmbientNavigationUi.OffRoute(OffRouteStatus.OFFLINE, null), offline = true)

/** El caso más alto: señal débil, icono, título y el error en dos líneas. */
@PreviewTest @HaptiPreviews @Composable
fun AmbientOffRouteWeakGpsFailed() = Ambient(AmbientNavigationUi.OffRoute(OffRouteStatus.FAILED, NavigationNotice.WEAK_GPS))

@PreviewTest @HaptiPreviews @Composable
fun AmbientArrived() = Ambient(AmbientNavigationUi.Arrived)
