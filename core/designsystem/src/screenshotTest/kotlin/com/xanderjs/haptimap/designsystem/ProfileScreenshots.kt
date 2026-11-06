package com.xanderjs.haptimap.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.android.tools.screenshot.PreviewTest
import com.xanderjs.haptimap.designsystem.molecules.ProfileOption
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

// ProfileOption (paso 16) seleccionado y sin seleccionar, en las 12 combinaciones de @HaptiPreviews.
// Referencias: ./gradlew :core:designsystem:updateDebugScreenshotTest; comprobar: validateDebugScreenshotTest.

@PreviewTest
@HaptiPreviews
@Composable
fun ProfileOptions() = PreviewSurface {
    Column(verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.s)) {
        ProfileOption(
            icon = painterResource(R.drawable.ic_profile_focus),
            title = stringResource(R.string.sample_profile_focus),
            subtitle = stringResource(R.string.sample_profile_focus_subtitle),
            selected = true,
            onClick = {},
        )
        ProfileOption(
            icon = painterResource(R.drawable.ic_profile_tour),
            title = stringResource(R.string.sample_profile_tour),
            subtitle = stringResource(R.string.sample_profile_tour_subtitle),
            selected = false,
            onClick = {},
        )
    }
}
