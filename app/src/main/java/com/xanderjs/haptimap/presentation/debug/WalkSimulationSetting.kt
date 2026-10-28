package com.xanderjs.haptimap.presentation.debug

import androidx.annotation.StringRes
import com.xanderjs.haptimap.R
import com.xanderjs.haptimap.core.location.WalkSimulation
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Modos del simulador de paseo (paso 07). Solo los usa la compilación debug (src/debug/.../LocationModule):
 * en release el catálogo no muestra el selector y el GPS es siempre el real.
 */
enum class SimulationMode(@param:StringRes val label: Int, val walk: WalkSimulation?) {
    OFF(R.string.simulation_off, null),
    WALK(R.string.simulation_walk, WalkSimulation()),
    NOISY(R.string.simulation_noisy, WalkSimulation(noiseMeters = 4.0, spikeEvery = 15)),
    // 14 fixes malos seguidos cada 40: pasa de los 10 s y muestra "Señal GPS débil"
    WEAK_GPS(R.string.simulation_weak_gps, WalkSimulation(noiseMeters = 2.0, spikeEvery = 40, spikeLength = 14)),
    FAST(R.string.simulation_fast, WalkSimulation(speedMetersPerSecond = 5.0)),
    ;

    fun next(): SimulationMode = entries[(ordinal + 1) % entries.size]
}

/** El modo elegido en el catálogo, para todo el proceso. */
@Singleton
class WalkSimulationSetting @Inject constructor() {
    val mode = MutableStateFlow(SimulationMode.OFF)
}
