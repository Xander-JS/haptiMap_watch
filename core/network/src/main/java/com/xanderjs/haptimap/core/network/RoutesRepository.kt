package com.xanderjs.haptimap.core.network

import com.xanderjs.haptimap.core.model.ConnectivityPolicy
import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.PlaceSearchError
import com.xanderjs.haptimap.core.model.RouteError
import com.xanderjs.haptimap.core.model.RouteResult
import java.io.IOException
import java.util.logging.Logger
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

/** Ruta a pie. [origin] null (sin posición todavía) devuelve NO_LOCATION. */
interface RoutesRepository {
    /** false si la app se compiló sin clave: quien pide la ruta lo comprueba antes de esperar al GPS. */
    val hasApiKey: Boolean

    /** [alternatives] pide rutas alternativas a Google (paso 21); por ahora siempre se usa la primera. */
    suspend fun computeRoute(origin: GeoPoint?, destination: Place, alternatives: Boolean = false): RouteResult
}

/**
 * Routes API por REST. Sin clave ([hasApiKey] false) o sin red no se hace la petición.
 * [languageCode] se lee en cada petición, como en [GooglePlacesRepository].
 */
class GoogleRoutesRepository(
    private val api: RoutesApi,
    private val connectivity: ConnectivityMonitor,
    private val languageCode: () -> String,
    override val hasApiKey: Boolean,
    // cuenta para el presupuesto compartido y el contador de depuración (paso 17); una ruta no se bloquea
    private val budget: ApiCallBudget = ApiCallBudget.UNLIMITED,
) : RoutesRepository {

    override suspend fun computeRoute(origin: GeoPoint?, destination: Place, alternatives: Boolean): RouteResult {
        if (!hasApiKey) return RouteResult.Failure(RouteError.MISSING_KEY).logged()
        // sin red no se hace la petición (sección 17)
        if (!ConnectivityPolicy.canRequestRoute(connectivity.status.value)) return RouteResult.Failure(RouteError.NO_CONNECTION).logged()
        if (origin == null) return RouteResult.Failure(RouteError.NO_LOCATION).logged()
        logger.info("Request $origin -> ${destination.toWaypoint()}, language ${languageCode()}")
        budget.record(ApiKind.ROUTES)
        val response = try {
            api.computeRoutes(
                RoutesRequest(
                    origin = Waypoint(LocationDto(LatLng(origin.latitude, origin.longitude))),
                    destination = destination.toWaypoint(),
                    languageCode = languageCode(),
                    computeAlternativeRoutes = alternatives,
                ),
            )
        } catch (e: HttpException) {
            return RouteResult.Failure(routeErrorFor(e.code(), e.response()?.errorBody()?.string())).logged()
        } catch (e: IOException) {
            // el monitor aún no ha visto la caída (periodo de gracia) o la red falló a mitad
            return RouteResult.Failure(RouteError.NO_CONNECTION).logged()
        } catch (e: SerializationException) {
            return RouteResult.Failure(RouteError.API_ERROR).logged()
        }
        val route = response.toRoute(destination, origin) ?: return RouteResult.Failure(RouteError.NO_ROUTE).logged()
        return RouteResult.Success(route).logged()
    }

    private fun RouteResult.logged() = also {
        logger.info(if (it is RouteResult.Success) "Route ${it.route.distanceMeters} m, ${it.route.duration}, ${it.route.steps.map { s -> s.maneuver }}" else "$it")
    }

    private companion object {
        val logger: Logger = Logger.getLogger("HaptiRoutes")
    }
}

// mismo proyecto y misma clave que Places: los códigos de error significan lo mismo
internal fun routeErrorFor(httpCode: Int, errorBody: String?): RouteError = when (placesErrorFor(httpCode, errorBody)) {
    PlaceSearchError.API_QUOTA -> RouteError.API_QUOTA
    PlaceSearchError.API_INVALID_KEY -> RouteError.API_INVALID_KEY
    else -> RouteError.API_ERROR
}
