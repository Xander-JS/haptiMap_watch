package com.xanderjs.haptimap.core.location

import com.xanderjs.haptimap.core.model.LocationFix
import kotlinx.coroutines.flow.Flow

/**
 * Contrato del Location Engine (sección 5.1).
 * La implementación con Fused Location Provider y muestreo adaptativo
 * se desarrolla en la fase de GPS.
 */
interface LocationSource {
    /** Flujo continuo de posiciones mientras haya suscriptores. */
    fun locationUpdates(intervalMillis: Long): Flow<LocationFix>
}
