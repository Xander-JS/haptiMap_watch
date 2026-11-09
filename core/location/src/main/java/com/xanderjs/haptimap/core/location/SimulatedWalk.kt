package com.xanderjs.haptimap.core.location

import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.GpsStatus
import com.xanderjs.haptimap.core.model.LocationFix
import com.xanderjs.haptimap.core.model.MeasuredPath
import com.xanderjs.haptimap.core.model.bearingTo
import com.xanderjs.haptimap.core.model.offset
import java.util.Random
import kotlin.math.min
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * Cómo camina el simulador (paso 07, solo depuración): a [speedMetersPerSecond], con ruido gaussiano de
 * [noiseMeters] por eje y, si [spikeEvery] > 0, los últimos [spikeLength] fixes de cada [spikeEvery] con
 * [spikeAccuracyMeters] de error y desplazados [spikeOffsetMeters]. Misma semilla, mismo recorrido.
 */
data class WalkSimulation(
    val speedMetersPerSecond: Double = 1.4,
    val noiseMeters: Double = 0.0,
    val accuracyMeters: Float = 5f,
    val spikeEvery: Int = 0,
    val spikeLength: Int = 1,
    val spikeAccuracyMeters: Float = 40f,
    val spikeOffsetMeters: Double = 40.0,
    /** Ritmo fijo del paseo simulado (independiente del muestreo adaptativo del paso 23). */
    val intervalMillis: Long = 1_000L,
    val seed: Long = 1,
)

/**
 * Un fix cada intervalo recorriendo [points] desde el inicio; al llegar al final se queda allí.
 * Sin fin: para cuando se deja de recolectar, como el GPS real.
 */
fun simulatedWalk(points: List<GeoPoint>, simulation: WalkSimulation, nowMillis: () -> Long = System::currentTimeMillis): Flow<LocationFix> = flow {
    val path = MeasuredPath(points)
    val random = Random(simulation.seed)
    val step = simulation.speedMetersPerSecond * simulation.intervalMillis / 1000
    var index = 0
    while (true) {
        val walked = min(index * step, path.length)
        val onPath = path.pointAt(walked)
        val moving = walked < path.length
        val spike = simulation.spikeEvery > 0 && index % simulation.spikeEvery >= simulation.spikeEvery - simulation.spikeLength
        var point = onPath.offset(0.0, random.nextGaussian() * simulation.noiseMeters).offset(90.0, random.nextGaussian() * simulation.noiseMeters)
        if (spike) point = point.offset(random.nextDouble() * 360, simulation.spikeOffsetMeters)
        emit(
            LocationFix(
                point = point,
                accuracyMeters = if (spike) simulation.spikeAccuracyMeters else simulation.accuracyMeters,
                speedMetersPerSecond = if (moving) simulation.speedMetersPerSecond.toFloat() else 0f,
                bearingDegrees = if (moving) onPath.bearingTo(path.pointAt(walked + 1)).toFloat() else null,
                timestampMillis = nowMillis(),
            ),
        )
        index++
        delay(simulation.intervalMillis)
    }
}

/** El recorrido simulado como estado del GPS, igual que lo da FusedLocationSource. */
fun simulatedGpsStatus(points: List<GeoPoint>, simulation: WalkSimulation, nowMillis: () -> Long = System::currentTimeMillis): Flow<GpsStatus> =
    simulatedWalk(points, simulation, nowMillis).map(GpsStatus::from).onStart { emit(GpsStatus.Searching) }

/**
 * Un desvío para el simulador (paso 08): a [atMeters] de la ruta sale [awayMeters] en perpendicular, a la
 * izquierda; si [returns], vuelve por el mismo camino y sigue la ruta, si no, se queda allí.
 */
data class Detour(val atMeters: Double, val awayMeters: Double, val returns: Boolean)

/** [points] con el desvío intercalado, para pasarlo a [simulatedWalk]. */
fun List<GeoPoint>.withDetour(detour: Detour): List<GeoPoint> {
    val path = MeasuredPath(this)
    val at = path.pointAt(detour.atMeters)
    val away = at.offset(at.bearingTo(path.pointAt(detour.atMeters + 1)) - 90, detour.awayMeters)
    val before = filterIndexed { i, _ -> path.cumulative[i] < detour.atMeters }
    if (!detour.returns) return before + at + away
    return before + at + away + at + filterIndexed { i, _ -> path.cumulative[i] > detour.atMeters }
}
