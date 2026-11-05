package com.xanderjs.haptimap.presentation.debug

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xanderjs.haptimap.core.haptics.PlayedHaptic
import com.xanderjs.haptimap.designsystem.molecules.HapticCueBadge
import com.xanderjs.haptimap.feature.onboarding.label
import com.xanderjs.haptimap.feature.onboarding.playbackProgress
import com.xanderjs.haptimap.feature.onboarding.tint
import com.xanderjs.haptimap.feature.onboarding.toSegments
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow

/** Tiempo que el indicador sigue a la vista después de acabar el patrón. */
private val LINGER = 1.5.seconds

// bajo la distancia de la pantalla de navegación, sin taparla; a esa altura la pantalla redonda mide
// ~0,67 de ancho; los extremos redondos de la píldora dejan pasar algo más (el texto se corta con elipsis)
private val BADGE_ALIGNMENT = BiasAlignment(0f, 0.72f)
private const val BADGE_MAX_WIDTH_FRACTION = 0.74f

/**
 * Solo debug (src/release es un composable vacío): sobre cualquier pantalla, el patrón que acaba de sonar,
 * para enseñar el flujo háptico entero en el emulador, que no vibra.
 */
@Composable
fun DebugHapticOverlay(played: StateFlow<PlayedHaptic?>) {
    val last by played.collectAsStateWithLifecycle()
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(last) {
        val shown = last ?: return@LaunchedEffect
        visible = true
        delay(shown.pattern.totalDurationMs.milliseconds + LINGER)
        visible = false
    }
    val progress = playbackProgress(last)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible,
            Modifier.align(BADGE_ALIGNMENT).widthIn(max = maxWidth * BADGE_MAX_WIDTH_FRACTION),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            last?.let { HapticCueBadge(stringResource(it.event.label), it.pattern.toSegments(), progress = progress ?: 1f, tint = it.event.tint) }
        }
    }
}
