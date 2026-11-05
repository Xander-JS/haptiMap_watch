package com.xanderjs.haptimap.feature.navigationui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.AppScaffold
import com.android.tools.screenshot.PreviewTest
import com.xanderjs.haptimap.designsystem.R as DsR
import com.xanderjs.haptimap.designsystem.atoms.ArrowDirection
import com.xanderjs.haptimap.designsystem.atoms.HaptiTimeText
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

// Paso 07: la pantalla de navegación en las 12 combinaciones de @HaptiPreviews.
// Referencias: ./gradlew :feature:navigation-ui:updateDebugScreenshotTest; comprobar: validateDebugScreenshotTest.

@Composable
private fun Screen(state: NavigationUiState) = HaptiMapTheme {
    AppScaffold(timeText = { HaptiTimeText() }) { NavigationContent(state, {}, {}, {}, {}, {}) }
}

/** Girar a la izquierda en 45 m; quedan 850 m y 11 min. */
@Composable
internal fun turn(instruction: String? = stringResource(DsR.string.sample_instruction)) =
    GuidanceUi(ArrowDirection.LEFT, 45, instruction, 850, 11.minutes)

@Composable
private fun Navigating(guidance: GuidanceUi = turn(), notice: NavigationNotice? = null) =
    Screen(NavigationUiState.Navigating(guidance, notice))

@PreviewTest @HaptiPreviews @Composable
fun NavigationTurn() = Navigating()

/** La instrucción más larga habitual: tres líneas y puntos suspensivos, sin tapar las estadísticas. */
@PreviewTest @HaptiPreviews @Composable
fun NavigationLongInstruction() = Navigating(turn(stringResource(DsR.string.sample_instruction_long)))

/** Sin instrucción del proveedor (p. ej. seguir recto): flecha, distancia y estadísticas. */
@PreviewTest @HaptiPreviews @Composable
fun NavigationNoInstruction() = Navigating(GuidanceUi(ArrowDirection.STRAIGHT, 120, null, 1240, 15.minutes))

/** Encima de la maniobra: 0 m y lo que queda casi nada. */
@PreviewTest @HaptiPreviews @Composable
fun NavigationVeryShortDistance() = Navigating(GuidanceUi(ArrowDirection.RIGHT, 0, stringResource(DsR.string.sample_instruction), 3, 5.seconds))

/** Un tramo de 12,4 km y una ruta de más de dos horas. */
@PreviewTest @HaptiPreviews @Composable
fun NavigationVeryLongDistance() = Navigating(GuidanceUi(ArrowDirection.STRAIGHT, 12_400, stringResource(DsR.string.sample_instruction), 14_850, 2.hours + 35.minutes))

/** Última maniobra: la llegada, con el destino como instrucción. */
@PreviewTest @HaptiPreviews @Composable
fun NavigationToArrival() = Navigating(GuidanceUi(ArrowDirection.ARRIVE, 30, stringResource(DsR.string.sample_place_museum), 30, 1.minutes))

@PreviewTest @HaptiPreviews @Composable
fun NavigationOffline() = Navigating(notice = NavigationNotice.OFFLINE)

/** También sin red: solo se ve el de señal débil (navigationNotice). */
@PreviewTest @HaptiPreviews @Composable
fun NavigationWeakGps() = Navigating(notice = NavigationNotice.WEAK_GPS)

@PreviewTest @HaptiPreviews @Composable
fun NavigationWeakGpsLongInstruction() = Navigating(turn(stringResource(DsR.string.sample_instruction_long)), NavigationNotice.WEAK_GPS)

// Paso 08: fuera de ruta, una captura por línea de estado (offRouteStatus) y la más alta con aviso.

internal fun offRoute(status: OffRouteStatus, notice: NavigationNotice? = null) =
    NavigationUiState.OffRoute(status, metersFromRoute = 32, notice = notice)

@Composable
private fun OffRoute(status: OffRouteStatus, notice: NavigationNotice? = null) = Screen(offRoute(status, notice))

@PreviewTest @HaptiPreviews @Composable
fun NavigationOffRoute() = OffRoute(OffRouteStatus.HEAD_BACK)

@PreviewTest @HaptiPreviews @Composable
fun NavigationOffRouteRecalculating() = OffRoute(OffRouteStatus.RECALCULATING)

@PreviewTest @HaptiPreviews @Composable
fun NavigationOffRouteOffline() = OffRoute(OffRouteStatus.OFFLINE)

/** Con Reintentar, que va antes de la distancia a la ruta. */
@PreviewTest @HaptiPreviews @Composable
fun NavigationOffRouteFailed() = OffRoute(OffRouteStatus.FAILED)

/** La primera vista más alta: aviso, icono, título y un error de dos líneas. */
@PreviewTest @HaptiPreviews @Composable
fun NavigationOffRouteWeakGpsFailed() = OffRoute(OffRouteStatus.FAILED, NavigationNotice.WEAK_GPS)

@PreviewTest @HaptiPreviews @Composable
fun NavigationStopConfirmation() = HaptiMapTheme {
    AppScaffold(timeText = { HaptiTimeText() }) { StopConfirmationContent() }
}

@PreviewTest @HaptiPreviews @Composable
fun NavigationArrived() = Screen(NavigationUiState.Arrived(stringResource(DsR.string.sample_place_museum)))

@PreviewTest @HaptiPreviews @Composable
fun NavigationArrivedLongName() = Screen(NavigationUiState.Arrived("Museo Nacional Centro de Arte Reina Sofía"))
