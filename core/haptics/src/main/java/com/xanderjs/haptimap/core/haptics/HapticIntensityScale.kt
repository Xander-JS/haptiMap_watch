package com.xanderjs.haptimap.core.haptics

import com.xanderjs.haptimap.core.model.HapticIntensity

/**
 * El multiplicador de amplitud de cada intensidad (paso 25) con el que se alimenta [HapticEngine.intensity].
 * MEDIUM = 1.0 deja los patrones tal cual (el comportamiento hasta ahora); [HapticPattern.scaled] recorta a
 * 1..255, así que HIGH no desborda ni apaga ningún pulso.
 */
val HapticIntensity.amplitudeScale: Float
    get() = when (this) {
        HapticIntensity.LOW -> 0.5f
        HapticIntensity.MEDIUM -> 1.0f
        HapticIntensity.HIGH -> 1.5f
    }
