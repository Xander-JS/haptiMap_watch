package com.xanderjs.haptimap.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.android.tools.screenshot.PreviewTest
import com.xanderjs.haptimap.designsystem.atoms.NoticeLabel
import com.xanderjs.haptimap.designsystem.atoms.StatusTone
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

// NoticeLabel (paso 06) en sus cuatro tonos, en las 12 combinaciones de @HaptiPreviews. El aviso real
// de sin red, con su texto, se captura en :feature:navigation-ui (RouteReadyOffline).

@PreviewTest @HaptiPreviews @Composable
fun NoticeLabelTones() = PreviewSurface {
    Column(verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.s)) {
        StatusTone.entries.forEach { NoticeLabel(stringResource(R.string.sample_notice), it) }
    }
}
