package com.xanderjs.haptimap.core.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Por debajo de esto una carga se percibe como instantánea: mostrar "Cargando…" solo daría un parpadeo. */
val SLOW_LOAD_THRESHOLD: Duration = 300.milliseconds

/**
 * Ejecuta [block] y llama a [onSlow] solo si tarda más de [after]. Para enseñar un indicador de carga
 * sin que parpadee en las cargas rápidas. El retraso corre en el dispatcher del llamador (tiempo virtual en tests).
 */
suspend fun <T> withSlowNotice(after: Duration = SLOW_LOAD_THRESHOLD, onSlow: () -> Unit, block: suspend () -> T): T =
    coroutineScope {
        val notice = launch {
            delay(after)
            onSlow()
        }
        try {
            block()
        } finally {
            notice.cancel()
        }
    }
