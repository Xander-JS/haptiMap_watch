package com.xanderjs.haptimap.core.location

import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.GpsStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull

/** Intervalo entre lecturas a ritmo de peatón. El paso 23 lo hará adaptativo. */
const val DEFAULT_INTERVAL_MILLIS = 1_000L

/** Contrato del Location Engine (sección 5.1). */
interface LocationSource {
    /** Estado del GPS mientras haya suscriptores; al dejar de recolectar se detienen las actualizaciones. */
    fun gpsStatus(intervalMillis: Long = DEFAULT_INTERVAL_MILLIS): Flow<GpsStatus>
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
