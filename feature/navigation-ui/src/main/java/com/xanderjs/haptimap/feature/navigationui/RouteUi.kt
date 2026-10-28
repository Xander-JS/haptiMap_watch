package com.xanderjs.haptimap.feature.navigationui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.xanderjs.haptimap.core.model.Maneuver
import com.xanderjs.haptimap.core.model.Route
import com.xanderjs.haptimap.core.model.RouteError
import com.xanderjs.haptimap.core.model.firstManeuver
import com.xanderjs.haptimap.core.navigation.NavigationProgress
import com.xanderjs.haptimap.designsystem.atoms.ArrowDirection
import com.xanderjs.haptimap.designsystem.atoms.NoticeLabel
import com.xanderjs.haptimap.designsystem.organisms.RouteSummaryData
import com.xanderjs.haptimap.designsystem.organisms.StatusMessage
import com.xanderjs.haptimap.designsystem.atoms.StatusTone
import kotlin.math.roundToInt
import kotlin.time.Duration

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

/** Con una ruta cargada y sin red: la ruta está guardada y sigue funcionando. Warning, como el aviso de la hora. */
@Composable
fun RouteOfflineNotice(modifier: Modifier = Modifier) =
    NoticeLabel(stringResource(R.string.route_offline_notice), StatusTone.Warning, modifier)

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

internal val Maneuver.arrow: ArrowDirection
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

/** Lo que muestra la pantalla de navegación (paso 07), en valores del sistema de diseño. */
data class GuidanceUi(
    val direction: ArrowDirection,
    val distanceMeters: Int,
    val instruction: String?,
    val remainingMeters: Int,
    val remainingTime: Duration,
)

/** La llegada no trae instrucción del proveedor: se muestra el destino. */
fun NavigationProgress.toGuidance(destinationName: String) = GuidanceUi(
    direction = nextManeuver.maneuver.arrow,
    distanceMeters = distanceToManeuverMeters.roundToInt(),
    instruction = nextManeuver.instruction ?: destinationName.takeIf { nextManeuver.maneuver == Maneuver.ARRIVAL },
    remainingMeters = remainingMeters.roundToInt(),
    remainingTime = remainingTime,
)

/** Un solo aviso a la vez en la pantalla de navegación. */
enum class NavigationNotice { WEAK_GPS, OFFLINE }

/**
 * Señal débil gana a sin red: afecta a la guía ahora mismo, y sin red la ruta guardada sigue
 * funcionando. Sin red ya lo dice la hora ("Sin conexión"), así que no se pierde.
 */
fun navigationNotice(weakSignal: Boolean, offline: Boolean): NavigationNotice? = when {
    weakSignal -> NavigationNotice.WEAK_GPS
    offline -> NavigationNotice.OFFLINE
    else -> null
}

@Composable
fun NavigationNoticeLabel(notice: NavigationNotice, modifier: Modifier = Modifier) = when (notice) {
    NavigationNotice.OFFLINE -> RouteOfflineNotice(modifier)
    NavigationNotice.WEAK_GPS -> NoticeLabel(stringResource(R.string.navigation_weak_gps), StatusTone.Warning, modifier)
}
