package com.xanderjs.haptimap.designsystem.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.wear.compose.foundation.requestFocusOnHierarchyActive
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ScreenScaffold
import com.xanderjs.haptimap.designsystem.R
import com.xanderjs.haptimap.designsystem.atoms.ArrowDirection
import com.xanderjs.haptimap.designsystem.atoms.HaptiButton
import com.xanderjs.haptimap.designsystem.atoms.HaptiButtonVariant
import com.xanderjs.haptimap.designsystem.atoms.HaptiTimeText
import com.xanderjs.haptimap.designsystem.atoms.InstructionText
import com.xanderjs.haptimap.designsystem.atoms.LocalOffline
import com.xanderjs.haptimap.designsystem.molecules.ManeuverIndicator
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Pantalla para mirar de un vistazo (paso 07, navegación). En la primera vista, centrado, [glance]
 * entero. Los [details], por orden de prioridad, van debajo en la primera vista mientras caben enteros;
 * desde el primero que no cabe, todos empiezan justo por debajo del borde inferior y se llega con scroll.
 * [belowFold] (acciones) siempre empieza por debajo del borde: ninguna línea de texto queda cortada por
 * abajo en la primera vista y no se pulsa nada al mirar.
 *
 * Columna con scroll y no TransformingLazyColumn: la lista centra su primer elemento y no deja fijar
 * qué cae en la primera vista. Corona giratoria con rotaryScrollable.
 *
 * [edgeButton] (paso 13): la acción principal en el borde inferior (HaptiEdgeButton), siempre en la primera vista
 * en las tres formas, sea cual sea el texto; el contenido se mide sobre la zona que queda encima.
 */
@Composable
fun GlanceScreenTemplate(
    glance: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    details: List<@Composable () -> Unit> = emptyList(),
    belowFold: @Composable ColumnScope.() -> Unit = {},
    edgeButton: (@Composable () -> Unit)? = null,
) {
    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    val offline = LocalOffline.current
    val round = LocalConfiguration.current.isScreenRound
    val widthFraction = HaptiMapTheme.dimens.centeredContentWidthFraction
    val clockBand = HaptiMapTheme.dimens.clockBandSafeTop
    val gap = HaptiMapTheme.spacing.s
    // clearance: lo que ocupa el botón del borde (0 sin él: la pantalla queda como antes del paso 13)
    val content: @Composable (PaddingValues, Dp) -> Unit = { contentPadding, clearance ->
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val screenHeight = maxHeight
            val top = contentPadding.calculateTopPadding() + clockBand
            val safeBottom = maxOf(safeBottom(screenHeight, contentPadding, round, widthFraction), clearance)
            val layoutDirection = LocalLayoutDirection.current
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .requestFocusOnHierarchyActive()
                    .rotaryScrollable(RotaryScrollableDefaults.behavior(scrollState), focusRequester)
                    .verticalScroll(scrollState)
                    .padding(
                        start = contentPadding.calculateStartPadding(layoutDirection),
                        top = top,
                        end = contentPadding.calculateEndPadding(layoutDirection),
                        // con botón, lo último puede subir por encima de él
                        bottom = contentPadding.calculateBottomPadding() + clearance,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(gap),
            ) {
                // lo que no cabe empieza fuera de la pantalla, como sin botón: no asoma a los lados del botón
                FirstView(safeHeight = screenHeight - top - safeBottom, visibleHeight = screenHeight - top, glance = glance, details = details)
                belowFold()
            }
        }
    }
    ScreenScaffold(scrollState = scrollState, modifier = modifier, timeText = { HaptiTimeText(offline) }) { contentPadding ->
        if (edgeButton == null) {
            content(contentPadding, 0.dp)
        } else {
            WithEdgeButton(edgeButton, gap) { clearance -> content(contentPadding, clearance) }
        }
    }
}

/**
 * La acción principal pegada al borde inferior, fija, con el contenido por detrás (paso 13). Se mide primero el botón
 * y el contenido recibe su alto más [gap]. No es el edgeButton de ScreenScaffold: ese se encoge hasta que el scroll
 * llega al final (pensado para listas) y con una columna con scroll no llegaba a verse.
 */
@Composable
private fun WithEdgeButton(edgeButton: @Composable () -> Unit, gap: Dp, content: @Composable (clearance: Dp) -> Unit) =
    SubcomposeLayout(Modifier.fillMaxSize()) { constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val button = subcompose("edgeButton", edgeButton).map { it.measure(loose) }
        val buttonHeight = button.maxOfOrNull { it.height } ?: 0
        val clearance = buttonHeight.toDp() + gap
        val body = subcompose("content") { content(clearance) }.map { it.measure(constraints) }
        layout(constraints.maxWidth, constraints.maxHeight) {
            body.forEach { it.place(0, 0) }
            button.forEach { it.place((constraints.maxWidth - it.width) / 2, constraints.maxHeight - it.height) }
        }
    }

/** Hasta dónde llega el contenido por abajo: el padding del scaffold y, en la redonda, la cuerda de [widthFraction]. */
internal fun safeBottom(screenHeight: Dp, contentPadding: PaddingValues, round: Boolean, widthFraction: Float): Dp {
    // en la redonda, el texto a widthFraction del ancho cabe hasta donde la cuerda mide eso
    val roundBottom = if (round) screenHeight / 2 * (1 - sqrt(1 - widthFraction * widthFraction)) else screenHeight * 0
    return maxOf(contentPadding.calculateBottomPadding(), roundBottom)
}

/**
 * [glance] y los [details] que caben enteros, centrados en [safeHeight]; el resto, en orden, desde
 * [visibleHeight] (el borde de la pantalla). Mide al menos [visibleHeight]: lo que siga empieza fuera de
 * la primera vista. Sin [placeDetails] (ambiente) los detalles solo reservan su sitio: [glance] queda donde
 * está en la vista interactiva.
 */
@Composable
internal fun FirstView(
    safeHeight: Dp,
    visibleHeight: Dp,
    glance: @Composable ColumnScope.() -> Unit,
    details: List<@Composable () -> Unit>,
    placeDetails: Boolean = true,
) {
    val gap = HaptiMapTheme.spacing.s
    Layout(
        content = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(gap),
                content = glance,
            )
            details.forEach { it() }
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0, maxHeight = Constraints.Infinity)
        val placeables = measurables.map { it.measure(loose) }
        val gapPx = gap.roundToPx()
        val safe = safeHeight.roundToPx()
        // cuántos (la vista rápida y los detalles que la siguen) caben enteros en la primera vista
        var shown = 1
        var block = placeables[0].height
        while (shown < placeables.size && block + gapPx + placeables[shown].height <= safe) {
            block += gapPx + placeables[shown].height
            shown++
        }
        val tops = IntArray(placeables.size)
        tops[0] = ((safe - block) / 2).coerceAtLeast(0)
        for (i in 1 until placeables.size) {
            val after = tops[i - 1] + placeables[i - 1].height + gapPx
            tops[i] = if (i == shown) max(after, visibleHeight.roundToPx() + gapPx) else after
        }
        // sin scroll (ambiente) no puede medir más que su sitio: el padre lo centraría y movería glance
        val height = max(visibleHeight.roundToPx(), tops.last() + placeables.last().height).coerceAtMost(constraints.maxHeight)
        val width = constraints.maxWidth
        layout(width, height) {
            val placed = if (placeDetails) placeables else placeables.take(1)
            placed.forEachIndexed { i, placeable -> placeable.placeRelative((width - placeable.width) / 2, tops[i]) }
        }
    }
}

@HaptiPreviews
@Composable
private fun GlanceScreenTemplatePreview() = HaptiMapTheme {
    AppScaffold(timeText = { HaptiTimeText() }) {
        GlanceScreenTemplate(
            glance = { ManeuverIndicator(direction = ArrowDirection.LEFT, distanceMeters = 45) },
            details = listOf { InstructionText(stringResource(R.string.sample_instruction)) },
            belowFold = {
                HaptiButton(
                    text = stringResource(R.string.sample_button_secondary),
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    variant = HaptiButtonVariant.Secondary,
                )
            },
        )
    }
}
