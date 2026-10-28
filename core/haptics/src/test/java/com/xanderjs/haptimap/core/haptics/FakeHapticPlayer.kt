package com.xanderjs.haptimap.core.haptics

import com.xanderjs.haptimap.core.model.HapticEvent
import com.xanderjs.haptimap.core.model.HapticPattern

/** Anota lo que se reproduce y cuándo, en el reloj que se le da (el virtual de runTest). */
class FakeHapticPlayer(private val now: () -> Long) : HapticPlayer {
    data class Played(val event: HapticEvent, val atMillis: Long, val pattern: HapticPattern)

    val played = mutableListOf<Played>()
    var cancels = 0

    val events get() = played.map { it.event }

    override fun play(event: HapticEvent, pattern: HapticPattern) {
        played += Played(event, now(), pattern)
    }

    override fun cancel() {
        cancels++
    }
}
