package com.xanderjs.haptimap.core.location

import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.GpsStatus
import com.xanderjs.haptimap.core.model.LocationFix
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withTimeoutOrNull

/** Contrato del Location Engine (sección 5.1). El muestreo lo decide [AdaptiveLocationPolicy] (paso 23). */
interface LocationSource {
    /** Estado del GPS mientras haya suscriptores; al dejar de recolectar se detienen las actualizaciones. */
    fun gpsStatus(sampling: LocationSampling = LocationSampling.Cruise): Flow<GpsStatus>
}

/** Cuánto se espera a la primera posición antes de pedir datos de red; pasado esto se pide sin posición. */
const val LOCATION_FIX_TIMEOUT_MILLIS = 10_000L

/**
 * La primera posición utilizable (Fix o LowAccuracy), o null si el GPS falla o no la da en [timeoutMillis].
 * `firstOrNull`: un flujo que termina sin fix no lanza.
 */
suspend fun LocationSource.firstPoint(timeoutMillis: Long = LOCATION_FIX_TIMEOUT_MILLIS): GeoPoint? {
    val status = withTimeoutOrNull(timeoutMillis) { gpsStatus().firstOrNull { it != GpsStatus.Searching } }
    return when (status) {
        is GpsStatus.Fix -> status.location.point
        is GpsStatus.LowAccuracy -> status.location.point
        else -> null
    }
}

/** Solo las posiciones, también las de baja precisión: el Navigation Engine decide qué hacer con cada una. */
fun LocationSource.fixes(sampling: LocationSampling = LocationSampling.Cruise): Flow<LocationFix> = gpsStatus(sampling).mapNotNull {
    when (it) {
        is GpsStatus.Fix -> it.location
        is GpsStatus.LowAccuracy -> it.location
        else -> null
    }
}

/**
 * Posiciones con muestreo adaptativo (paso 23): cada nuevo [LocationSampling] reinicia la petición de ubicación
 * (flatMapLatest, es un flujo frío), por eso [AdaptiveLocationPolicy] limita cuántas veces cambia. `distinctUntilChanged`
 * evita reiniciar si el muestreo no cambió.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun LocationSource.adaptiveFixes(sampling: Flow<LocationSampling>): Flow<LocationFix> =
    sampling.distinctUntilChanged().flatMapLatest { fixes(it) }
