package com.xanderjs.haptimap.feature.navigationui

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.AppScaffold
import com.android.tools.screenshot.PreviewTest
import androidx.compose.ui.res.stringResource
import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.Maneuver
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.Route
import com.xanderjs.haptimap.core.model.RouteError
import com.xanderjs.haptimap.core.model.RouteStep
import com.xanderjs.haptimap.designsystem.R as DsR
import com.xanderjs.haptimap.designsystem.atoms.HaptiTimeText
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

// Cada estado de la pantalla en las 12 combinaciones de @HaptiPreviews (redonda S/L y cuadrada × claro/oscuro × en/es).
// Referencias: ./gradlew :feature:navigation-ui:updateDebugScreenshotTest; comprobar: validateDebugScreenshotTest.

/** Ruta de ejemplo: 1,2 km, 15 min; salida de 120 m, giro a la izquierda y llegada. Textos de ejemplo del sistema de diseño. */
@Composable
private fun sampleRoute(): Route {
    val point = GeoPoint(0.0, 0.0)
    fun step(maneuver: Maneuver, meters: Int, instruction: String?) =
        RouteStep(maneuver, meters, meters.seconds, listOf(point), point, point, instruction, null)
    return Route(
        destination = Place("museum", stringResource(DsR.string.sample_place_museum), point, null, null, 1240),
        distanceMeters = 1240,
        duration = 15.minutes,
        points = listOf(point),
        steps = listOf(
            step(Maneuver.CONTINUE, 120, null),
            step(Maneuver.TURN_LEFT, 1120, stringResource(DsR.string.sample_instruction)),
            step(Maneuver.ARRIVAL, 0, null),
        ),
    )
}

@Composable
private fun Screen(state: RouteSummaryUiState) = HaptiMapTheme {
    AppScaffold(timeText = { HaptiTimeText() }) { RouteSummaryContent(state, {}, {}) }
}

@Composable
private fun Failed(error: RouteError) = Screen(RouteSummaryUiState(RouteStatus.Failed(error)))

@PreviewTest @HaptiPreviews @Composable
fun RouteReady() = Screen(RouteSummaryUiState(status = null, route = sampleRoute()))

/** Un destino de dos líneas: HeadingText no se corta arriba en la redonda. */
@PreviewTest @HaptiPreviews @Composable
fun RouteReadyLongName() = Screen(RouteSummaryUiState(status = null, route = sampleRoute().let { it.copy(destination = it.destination.copy(name = LONG_NAME)) }))

@PreviewTest @HaptiPreviews @Composable
fun RouteLoading() = Screen(RouteSummaryUiState(RouteStatus.Loading))

@PreviewTest @HaptiPreviews @Composable
fun RouteNoConnection() = Failed(RouteError.NO_CONNECTION)

@PreviewTest @HaptiPreviews @Composable
fun RouteNoLocation() = Failed(RouteError.NO_LOCATION)

@PreviewTest @HaptiPreviews @Composable
fun RouteNoRoute() = Failed(RouteError.NO_ROUTE)

@PreviewTest @HaptiPreviews @Composable
fun RouteMissingKey() = Failed(RouteError.MISSING_KEY)

@PreviewTest @HaptiPreviews @Composable
fun RouteQuota() = Failed(RouteError.API_QUOTA)

@PreviewTest @HaptiPreviews @Composable
fun RouteInvalidKey() = Failed(RouteError.API_INVALID_KEY)

@PreviewTest @HaptiPreviews @Composable
fun RouteApiError() = Failed(RouteError.API_ERROR)

private const val LONG_NAME = "Museo Nacional Centro de Arte Reina Sofía"
