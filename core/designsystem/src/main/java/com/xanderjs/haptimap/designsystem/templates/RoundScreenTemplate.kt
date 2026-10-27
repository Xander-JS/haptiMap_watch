package com.xanderjs.haptimap.designsystem.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ScreenScaffold
import com.xanderjs.haptimap.designsystem.atoms.ArrowDirection
import com.xanderjs.haptimap.designsystem.atoms.HaptiTimeText
import com.xanderjs.haptimap.designsystem.atoms.LocalOffline
import com.xanderjs.haptimap.designsystem.molecules.ManeuverIndicator
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * Plantilla base de pantalla redonda: TimeText arriba, indicador de scroll, márgenes seguros
 * (ScreenScaffold calcula el padding para que nada quede cortado por el círculo, más clockBandSafeTop
 * bajo la hora) y lista con
 * scroll por corona giratoria. Debe vivir dentro de un AppScaffold con timeText = { HaptiTimeText() }:
 * tras una recomposición el scaffold de Wear puede volver a la hora de la app.
 */
@Composable
fun RoundScreenTemplate(
    modifier: Modifier = Modifier,
    content: TransformingLazyColumnScope.() -> Unit,
) {
    val listState = rememberTransformingLazyColumnState()
    // se lee aquí: Wear compone timeText dentro de AppScaffold, fuera de los providers de la pantalla
    val offline = LocalOffline.current
    ScreenScaffold(
        scrollState = listState,
        modifier = modifier,
        timeText = { HaptiTimeText(offline) },
    ) { contentPadding ->
        val layoutDirection = LocalLayoutDirection.current
        TransformingLazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                start = contentPadding.calculateStartPadding(layoutDirection),
                top = contentPadding.calculateTopPadding() + HaptiMapTheme.dimens.clockBandSafeTop,
                end = contentPadding.calculateEndPadding(layoutDirection),
                bottom = contentPadding.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(HaptiMapTheme.spacing.s),
            content = content,
        )
    }
}

@HaptiPreviews
@Composable
private fun RoundScreenTemplatePreview() = HaptiMapTheme {
    AppScaffold(timeText = { HaptiTimeText() }) {
        RoundScreenTemplate {
            item { ManeuverIndicator(direction = ArrowDirection.RIGHT, distanceMeters = 120) }
        }
    }
}
