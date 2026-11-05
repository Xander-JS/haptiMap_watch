package com.xanderjs.haptimap.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * El ciclo de vida del padre, pero como mucho CREATED mientras [stopped] (paso 12, ambiente). Las pantallas de debajo
 * de la vista de ambiente dejan así de recoger sus StateFlow (collectAsStateWithLifecycle) y no se redibujan con cada
 * fix; al salir vuelven a STARTED / RESUMED y recogen el valor actual, sin datos viejos.
 */
@Composable
fun rememberStoppableLifecycleOwner(stopped: Boolean): LifecycleOwner {
    val parent = LocalLifecycleOwner.current
    val owner = remember(parent) { StoppableLifecycleOwner() }
    DisposableEffect(parent, stopped) {
        // addObserver entrega en el acto el estado actual del padre: no hace falta sincronizar aparte
        val sync = LifecycleEventObserver { _, _ ->
            val state = parent.lifecycle.currentState
            owner.registry.currentState = if (stopped) minOf(state, Lifecycle.State.CREATED) else state
        }
        parent.lifecycle.addObserver(sync)
        onDispose { parent.lifecycle.removeObserver(sync) }
    }
    return owner
}

private class StoppableLifecycleOwner : LifecycleOwner {
    val registry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = registry
}
