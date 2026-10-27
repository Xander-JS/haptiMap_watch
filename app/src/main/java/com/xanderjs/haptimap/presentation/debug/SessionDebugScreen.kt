package com.xanderjs.haptimap.presentation.debug

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Text
import com.xanderjs.haptimap.R
import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.navigation.Destination
import com.xanderjs.haptimap.core.navigation.NavigationEvent
import com.xanderjs.haptimap.core.navigation.NavigationSession
import com.xanderjs.haptimap.core.navigation.SessionError
import com.xanderjs.haptimap.designsystem.atoms.HaptiButton
import com.xanderjs.haptimap.designsystem.atoms.HaptiButtonVariant
import com.xanderjs.haptimap.designsystem.atoms.HaptiTimeText
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.templates.RoundScreenTemplate
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/** Pantalla temporal (paso 02): muestra el estado de la sesión y dispara eventos para ver las transiciones. */
@Composable
fun SessionDebugScreen(viewModel: SessionDebugViewModel = hiltViewModel()) {
    val session by viewModel.uiState.collectAsStateWithLifecycle()
    SessionDebugContent(session, viewModel.events, viewModel::onEvent)
}

@Composable
private fun SessionDebugContent(session: NavigationSession, events: List<NavigationEvent>, onEvent: (NavigationEvent) -> Unit) {
    val colors = HaptiMapTheme.colors
    val error = when (session) {
        is NavigationSession.DestinationSelected -> session.error
        is NavigationSession.OffRoute -> session.error
        else -> null
    }
    RoundScreenTemplate {
        item { Text(stringResource(R.string.session_debug_title), style = HaptiMapTheme.typography.title, color = colors.primary) }
        item { Text(stringResource(session.label), style = HaptiMapTheme.typography.body, color = colors.onBackground) }
        if (error != null) {
            item {
                Text(stringResource(error.label), style = HaptiMapTheme.typography.caption, color = colors.error, textAlign = TextAlign.Center)
            }
        }
        events.forEach { event ->
            item {
                HaptiButton(
                    text = stringResource(event.label),
                    onClick = { onEvent(event) },
                    modifier = Modifier.fillMaxWidth(),
                    variant = HaptiButtonVariant.Secondary,
                )
            }
        }
    }
}

@get:StringRes
private val NavigationSession.label: Int
    get() = when (this) {
        NavigationSession.Idle -> R.string.session_idle
        NavigationSession.Searching -> R.string.session_searching
        is NavigationSession.DestinationSelected -> R.string.session_destination_selected
        is NavigationSession.RouteLoading -> R.string.session_route_loading
        is NavigationSession.NavigationActive -> R.string.session_navigation_active
        is NavigationSession.OffRoute -> R.string.session_off_route
        is NavigationSession.Recalculating -> R.string.session_recalculating
        is NavigationSession.Arrived -> R.string.session_arrived
        is NavigationSession.Completed -> R.string.session_completed
    }

@get:StringRes
private val SessionError.label: Int
    get() = when (this) {
        SessionError.ROUTE_UNAVAILABLE -> R.string.session_error_route_unavailable
        SessionError.NO_CONNECTION -> R.string.session_error_no_connection
    }

@get:StringRes
private val NavigationEvent.label: Int
    get() = when (this) {
        NavigationEvent.StartSearch -> R.string.event_start_search
        is NavigationEvent.SelectDestination -> R.string.event_select_destination
        NavigationEvent.RequestRoute -> R.string.event_request_route
        is NavigationEvent.RouteLoaded -> R.string.event_route_loaded
        is NavigationEvent.RouteFailed -> R.string.event_route_failed
        NavigationEvent.LeaveRoute -> R.string.event_leave_route
        NavigationEvent.ReturnToRoute -> R.string.event_return_to_route
        NavigationEvent.Recalculate -> R.string.event_recalculate
        is NavigationEvent.RouteRecalculated -> R.string.event_route_recalculated
        is NavigationEvent.RecalculationFailed -> R.string.event_recalculation_failed
        NavigationEvent.Arrive -> R.string.event_arrive
        NavigationEvent.Finish -> R.string.event_finish
        NavigationEvent.Cancel -> R.string.event_cancel
    }

@HaptiPreviews
@Composable
private fun SessionDebugPreview() = HaptiMapTheme {
    val destination = Destination(name = "", point = GeoPoint(0.0, 0.0))
    AppScaffold(timeText = { HaptiTimeText() }) {
        SessionDebugContent(
            session = NavigationSession.DestinationSelected(destination, SessionError.ROUTE_UNAVAILABLE),
            events = listOf(NavigationEvent.RequestRoute, NavigationEvent.Cancel),
            onEvent = {},
        )
    }
}
