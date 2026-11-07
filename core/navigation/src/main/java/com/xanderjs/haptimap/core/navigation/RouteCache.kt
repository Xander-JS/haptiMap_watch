package com.xanderjs.haptimap.core.navigation

import com.xanderjs.haptimap.core.model.NavigationProfile
import com.xanderjs.haptimap.core.model.Route
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

/** Sube al cambiar el formato: una entrada con otra versión se descarta. v2 añade el perfil (paso 18). */
const val ROUTE_CACHE_VERSION = 2

/** Un paseo retomado tras matar el proceso tiene minutos; una ruta de hace horas ya no sirve. */
val ROUTE_CACHE_MAX_AGE = 6.hours

/**
 * La ruta activa en un archivo JSON (paso 06), para seguir sin red y sobrevivir a la muerte del
 * proceso. Una sola ruta: sin base de datos. [load] devuelve null, sin lanzar, si no hay archivo, si
 * está corrupto, si es de otra versión o si tiene más de [maxAge]; los inválidos se borran.
 *
 * Guarda también el [NavigationProfile] del trayecto (paso 18): un paseo retomado conserva su perfil
 * real (Focus o Tour) en vez de volver siempre a Focus.
 */
class RouteCache(
    private val file: File,
    private val maxAge: Duration = ROUTE_CACHE_MAX_AGE,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    fun save(route: Route, profile: NavigationProfile = NavigationProfile.FOCUS) {
        val tmp = File(file.path + ".tmp")
        try {
            tmp.writeText(Json.encodeToString(CachedRouteFile(ROUTE_CACHE_VERSION, nowMillis(), route.toCached(), profile)))
            // renombrar es atómico: si el proceso muere a mitad, queda el archivo anterior entero
            if (!tmp.renameTo(file)) logger.warning("Could not replace $file")
        } catch (e: IOException) {
            logger.warning("Could not save route: $e")
        }
    }

    fun load(): CachedTrip? {
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
            else -> return CachedTrip(entry.route.toRoute(), entry.profile)
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
        session.drop(1).map { it.activeRoute to it.navigationProfile }.distinctUntilChanged().collect { (route, profile) ->
            if (route != null) save(route, profile) else clear()
        }
    }

    private companion object {
        val logger: Logger = Logger.getLogger("HaptiRouteCache")
    }
}

/** Lo retomado de la caché: la ruta y el perfil con el que se navegaba (paso 18). */
data class CachedTrip(val route: Route, val profile: NavigationProfile)

// El formato del archivo; la (de)serialización de la Route vive en RouteCodec (compartida con la caché del desvío).
@Serializable
private data class CachedRouteFile(val version: Int, val savedAtMillis: Long, val route: CachedRoute, val profile: NavigationProfile)
