package com.xanderjs.haptimap.feature.onboarding

import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.xanderjs.haptimap.core.haptics.PlayedHaptic
import com.xanderjs.haptimap.core.model.HapticEvent
import com.xanderjs.haptimap.core.model.HapticPattern
import com.xanderjs.haptimap.core.model.HapticTokens
import com.xanderjs.haptimap.designsystem.atoms.WaveformSegment
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

// Del lenguaje háptico a valores de UI para HapticWaveform, HapticPatternCard y HapticCueBadge (DESIGN_SYSTEM.md §1).
// Aquí desde el paso 13: el tutorial háptico los usa, y el banco de pruebas y el indicador de debug de :app también.

private const val MAX_AMPLITUDE = 255f

fun HapticPattern.toSegments(): List<WaveformSegment> = timings.indices.map { WaveformSegment(timings[it], amplitudes[it] / MAX_AMPLITUDE) }

/** La escala común del banco de pruebas: el patrón más largo ocupa todo el ancho y los demás se comparan con él. */
val hapticScaleMs: Long = HapticEvent.entries.maxOf { HapticTokens.patternFor(it).totalDurationMs }

@get:StringRes
val HapticEvent.label: Int
    get() = when (this) {
        HapticEvent.TURN_LEFT -> R.string.haptic_turn_left
        HapticEvent.TURN_RIGHT -> R.string.haptic_turn_right
        HapticEvent.CONTINUE -> R.string.haptic_continue
        HapticEvent.TURN_IMMINENT -> R.string.haptic_turn_imminent
        HapticEvent.ARRIVAL -> R.string.haptic_arrival
        HapticEvent.OFF_ROUTE -> R.string.haptic_off_route
        HapticEvent.BACK_ON_ROUTE -> R.string.haptic_back_on_route
        HapticEvent.POI_SUGGESTION -> R.string.haptic_poi_suggestion
        HapticEvent.POI_ARRIVAL -> R.string.haptic_poi_arrival
        HapticEvent.PROFILE_SELECTED -> R.string.haptic_profile_selected
    }

/** El color del evento en la pantalla: el mismo rol que tiene ese estado en la navegación. */
val HapticEvent.tint: Color
    @Composable get() = when (this) {
        HapticEvent.OFF_ROUTE -> HaptiMapTheme.colors.offRoute
        HapticEvent.ARRIVAL -> HaptiMapTheme.colors.success
        HapticEvent.POI_SUGGESTION, HapticEvent.POI_ARRIVAL -> HaptiMapTheme.colors.poiHighlight
        else -> HaptiMapTheme.colors.primary
    }

/** Lo que va sonado de [played] (0..1) mientras suena; null antes y después. Sigue al reloj, no al vibrador. */
@Composable
fun playbackProgress(played: PlayedHaptic?): Float? {
    val progress = remember(played) { Animatable(0f) }
    var running by remember(played) { mutableStateOf(played != null) }
    LaunchedEffect(played) {
        if (played == null) return@LaunchedEffect
        progress.animateTo(1f, tween(played.pattern.totalDurationMs.toInt(), easing = LinearEasing))
        running = false
    }
    return if (running) progress.value else null
}
