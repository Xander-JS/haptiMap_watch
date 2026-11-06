package com.xanderjs.haptimap.core.model

import kotlin.math.roundToInt

/**
 * Un patrón de vibración como datos: el segmento i dura timings[i] ms con amplitudes[i]
 * (0 = silencio, 255 = máximo). Es el formato de VibrationEffect.createWaveform.
 */
class HapticPattern(val timings: LongArray, val amplitudes: IntArray) {
    init {
        require(timings.size == amplitudes.size) { "timings y amplitudes deben tener el mismo tamaño" }
        require(amplitudes.all { it in 0..255 }) { "amplitud fuera de rango 0..255" }
    }

    val totalDurationMs: Long get() = timings.sum()

    /** Pulsos que se notan: segmentos con amplitud seguidos cuentan como uno (p. ej. una subida). */
    val pulseCount: Int get() = onOffTimings().size / 2

    /**
     * Para vibradores sin control de amplitud: apagado, encendido, apagado… empezando por un apagado
     * (formato de createWaveform(timings, repeat)). Los segmentos con amplitud seguidos se funden en uno.
     */
    fun onOffTimings(): LongArray {
        val out = mutableListOf(0L)
        timings.indices.forEach { i ->
            val on = amplitudes[i] > 0
            // índice impar = encendido
            if (on == (out.size % 2 == 0)) out[out.lastIndex] += timings[i] else out += timings[i]
        }
        if (out.size % 2 == 1) out.removeAt(out.lastIndex)
        return out.toLongArray()
    }

    /** Intensidad global (ajuste del paso 25): escala las amplitudes sin que un pulso desaparezca ni pase de 255. */
    fun scaled(intensity: Float): HapticPattern = HapticPattern(
        timings.copyOf(),
        IntArray(amplitudes.size) { i -> if (amplitudes[i] == 0) 0 else (amplitudes[i] * intensity).roundToInt().coerceIn(1, 255) },
    )
}

/**
 * Lenguaje háptico de HaptiMap (sección 12). Solo datos, como los tokens de diseño: el paso 14 los
 * recalibra aquí sin tocar lógica. Vive junto a HapticEvent para que core:haptics lo reproduzca sin
 * depender de Compose, y el catálogo lo dibuje desde aquí.
 * El `when` es exhaustivo: un HapticEvent nuevo sin patrón no compila.
 */
object HapticTokens {
    private const val OFF = 0
    private const val SOFT = 80
    private const val GENTLE = 120
    private const val MEDIUM = 160
    private const val STRONG = 255

    private const val TAP = 40L
    private const val PULSE = 120L
    private const val LONG = 500L
    private const val LONG_INTENSE = 900L
    private const val GAP = 150L
    private const val BURST_GAP = 50L

    // izquierda y derecha se distinguen por el ritmo además de por la cuenta: lento y largo frente a
    // rápido y corto. El ritmo sobrevive sin control de amplitud. Lo primero que probar en el reloj (paso 14).
    private const val LEFT_PULSE = 200L
    private const val LEFT_GAP = 250L
    private const val RIGHT_PULSE = 80L
    private const val RIGHT_GAP = 100L

    /** Devuelve arrays nuevos en cada llamada para que nadie mute el token compartido. */
    fun patternFor(event: HapticEvent): HapticPattern = when (event) {
        // 2 pulsos medios, lentos y largos: "taaa — taaa"
        HapticEvent.TURN_LEFT -> HapticPattern(
            longArrayOf(LEFT_PULSE, LEFT_GAP, LEFT_PULSE),
            intArrayOf(MEDIUM, OFF, MEDIUM),
        )
        // 3 pulsos medios, rápidos y cortos: "ta-ta-ta"
        HapticEvent.TURN_RIGHT -> HapticPattern(
            longArrayOf(RIGHT_PULSE, RIGHT_GAP, RIGHT_PULSE, RIGHT_GAP, RIGHT_PULSE),
            intArrayOf(MEDIUM, OFF, MEDIUM, OFF, MEDIUM),
        )
        // 1 pulso suave
        HapticEvent.CONTINUE -> HapticPattern(
            longArrayOf(PULSE),
            intArrayOf(SOFT),
        )
        // Pulsos progresivos: más fuertes, más largos y más seguidos (sin amplitud sigue siendo progresivo)
        HapticEvent.TURN_IMMINENT -> HapticPattern(
            longArrayOf(TAP, GAP, 70L, 110L, 100L, 70L, 150L),
            intArrayOf(SOFT, OFF, MEDIUM, OFF, STRONG, OFF, STRONG),
        )
        // Pulso largo + doble toque
        HapticEvent.ARRIVAL -> HapticPattern(
            longArrayOf(LONG, GAP, TAP, BURST_GAP, TAP),
            intArrayOf(MEDIUM, OFF, STRONG, OFF, STRONG),
        )
        // Pulso largo e intenso
        HapticEvent.OFF_ROUTE -> HapticPattern(
            longArrayOf(LONG_INTENSE),
            intArrayOf(STRONG),
        )
        // Una subida suave y continua: ni el toque corto de continuar ni los pulsos de un giro
        HapticEvent.BACK_ON_ROUTE -> HapticPattern(
            longArrayOf(130L, 130L, 140L),
            intArrayOf(SOFT, GENTLE, MEDIUM),
        )
        // Ráfaga distintiva: 4 toques rápidos, suave-fuerte alternados
        HapticEvent.POI_SUGGESTION -> HapticPattern(
            longArrayOf(TAP, BURST_GAP, TAP, BURST_GAP, TAP, BURST_GAP, TAP),
            intArrayOf(SOFT, OFF, STRONG, OFF, SOFT, OFF, STRONG),
        )
        // Confirmación: un toque suave y uno firme seguidos, "tk-TUM". Corto, no se confunde con una señal de ruta.
        HapticEvent.PROFILE_SELECTED -> HapticPattern(
            longArrayOf(TAP, BURST_GAP, PULSE),
            intArrayOf(GENTLE, OFF, STRONG),
        )
    }
}
