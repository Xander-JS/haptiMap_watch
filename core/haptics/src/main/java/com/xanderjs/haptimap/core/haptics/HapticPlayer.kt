package com.xanderjs.haptimap.core.haptics

import com.xanderjs.haptimap.core.model.HapticEvent
import com.xanderjs.haptimap.core.model.HapticPattern

/**
 * El hardware de vibración (sección 11). Solo lo llama [HapticEngine], que ya ha decidido qué suena y
 * cuándo: el reproductor no programa ni encola. [event] va para el log.
 */
interface HapticPlayer {
    fun play(event: HapticEvent, pattern: HapticPattern)
    fun cancel()
}
