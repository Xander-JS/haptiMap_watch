package com.xanderjs.haptimap.presentation.debug

import androidx.lifecycle.ViewModel
import com.xanderjs.haptimap.core.haptics.HapticEngine
import com.xanderjs.haptimap.core.haptics.PlayedHaptic
import com.xanderjs.haptimap.core.model.HapticEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/** El banco de pruebas pide los patrones al motor, no al vibrador: suenan con las mismas reglas y el mismo log. */
@HiltViewModel
class HapticsDebugViewModel @Inject constructor(private val engine: HapticEngine) : ViewModel() {
    val played: StateFlow<PlayedHaptic?> = engine.played

    fun play(event: HapticEvent) = engine.play(event)
}
