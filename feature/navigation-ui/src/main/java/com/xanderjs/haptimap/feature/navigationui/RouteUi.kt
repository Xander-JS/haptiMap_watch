package com.xanderjs.haptimap.feature.navigationui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.xanderjs.haptimap.core.model.Maneuver
import com.xanderjs.haptimap.core.model.Route
import com.xanderjs.haptimap.core.model.RouteError
import com.xanderjs.haptimap.core.model.firstManeuver
import com.xanderjs.haptimap.designsystem.atoms.ArrowDirection
import com.xanderjs.haptimap.designsystem.organisms.RouteSummaryData
import com.xanderjs.haptimap.designsystem.organisms.StatusMessage
import com.xanderjs.haptimap.designsystem.organisms.StatusTone

// Traducción del dominio de rutas a valores del sistema de diseño: el único sitio que la conoce.

/** Lo que muestra la petición de una ruta mientras no hay ruta: cargando, o por qué no la hay. */
sealed interface RouteStatus {
    data object Loading : RouteStatus
    data class Failed(val error: RouteError) : RouteStatus
}

@Composable
fun RouteStatusMessage(status: RouteStatus, modifier: Modifier = Modifier) = when (status) {
    RouteStatus.Loading -> StatusMessage(stringResource(R.string.route_loading), modifier, loading = true)
    is RouteStatus.Failed -> StatusMessage(stringResource(status.error.message), modifier, tone = status.error.tone)
}

@get:StringRes
private val RouteError.message: Int
    get() = when (this) {
        RouteError.NO_CONNECTION -> R.string.route_error_no_connection
        RouteError.NO_LOCATION -> R.string.route_error_no_location
        RouteError.NO_ROUTE -> R.string.route_error_no_route
        RouteError.MISSING_KEY -> R.string.route_error_missing_key
        RouteError.API_QUOTA -> R.string.route_error_quota
        RouteError.API_INVALID_KEY -> R.string.route_error_invalid_key
        RouteError.API_ERROR -> R.string.route_error_api
    }

// sin ruta no es un fallo del servicio; sin red ni ubicación se puede esperar; lo demás es error
private val RouteError.tone: StatusTone
    get() = when (this) {
        RouteError.NO_ROUTE -> StatusTone.Muted
        RouteError.NO_CONNECTION, RouteError.NO_LOCATION -> StatusTone.Warning
        else -> StatusTone.Error
    }

private val Maneuver.arrow: ArrowDirection
    get() = when (this) {
        Maneuver.TURN_LEFT -> ArrowDirection.LEFT
        Maneuver.TURN_RIGHT -> ArrowDirection.RIGHT
        Maneuver.CONTINUE -> ArrowDirection.STRAIGHT
        Maneuver.ARRIVAL -> ArrowDirection.ARRIVE
    }

fun Route.toSummary(): RouteSummaryData {
    val next = firstManeuver()
    return RouteSummaryData(distanceMeters, duration, next.step.maneuver.arrow, next.distanceMeters, next.step.instruction)
}
