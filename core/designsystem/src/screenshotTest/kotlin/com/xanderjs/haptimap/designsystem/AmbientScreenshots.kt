package com.xanderjs.haptimap.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.AppScaffold
import com.android.tools.screenshot.PreviewTest
import com.xanderjs.haptimap.designsystem.atoms.ArrowDirection
import com.xanderjs.haptimap.designsystem.atoms.DirectionArrow
import com.xanderjs.haptimap.designsystem.atoms.HaptiIconSize
import com.xanderjs.haptimap.designsystem.atoms.HaptiTimeText
import com.xanderjs.haptimap.designsystem.atoms.LocalOffline
import com.xanderjs.haptimap.designsystem.atoms.NoticeLabel
import com.xanderjs.haptimap.designsystem.atoms.StatusTone
import com.xanderjs.haptimap.designsystem.molecules.ManeuverIndicator
import com.xanderjs.haptimap.designsystem.organisms.StatusMessage
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.templates.AmbientScreenTemplate
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

// Paso 12: los componentes en el tema de ambiente, en las 12 combinaciones de @HaptiPreviews. Claro y oscuro
// deben salir iguales: AOD es oscuro siempre. Las pantallas de ambiente reales están en :feature:navigation-ui.

/** Las flechas de contorno, grandes y medianas. */
@PreviewTest @HaptiPreviews @Composable
fun AmbientArrows() = PreviewSurface(ambient = true) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.m)) {
        Row(horizontalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.s)) {
            ArrowDirection.entries.forEach { DirectionArrow(direction = it) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.m)) {
            ArrowDirection.entries.forEach { DirectionArrow(direction = it, size = HaptiIconSize.Medium) }
        }
    }
}

/** Avisos sin píldora (surface negro) y el estado de carga sin spinner: en ambiente no hay rellenos ni animaciones. */
@PreviewTest @HaptiPreviews @Composable
fun AmbientNoticeAndStatus() = PreviewSurface(ambient = true) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.s)) {
        NoticeLabel(stringResource(R.string.sample_notice), StatusTone.Warning)
        StatusMessage(stringResource(R.string.sample_status_loading), loading = true)
    }
}

/** La plantilla sin red, con el tema de ambiente en la raíz como en MainActivity: también la hora va en gris y negro. */
@PreviewTest @HaptiPreviews @Composable
fun AmbientTemplateOffline() = HaptiMapTheme(ambient = true) {
    CompositionLocalProvider(LocalOffline provides true) {
        AppScaffold(timeText = { HaptiTimeText() }) {
            AmbientScreenTemplate { ManeuverIndicator(direction = ArrowDirection.RIGHT, distanceMeters = 150) }
        }
    }
}
