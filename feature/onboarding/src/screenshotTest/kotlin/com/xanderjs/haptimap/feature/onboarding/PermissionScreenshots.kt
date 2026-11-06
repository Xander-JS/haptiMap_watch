package com.xanderjs.haptimap.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.AppScaffold
import com.android.tools.screenshot.PreviewTest
import com.xanderjs.haptimap.designsystem.atoms.HaptiTimeText
import com.xanderjs.haptimap.designsystem.preview.HaptiLargeFontPreviews
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

// Cada paso del permiso de ubicación en las 12 combinaciones de @HaptiPreviews (desde el paso 07: la regla
// de la primera vista, DESIGN_SYSTEM.md §7). Referencias: ./gradlew :feature:onboarding:updateDebugScreenshotTest.

@Composable
private fun Screen(step: PermissionStep) = HaptiMapTheme {
    AppScaffold(timeText = { HaptiTimeText() }) { PermissionContent(step) {} }
}

@PreviewTest @HaptiPreviews @Composable
fun PermissionRationale() = Screen(PermissionStep.Rationale)

@PreviewTest @HaptiPreviews @Composable
fun PermissionDenied() = Screen(PermissionStep.Denied)

@PreviewTest @HaptiPreviews @Composable
fun PermissionBlocked() = Screen(PermissionStep.Blocked)

@PreviewTest @HaptiPreviews @Composable
fun PermissionApproximate() = Screen(PermissionStep.Approximate)

@PreviewTest @HaptiPreviews @Composable
fun PermissionApproximateBlocked() = Screen(PermissionStep.ApproximateBlocked)

// Paso 15A: el flujo principal con la letra más grande del sistema (1.24), 12 combinaciones

@PreviewTest @HaptiLargeFontPreviews @Composable
fun LargeFontPermissionRationale() = PermissionRationale()
