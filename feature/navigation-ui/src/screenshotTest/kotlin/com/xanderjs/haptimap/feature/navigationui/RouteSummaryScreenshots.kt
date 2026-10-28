package com.xanderjs.haptimap.feature.navigationui

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.AppScaffold
import com.android.tools.screenshot.PreviewTest
import com.xanderjs.haptimap.core.model.RouteError
import com.xanderjs.haptimap.designsystem.atoms.HaptiTimeText
import com.xanderjs.haptimap.designsystem.organisms.RouteStatus
import com.xanderjs.haptimap.designsystem.organisms.sampleRoute
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

// Cada estado de la pantalla en las 12 combinaciones de @HaptiPreviews (redonda S/L y cuadrada × claro/oscuro × en/es).
// Referencias: ./gradlew :feature:navigation-ui:updateDebugScreenshotTest; comprobar: validateDebugScreenshotTest.

@Composable
private fun Screen(state: RouteSummaryUiState) = HaptiMapTheme {
    AppScaffold(timeText = { HaptiTimeText() }) { RouteSummaryContent(state, {}, {}) }
}

@Composable
private fun Failed(error: RouteError) = Screen(RouteSummaryUiState(RouteStatus.Failed(error)))

@PreviewTest @HaptiPreviews @Composable
fun RouteReady() = Screen(RouteSummaryUiState(status = null, route = sampleRoute()))

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
