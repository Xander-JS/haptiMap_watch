package com.xanderjs.haptimap.feature.navigationui

import com.xanderjs.haptimap.core.intelligence.PoiCandidate
import com.xanderjs.haptimap.core.intelligence.PoiDecision
import com.xanderjs.haptimap.core.intelligence.PoiEvaluation
import com.xanderjs.haptimap.core.model.ExplorationCategory
import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.navigation.CachedRoute
import com.xanderjs.haptimap.core.navigation.toCached
import com.xanderjs.haptimap.core.navigation.toRoute
import java.io.File
import java.io.IOException
import java.util.logging.Logger
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Sube al cambiar el formato: una entrada con otra versión se descarta. */
const val DETOUR_STORE_VERSION = 1

/** La fase del desvío que se guarda (las transitorias Loading/PoiReached no se persisten como tales). */
enum class DetourPhase { TO_POI, RETURNING }

/** Lo retomado tras la muerte del proceso: el POI, la ruta original y la fase; la ruta del desvío sale de la sesión. */
data class RestoredDetour(val poi: PoiEvaluation, val originalRoute: com.xanderjs.haptimap.core.model.Route, val phase: DetourPhase)

/**
 * Persiste el contexto del desvío del Tour (paso 20) en un archivo JSON, junto a la caché de ruta del paso 06:
 * mantiene la ruta ORIGINAL disponible durante todo el desvío (para cancelar sin pedir otra si se sigue cerca) y deja
 * que la muerte del proceso se restaure correctamente. La ruta del DESVÍO que se navega la guarda ya la caché normal
 * (es la activa); aquí solo va lo que esa caché no sabe: el POI, la ruta original y la fase.
 *
 * Solo se guardan las fases con ruta de desvío ya en curso ([DetourPhase.TO_POI] y, para POI_REACHED/RETURNING,
 * [DetourPhase.RETURNING]); [TourDetour.Loading] y [TourDetour.Inactive] borran el archivo. Reutiliza [CachedRoute].
 */
class DetourStore(private val file: File) {

    fun save(state: TourDetour) {
        val entry = when (state) {
            is TourDetour.ToPoi -> entry(state.poi, state.originalRoute, DetourPhase.TO_POI)
            is TourDetour.PoiReached -> entry(state.poi, state.originalRoute, DetourPhase.RETURNING)
            is TourDetour.Returning -> entry(state.poi, state.originalRoute, DetourPhase.RETURNING)
            is TourDetour.Loading, TourDetour.Inactive -> return clear()
        }
        val tmp = File(file.path + ".tmp")
        try {
            tmp.writeText(Json.encodeToString(entry))
            if (!tmp.renameTo(file)) logger.warning("Could not replace $file")
        } catch (e: IOException) {
            logger.warning("Could not save detour: $e")
        }
    }

    fun load(): RestoredDetour? {
        if (!file.exists()) return null
        val entry = try {
            Json.decodeFromString<DetourFile>(file.readText())
        } catch (e: Exception) {
            logger.warning("Discarding corrupted detour store: $e")
            null
        }
        if (entry == null || entry.version != DETOUR_STORE_VERSION) {
            clear()
            return null
        }
        return RestoredDetour(entry.poi.toEvaluation(), entry.originalRoute.toRoute(), entry.phase)
    }

    fun clear() {
        file.delete()
    }

    private fun entry(poi: PoiEvaluation, originalRoute: com.xanderjs.haptimap.core.model.Route, phase: DetourPhase) =
        DetourFile(DETOUR_STORE_VERSION, poi.toCached(), originalRoute.toCached(), phase)

    private companion object {
        val logger: Logger = Logger.getLogger("HaptiDetourStore")
    }
}

@Serializable
private data class DetourFile(val version: Int, val poi: CachedPoi, val originalRoute: CachedRoute, val phase: DetourPhase)

// solo lo que la UI y el enrutado necesitan del POI; el score/decision se reconstruyen (no se muestran)
@Serializable
private data class CachedPoi(
    val id: String,
    val name: String,
    val category: ExplorationCategory?,
    val latitude: Double,
    val longitude: Double,
    val addedTimeMillis: Long,
    val detourMeters: Double,
)

private fun PoiEvaluation.toCached() = CachedPoi(
    candidate.id, candidate.name, candidate.category, candidate.location.latitude, candidate.location.longitude,
    addedTime.inWholeMilliseconds, detourMeters,
)

private fun CachedPoi.toEvaluation() = PoiEvaluation(
    candidate = PoiCandidate(id, name, category, GeoPoint(latitude, longitude)),
    score = 0.0,
    detourMeters = detourMeters,
    addedTime = addedTimeMillis.milliseconds,
    decision = PoiDecision.SUGGEST,
    discardReason = null,
)
