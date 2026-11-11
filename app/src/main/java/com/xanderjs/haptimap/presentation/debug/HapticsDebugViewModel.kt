package com.xanderjs.haptimap.presentation.debug

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xanderjs.haptimap.core.haptics.HapticEngine
import com.xanderjs.haptimap.core.haptics.PlayedHaptic
import com.xanderjs.haptimap.core.model.HapticEvent
import com.xanderjs.haptimap.designsystem.atoms.HapticPulse
import com.xanderjs.haptimap.feature.navigationui.pulseStartsMs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** El banco de pruebas pide los patrones al motor, no al vibrador: suenan con las mismas reglas y el mismo log. */
@HiltViewModel
class HapticsDebugViewModel @Inject constructor(private val engine: HapticEngine) : ViewModel() {
    val played: StateFlow<PlayedHaptic?> = engine.played

    /** Como NavigationViewModel.pulse, con el evento: el catálogo hace latir solo el disco del aviso que sonó. */
    val pulse: StateFlow<Pair<HapticEvent, HapticPulse>?> =
        engine.played.drop(1).filterNotNull()
            .map { it.event to HapticPulse(it.pattern.pulseStartsMs(), SystemClock.uptimeMillis()) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun play(event: HapticEvent) = engine.play(event)
}
