package com.xanderjs.haptimap.presentation.debug

import androidx.annotation.StringRes
import com.xanderjs.haptimap.R
import com.xanderjs.haptimap.core.location.Detour
import com.xanderjs.haptimap.core.location.WalkSimulation
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Modos del simulador de paseo (paso 07). Solo los usa la compilación debug (src/debug/.../LocationModule):
 * en release el catálogo no muestra el selector y el GPS es siempre el real.
 *
 * Los de desvío (paso 08) salen de la ruta a 60 m, solo en la primera ruta (la recalculada se anda sin
 * desvío). Con los umbrales por defecto, a 1,4 m/s: fuera desde los 25 m, desviado 5 s después y la
 * petición 10 s más tarde. [offlineSeconds]: sin red desde que sale de la ruta (src/debug/.../ConnectivityModule).
 */
enum class SimulationMode(
    @param:StringRes val label: Int,
    val walk: WalkSimulation?,
    val detour: Detour? = null,
    val offlineSeconds: Int = 0,
) {
    OFF(R.string.simulation_off, null),
    WALK(R.string.simulation_walk, WalkSimulation()),
    NOISY(R.string.simulation_noisy, WalkSimulation(noiseMeters = 4.0, spikeEvery = 15)),
    // 14 fixes malos seguidos cada 40: pasa de los 10 s y muestra "Señal GPS débil"
    WEAK_GPS(R.string.simulation_weak_gps, WalkSimulation(noiseMeters = 2.0, spikeEvery = 40, spikeLength = 14)),
    FAST(R.string.simulation_fast, WalkSimulation(speedMetersPerSecond = 5.0)),
    // se aleja 300 m: desvío, recálculo y la ruta nueva hasta el destino
    LEAVE_ROUTE(R.string.simulation_leave, WalkSimulation(), Detour(atMeters = 60.0, awayMeters = 300.0, returns = false)),
    // 30 m y vuelta: vuelve 9 s después de declararse el desvío, antes de la petición (10 s; OffRouteSimulationTest)
    LEAVE_AND_RETURN(R.string.simulation_leave_return, WalkSimulation(), Detour(atMeters = 60.0, awayMeters = 30.0, returns = true)),
    // 31,5 m y vuelta: vuelve 11 s después, a mitad de la petición falsa de 2 s (OffRouteSimulationTest)
    RETURN_LATE(R.string.simulation_return_late, WalkSimulation(), Detour(atMeters = 60.0, awayMeters = 31.5, returns = true)),
    // se aleja sin red 45 s: se queda desviado con la ruta vieja y recalcula al volver la red
    LEAVE_OFFLINE(R.string.simulation_leave_offline, WalkSimulation(), Detour(atMeters = 60.0, awayMeters = 300.0, returns = false), offlineSeconds = 45),
    ;

    fun next(): SimulationMode = entries[(ordinal + 1) % entries.size]
}

/** El modo elegido en el catálogo, para todo el proceso. */
@Singleton
class WalkSimulationSetting @Inject constructor() {
    val mode = MutableStateFlow(SimulationMode.OFF)

    /** Sin red forzado por un modo con [SimulationMode.offlineSeconds]; lo lee la conectividad de debug. */
    val offline = MutableStateFlow(false)
}
