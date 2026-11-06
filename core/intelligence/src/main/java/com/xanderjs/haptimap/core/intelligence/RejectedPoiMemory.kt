package com.xanderjs.haptimap.core.intelligence

/**
 * Memoria de POIs rechazados durante un trayecto (paso 18): uno ignorado o caducado no se vuelve a
 * sugerir en el mismo paseo. El evaluador la consulta; la capa de Tour (pasos 19-20) añade los
 * rechazados y la vacía al terminar el trayecto. Vive en memoria: no sobrevive a la muerte del proceso.
 *
 * shortcut: sin sincronizar; un único flujo de evaluación la usa. Si llega a tocarse desde varios
 * hilos, protegerla.
 */
class RejectedPoiMemory {
    private val rejected = mutableSetOf<String>()

    fun reject(id: String) {
        rejected += id
    }

    operator fun contains(id: String): Boolean = id in rejected

    /** Al terminar el trayecto. */
    fun clear() = rejected.clear()
}
