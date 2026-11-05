package com.xanderjs.haptimap.feature.navigationui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.AppScaffold
import com.android.tools.screenshot.PreviewTest
import com.xanderjs.haptimap.designsystem.R as DsR
import com.xanderjs.haptimap.designsystem.atoms.ArrowDirection
import com.xanderjs.haptimap.designsystem.atoms.HaptiTimeText
import com.xanderjs.haptimap.designsystem.atoms.LocalOffline
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

// Paso 12: la vista de ambiente / AOD en cada estado de la navegación, en las 12 combinaciones de @HaptiPreviews.
// Con el tema de ambiente en la raíz, como en MainActivity: claro y oscuro salen iguales (AOD es oscuro siempre).
// La vista Eco interactiva es NavigationScreen: cada captura de ambiente usa el estado de su gemela Eco en
// NavigationScreenshots (al lado), y AmbientPositionTest comprueba en las referencias que la vista no se mueve.

@Composable
private fun Ambient(state: NavigationUiState, offline: Boolean = false) = HaptiMapTheme(ambient = true) {
    CompositionLocalProvider(LocalOffline provides offline) {
        AppScaffold(timeText = { HaptiTimeText() }) { AmbientNavigationContent(checkNotNull(ambientNavigationUi(state))) }
    }
}

/** NavigationTurn */
@PreviewTest @HaptiPreviews @Composable
fun AmbientNavigating() = Ambient(NavigationUiState.Navigating(turn(), null))

/** NavigationOffline. Sin red: "Sin red" en la hora y el aviso "Funciona sin red" sin píldora. */
@PreviewTest @HaptiPreviews @Composable
fun AmbientNavigatingOffline() = Ambient(NavigationUiState.Navigating(turn(), NavigationNotice.OFFLINE), offline = true)

/** NavigationVeryLongDistance. La distancia más ancha: 12,5 km. */
@PreviewTest @HaptiPreviews @Composable
fun AmbientNavigatingLongDistance() = Ambient(
    NavigationUiState.Navigating(GuidanceUi(ArrowDirection.STRAIGHT, 12_400, stringResource(DsR.string.sample_instruction), 14_850, 2.hours + 35.minutes), null),
)

/** NavigationToArrival */
@PreviewTest @HaptiPreviews @Composable
fun AmbientToArrival() = Ambient(
    NavigationUiState.Navigating(GuidanceUi(ArrowDirection.ARRIVE, 30, stringResource(DsR.string.sample_place_museum), 30, 1.minutes), null),
)

/** NavigationOffRoute */
@PreviewTest @HaptiPreviews @Composable
fun AmbientOffRoute() = Ambient(offRoute(OffRouteStatus.HEAD_BACK))

/** NavigationOffRouteRecalculating. Buscando otra ruta: sin icono ni spinner, solo el texto. */
@PreviewTest @HaptiPreviews @Composable
fun AmbientRecalculating() = Ambient(offRoute(OffRouteStatus.RECALCULATING))

/** NavigationOffRouteOffline */
@PreviewTest @HaptiPreviews @Composable
fun AmbientOffRouteOffline() = Ambient(offRoute(OffRouteStatus.OFFLINE), offline = true)

/** NavigationOffRouteWeakGpsFailed. El caso más alto: señal débil, icono, título y el error en dos líneas. */
@PreviewTest @HaptiPreviews @Composable
fun AmbientOffRouteWeakGpsFailed() = Ambient(offRoute(OffRouteStatus.FAILED, NavigationNotice.WEAK_GPS))

/** NavigationArrived */
@PreviewTest @HaptiPreviews @Composable
fun AmbientArrived() = Ambient(NavigationUiState.Arrived(stringResource(DsR.string.sample_place_museum)))
