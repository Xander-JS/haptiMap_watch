package com.xanderjs.haptimap.designsystem.atoms

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * Un aviso háptico que acaba de sonar: cuándo empieza cada pulso desde el inicio del patrón ([startsMs]) y cuándo sonó
 * ([atUptimeMs], de SystemClock.uptimeMillis). Clase y no data class: dos avisos iguales seguidos son dos pulsos.
 */
@Immutable
class HapticPulse(val startsMs: List<Long>, val atUptimeMs: Long)

/** Lo que tarda un anillo en expandirse y desvanecerse desde que empieza su pulso. */
internal const val RIPPLE_MS = 700

/**
 * La vibración hecha visible (rediseño Pulso): un anillo fino en reposo alrededor del disco de la maniobra y, cuando
 * suena un aviso real ([pulse]), un anillo que se expande por cada pulso del patrón, a su ritmo. Una vez y se para:
 * nunca en bucle. Sin animación (ni anillos) en ambiente; y un aviso ya pasado (p. ej. al volver a la pantalla) no se
 * repite. Los anillos se dibujan fuera del sitio del componente ([HaptiDimens.pulseReach]) sin empujar nada.
 * Decorativo: TalkBack no lo lee.
 */
@Composable
fun PulseRings(pulse: HapticPulse?, color: Color, modifier: Modifier = Modifier) {
    if (HaptiMapTheme.ambient) return
    val dimens = HaptiMapTheme.dimens
    val elapsed = remember(pulse) { Animatable(0f) }
    LaunchedEffect(pulse) {
        if (pulse == null) return@LaunchedEffect
        val total = pulse.startsMs.max() + RIPPLE_MS
        val late = SystemClock.uptimeMillis() - pulse.atUptimeMs
        if (late >= total) return@LaunchedEffect
        elapsed.snapTo(late.coerceAtLeast(0).toFloat())
        elapsed.animateTo(total.toFloat(), tween((total - late).toInt(), easing = LinearEasing))
    }
    Canvas(modifier) {
        val disc = dimens.maneuverDisc.toPx() / 2
        drawCircle(color.copy(alpha = REST_ALPHA), disc + dimens.maneuverRingGap.toPx(), style = Stroke(dimens.outlineWidth.toPx()))
        if (pulse == null || !elapsed.isRunning) return@Canvas
        pulse.startsMs.forEach { start ->
            val p = (elapsed.value - start) / RIPPLE_MS
            if (p in 0f..1f) {
                val width = dimens.outlineWidth.toPx() * (1 + 2 * (1 - p))
                drawCircle(color.copy(alpha = 1 - p), disc + p * dimens.pulseReach.toPx(), style = Stroke(width))
            }
        }
    }
}

private const val REST_ALPHA = 0.45f
