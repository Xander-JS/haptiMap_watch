package com.xanderjs.haptimap.core.haptics

import com.xanderjs.haptimap.core.model.HapticEvent

/**
 * Contrato del Haptic Engine (sección 11).
 * Abstrae el hardware de vibración: recibe eventos semánticos.
 */
interface HapticPlayer {
    fun play(event: HapticEvent)
    fun cancel()
}
