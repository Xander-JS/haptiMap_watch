package com.xanderjs.haptimap.feature.navigationui

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.AppScaffold
import com.android.tools.screenshot.PreviewTest
import com.xanderjs.haptimap.core.model.NavigationProfile
import com.xanderjs.haptimap.designsystem.atoms.HaptiTimeText
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

// El selector de perfil (paso 16) con cada opción preseleccionada, en las 12 combinaciones de @HaptiPreviews.
// Referencias: ./gradlew :feature:navigation-ui:updateDebugScreenshotTest; comprobar: validateDebugScreenshotTest.

@Composable
private fun Screen(preselected: NavigationProfile) = HaptiMapTheme {
    AppScaffold(timeText = { HaptiTimeText() }) { ProfileSelectorContent(preselected = preselected, onSelect = {}) }
}

@PreviewTest @HaptiPreviews @Composable
fun ProfileSelectorFocus() = Screen(NavigationProfile.FOCUS)

@PreviewTest @HaptiPreviews @Composable
fun ProfileSelectorTour() = Screen(NavigationProfile.TOUR)
