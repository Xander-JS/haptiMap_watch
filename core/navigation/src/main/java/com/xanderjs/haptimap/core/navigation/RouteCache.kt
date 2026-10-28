package com.xanderjs.haptimap.core.navigation

import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.Maneuver
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.PlaceCategory
import com.xanderjs.haptimap.core.model.Route
import com.xanderjs.haptimap.core.model.RouteStep
import com.xanderjs.haptimap.core.model.decodePolyline
import com.xanderjs.haptimap.core.model.encodePolyline
import java.io.File
import java.io.IOException
import java.util.logging.Logger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Sube al cambiar el formato: una entrada con otra versión se descarta. */
const val ROUTE_CACHE_VERSION = 1

/** Un paseo retomado tras matar el proceso tiene minutos; una ruta de hace horas ya no sirve. */
val ROUTE_CACHE_MAX_AGE = 6.hours

/**
 * La ruta activa en un archivo JSON (paso 06), para seguir sin red y sobrevivir a la muerte del
 * proceso. Una sola ruta: sin base de datos. [load] devuelve null, sin lanzar, si no hay archivo, si
 * está corrupto, si es de otra versión o si tiene más de [maxAge]; los inválidos se borran.
 */
class RouteCache(
    private val file: File,
    private val maxAge: Duration = ROUTE_CACHE_MAX_AGE,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    fun save(route: Route) {
        val tmp = File(file.path + ".tmp")
        try {
            tmp.writeText(Json.encodeToString(CachedRouteFile(ROUTE_CACHE_VERSION, nowMillis(), route.toCached())))
            // renombrar es atómico: si el proceso muere a mitad, queda el archivo anterior entero
            if (!tmp.renameTo(file)) logger.warning("Could not replace $file")
        } catch (e: IOException) {
            logger.warning("Could not save route: $e")
        }
    }

    fun load(): Route? {
        if (!file.exists()) return null
        val entry = try {
            Json.decodeFromString<CachedRouteFile>(file.readText())
        } catch (e: Exception) {
            // contenido de disco: cualquier fallo al leerlo o decodificarlo es "no hay ruta"
            logger.warning("Discarding corrupted route cache: $e")
            null
        }
        val reason = when {
            entry == null -> "corrupted"
            entry.version != ROUTE_CACHE_VERSION -> "version ${entry.version}"
            (nowMillis() - entry.savedAtMillis).milliseconds > maxAge -> "expired"
            entry.route.steps.isEmpty() -> "no steps"
            else -> return entry.route.toRoute()
        }
        logger.info("Discarding route cache: $reason")
        clear()
        return null
    }

    fun clear() {
        file.delete()
    }

    /**
     * Sigue la sesión: guarda cada nueva [activeRoute] y borra la caché cuando deja de haberla
     * (Arrived, Completed, Cancel, un destino nuevo). Se salta el estado inicial: al arrancar la
     * sesión está en Idle y la ruta guardada tiene que seguir ahí para ofrecer retomarla.
     */
    suspend fun follow(session: StateFlow<NavigationSession>) {
        session.drop(1).map { it.activeRoute }.distinctUntilChanged().collect { route ->
            if (route != null) save(route) else clear()
        }
    }

    private companion object {
        val logger: Logger = Logger.getLogger("HaptiRouteCache")
    }
}

// DTOs propios de la caché: el formato en disco no depende de cómo cambie el dominio.
// Las geometrías van como polilínea codificada (la misma precisión 1e5 con la que llegan de Google).

@Serializable
private data class CachedRouteFile(val version: Int, val savedAtMillis: Long, val route: CachedRoute)

@Serializable
private data class CachedRoute(
    val destination: CachedPlace,
    val distanceMeters: Int,
    val durationMillis: Long,
    val polyline: String,
    val steps: List<CachedStep>,
)

@Serializable
private data class CachedPlace(
    val id: String,
    val name: String,
    val location: CachedPoint,
    val category: PlaceCategory?,
    val address: String?,
    val distanceMeters: Int,
)

@Serializable
private data class CachedStep(
    val maneuver: Maneuver,
    val distanceMeters: Int,
    val durationMillis: Long,
    val polyline: String,
    val start: CachedPoint,
    val end: CachedPoint,
    val instruction: String?,
    val sourceManeuver: String?,
)

@Serializable
private data class CachedPoint(val latitude: Double, val longitude: Double)

private fun Route.toCached() = CachedRoute(
    CachedPlace(destination.id, destination.name, destination.location.toCached(), destination.category, destination.address, destination.distanceMeters),
    distanceMeters,
    duration.inWholeMilliseconds,
    encodePolyline(points),
    steps.map {
        CachedStep(it.maneuver, it.distanceMeters, it.duration.inWholeMilliseconds, encodePolyline(it.points), it.start.toCached(), it.end.toCached(), it.instruction, it.sourceManeuver)
    },
)

private fun CachedRoute.toRoute() = Route(
    Place(destination.id, destination.name, destination.location.toGeoPoint(), destination.category, destination.address, destination.distanceMeters),
    distanceMeters,
    durationMillis.milliseconds,
    decodePolyline(polyline),
    steps.map {
        RouteStep(it.maneuver, it.distanceMeters, it.durationMillis.milliseconds, decodePolyline(it.polyline), it.start.toGeoPoint(), it.end.toGeoPoint(), it.instruction, it.sourceManeuver)
    },
)

private fun GeoPoint.toCached() = CachedPoint(latitude, longitude)

private fun CachedPoint.toGeoPoint() = GeoPoint(latitude, longitude)
