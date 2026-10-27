package com.xanderjs.haptimap.core.location

import com.xanderjs.haptimap.core.model.GpsStatus
import kotlinx.coroutines.flow.Flow

/** Intervalo entre lecturas a ritmo de peatón. El paso 23 lo hará adaptativo. */
const val DEFAULT_INTERVAL_MILLIS = 1_000L

/** Contrato del Location Engine (sección 5.1). */
interface LocationSource {
    /** Estado del GPS mientras haya suscriptores; al dejar de recolectar se detienen las actualizaciones. */
    fun gpsStatus(intervalMillis: Long = DEFAULT_INTERVAL_MILLIS): Flow<GpsStatus>
}
