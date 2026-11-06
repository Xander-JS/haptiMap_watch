package com.xanderjs.haptimap.core.intelligence

import java.util.concurrent.ConcurrentHashMap

/**
 * Memoria de POIs rechazados durante un trayecto (paso 18): uno ignorado o caducado no se vuelve a
 * sugerir en el mismo paseo. El evaluador la consulta; la capa de Tour (pasos 19-20) añade los
 * rechazados y la vacía al terminar el trayecto. Vive en memoria: no sobrevive a la muerte del proceso.
 *
 * Segura entre hilos (paso 19): la lee el flujo de evaluación de la tubería de POIs y la escribe el
 * controlador de sugerencias (timeout/descarte/aceptación), en corrutinas distintas. Un set concurrente
 * basta: cada operación (reject/contains/clear) es atómica y no se componen entre sí.
 */
class RejectedPoiMemory {
    private val rejected = ConcurrentHashMap.newKeySet<String>()

    fun reject(id: String) {
        rejected += id
    }

    operator fun contains(id: String): Boolean = id in rejected

    /** Al terminar el trayecto. */
    fun clear() = rejected.clear()
}
