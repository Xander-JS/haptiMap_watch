package com.xanderjs.haptimap.feature.navigationui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.xanderjs.haptimap.designsystem.atoms.ArrowDirection
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import com.xanderjs.haptimap.designsystem.R as DsR

/**
 * La notificación y la Ongoing Activity del servicio en primer plano (paso 10), en valores de UI:
 * los mismos iconos y textos que la pantalla de navegación. El servicio solo resuelve los recursos.
 */
data class NavigationNotificationContent(
    @param:DrawableRes val icon: Int,
    @param:StringRes val title: Int,
    val detail: NotificationDetail?,
)

sealed interface NotificationDetail {
    /** "120 m" hasta la maniobra. */
    data class Distance(val meters: Int) : NotificationDetail

    /** "40 m from the route" (navigation_from_route). */
    data class FromRoute(val meters: Int) : NotificationDetail

    /** El nombre del destino, tras llegar. */
    data class Destination(val name: String) : NotificationDetail
}

/** Null cuando ya no se navega: el servicio está parando y no hay nada que publicar. */
fun NavigationUiState.toNotificationContent(): NavigationNotificationContent? = when (this) {
    is NavigationUiState.Navigating -> NavigationNotificationContent(
        guidance.direction.icon,
        guidance.direction.description,
        NotificationDetail.Distance(guidance.distanceMeters),
    )
    is NavigationUiState.OffRoute -> NavigationNotificationContent(
        DsR.drawable.ic_off_route,
        status.message,
        metersFromRoute?.let(NotificationDetail::FromRoute),
    )
    is NavigationUiState.Arrived -> NavigationNotificationContent(
        ArrowDirection.ARRIVE.icon,
        R.string.navigation_arrived,
        NotificationDetail.Destination(destinationName),
    )
    NavigationUiState.Ended -> null
}

/** El mismo texto que [OffRouteStatusMessage]. */
@get:StringRes
val OffRouteStatus.message: Int
    get() = when (this) {
        OffRouteStatus.RECALCULATING -> R.string.navigation_rerouting
        OffRouteStatus.OFFLINE -> R.string.navigation_reroute_offline
        OffRouteStatus.FAILED -> R.string.navigation_reroute_failed
        OffRouteStatus.HEAD_BACK -> R.string.navigation_head_back
    }

/** Cada cuánto, como mucho, se reconstruye la notificación cuando solo cambia la distancia. */
val NOTIFICATION_DISTANCE_INTERVAL = 5.seconds

/**
 * Decide qué contenido se publica, para no reconstruir la notificación en cada fix (uno por segundo):
 * un cambio de maniobra, estado o destino sale al momento; si solo cambia la distancia, como mucho cada
 * [interval]; lo idéntico nunca. shortcut: una distancia retenida no sale hasta el siguiente cambio, así que
 * parado puede quedar hasta [interval] atrasada; si molesta, publicar la pendiente con un temporizador.
 */
class NotificationThrottle(private val interval: Duration = NOTIFICATION_DISTANCE_INTERVAL) {
    private var posted: NavigationNotificationContent? = null
    private var postedAtMillis = 0L

    fun shouldPost(content: NavigationNotificationContent, nowMillis: Long): Boolean {
        val last = posted
        val post = when {
            last == content -> false
            last == null || last.headline != content.headline -> true
            else -> nowMillis - postedAtMillis >= interval.inWholeMilliseconds
        }
        if (post) {
            posted = content
            postedAtMillis = nowMillis
        }
        return post
    }

    private val NavigationNotificationContent.headline
        get() = copy(
            detail = when (detail) {
                is NotificationDetail.Distance -> NotificationDetail.Distance(0)
                is NotificationDetail.FromRoute -> NotificationDetail.FromRoute(0)
                else -> detail
            },
        )
}
