package com.xanderjs.haptimap.core.network

import com.xanderjs.haptimap.core.model.ConnectivityPolicy
import com.xanderjs.haptimap.core.model.ExplorationCategory
import com.xanderjs.haptimap.core.model.GeoPoint
import java.io.IOException
import java.util.logging.Logger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

/** Tipos base (Tabla A de Places) de todas las categorías de exploración: el filtro `includedTypes` del Tour. */
val EXPLORATION_PLACE_TYPES: List<String> = ExplorationCategory.entries.flatMap { it.placeTypes }.distinct()

/** Por qué falló una consulta de POIs. */
enum class PoiSourceError { QUOTA, HTTP, TIMEOUT, API_ERROR }

/**
 * Resultado de pedir POIs cerca de un punto (paso 17). Estado interno claro para la tubería del Tour: cada caso
 * se trata distinto (sugerir, esperar red, parar por presupuesto, reintentar más tarde).
 */
sealed interface PoiFetch {
    data class Found(val places: List<PlaceDto>) : PoiFetch
    data object Empty : PoiFetch

    /** Sin conexión: se suspende la consulta sin tocar la navegación; se reintenta al volver la red. */
    data object Offline : PoiFetch

    /** Presupuesto agotado (paso 17, coste): se deja de consultar en silencio. */
    data object BudgetReached : PoiFetch
    data class Failed(val error: PoiSourceError) : PoiFetch
}

/**
 * Fuente de POIs del Modo Tour (paso 17). Busca lugares de exploración alrededor de un punto de la ruta por
 * delante del usuario; la tubería decide el punto y el radio. Sin datos falsos: solo Google Places (New).
 */
interface PoiSource {
    suspend fun searchAhead(center: GeoPoint, radiusMeters: Double, languageCode: String): PoiFetch
}

/**
 * Nearby Search de Places (New) para los POIs del Tour (paso 17). Pide solo [EXPLORATION_PLACE_TYPES] con la
 * máscara mínima [POI_FIELD_MASK] (SKU Pro), ordenado por distancia. Controles de coste y robustez:
 * no consulta sin red ni sin presupuesto; cuenta cada consulta en [budget]; un único reintento con espera ante
 * un fallo de red, nunca ante 4xx o cuota. Los nombres llegan en [languageCode] (el idioma de la app).
 */
class GooglePoiSource(
    private val api: PlacesApi,
    private val connectivity: ConnectivityMonitor,
    private val budget: ApiCallBudget,
    private val maxResults: Int = POI_MAX_RESULTS,
    private val retryBackoff: Duration = POI_RETRY_BACKOFF,
) : PoiSource {

    override suspend fun searchAhead(center: GeoPoint, radiusMeters: Double, languageCode: String): PoiFetch {
        if (!ConnectivityPolicy.canFetchPois(connectivity.status.value)) return PoiFetch.Offline.logged(center)
        if (!budget.canQuery()) return PoiFetch.BudgetReached.logged(center)

        val request = NearbyRequest(
            includedTypes = EXPLORATION_PLACE_TYPES,
            locationRestriction = Area(Circle(LatLng(center.latitude, center.longitude), radiusMeters)),
            languageCode = languageCode,
            maxResultCount = maxResults,
        )
        // la consulta cuenta aunque falle (control de coste: nunca se insiste más allá del presupuesto)
        budget.record(ApiKind.PLACES)
        logger.info("Query @ $center r=${radiusMeters}m lang=$languageCode | ${budget.debugSummary()}")
        return try {
            val places = requestWithOneRetry(request)
            (if (places.isEmpty()) PoiFetch.Empty else PoiFetch.Found(places)).logged(center)
        } catch (e: HttpException) {
            // nunca se reintenta un 4xx ni una cuota
            PoiFetch.Failed(if (e.code() == 429) PoiSourceError.QUOTA else PoiSourceError.HTTP).logged(center)
        } catch (e: IOException) {
            PoiFetch.Failed(PoiSourceError.TIMEOUT).logged(center)
        } catch (e: SerializationException) {
            PoiFetch.Failed(PoiSourceError.API_ERROR).logged(center)
        }
    }

    // un solo reintento con espera ante un fallo de red (IOException); los HttpException se dejan pasar sin reintentar
    private suspend fun requestWithOneRetry(request: NearbyRequest): List<PlaceDto> = try {
        api.searchNearbyPois(request).places
    } catch (e: IOException) {
        delay(retryBackoff)
        api.searchNearbyPois(request).places
    }

    private fun PoiFetch.logged(center: GeoPoint): PoiFetch = also {
        val detail = when (it) {
            is PoiFetch.Found -> "${it.places.size} places: ${it.places.map { p -> p.displayName?.text to p.primaryType }}"
            else -> it::class.simpleName
        }
        logger.info("Result @ $center: $detail")
    }

    private companion object {
        val logger: Logger = Logger.getLogger("HaptiPois")
    }
}

/** Pocos POIs por consulta: un paseo no necesita más y limita el coste. */
const val POI_MAX_RESULTS = 10

/** Espera del único reintento ante un fallo de red. */
val POI_RETRY_BACKOFF: Duration = 800.milliseconds
