package com.xanderjs.haptimap.designsystem.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ScreenScaffold
import com.xanderjs.haptimap.designsystem.atoms.ArrowDirection
import com.xanderjs.haptimap.designsystem.atoms.HaptiTimeText
import com.xanderjs.haptimap.designsystem.molecules.ManeuverIndicator
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * Plantilla base de pantalla redonda: TimeText arriba, indicador de scroll, márgenes seguros
 * (ScreenScaffold calcula el padding para que nada quede cortado por el círculo) y lista con
 * scroll por corona giratoria. Debe vivir dentro de un AppScaffold con timeText = { HaptiTimeText() }:
 * tras una recomposición el scaffold de Wear puede volver a la hora de la app.
 */
@Composable
fun RoundScreenTemplate(
    modifier: Modifier = Modifier,
    content: TransformingLazyColumnScope.() -> Unit,
) {
    val listState = rememberTransformingLazyColumnState()
    ScreenScaffold(
        scrollState = listState,
        modifier = modifier,
        timeText = { HaptiTimeText() },
    ) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
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
