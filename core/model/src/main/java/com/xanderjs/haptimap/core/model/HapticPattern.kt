package com.xanderjs.haptimap.core.model

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
    val pulseCount: Int get() = amplitudes.count { it > 0 }
}

/**
 * Lenguaje háptico de HaptiMap. Solo datos: vive junto a HapticEvent para que core:haptics lo
 * reproduzca (paso 09) sin depender de Compose, y el catálogo lo muestre desde aquí.
 * El `when` es exhaustivo: un HapticEvent nuevo sin patrón no compila.
 */
object HapticTokens {
    private const val OFF = 0
    private const val SOFT = 80
    private const val MEDIUM = 160
    private const val STRONG = 255

    private const val TAP = 40L
    private const val PULSE = 120L
    private const val LONG = 500L
    private const val LONG_INTENSE = 900L
    private const val GAP = 150L
    private const val BURST_GAP = 50L

    /** Devuelve arrays nuevos en cada llamada para que nadie mute el token compartido. */
    fun patternFor(event: HapticEvent): HapticPattern = when (event) {
        // 2 pulsos medios
        HapticEvent.TURN_LEFT -> HapticPattern(
            longArrayOf(PULSE, GAP, PULSE),
            intArrayOf(MEDIUM, OFF, MEDIUM),
        )
        // 3 pulsos medios
        HapticEvent.TURN_RIGHT -> HapticPattern(
            longArrayOf(PULSE, GAP, PULSE, GAP, PULSE),
            intArrayOf(MEDIUM, OFF, MEDIUM, OFF, MEDIUM),
        )
        // 1 pulso suave
        HapticEvent.CONTINUE -> HapticPattern(
            longArrayOf(PULSE),
            intArrayOf(SOFT),
        )
        // Pulsos progresivos: cada vez más fuertes y más seguidos
        HapticEvent.TURN_IMMINENT -> HapticPattern(
            longArrayOf(TAP, GAP, TAP, PULSE, TAP, BURST_GAP, PULSE),
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
        // Ráfaga distintiva: 4 toques rápidos, suave-fuerte alternados
        HapticEvent.POI_SUGGESTION -> HapticPattern(
            longArrayOf(TAP, BURST_GAP, TAP, BURST_GAP, TAP, BURST_GAP, TAP),
            intArrayOf(SOFT, OFF, STRONG, OFF, SOFT, OFF, STRONG),
        )
    }
}
