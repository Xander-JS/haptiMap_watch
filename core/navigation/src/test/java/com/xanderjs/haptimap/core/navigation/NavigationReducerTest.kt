package com.xanderjs.haptimap.core.navigation

import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.navigation.NavigationEvent as E
import com.xanderjs.haptimap.core.navigation.NavigationSession as S
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class NavigationReducerTest {

    private val destination = Destination("Sol", GeoPoint(40.4168, -3.7038))
    private val route = Route(destination)
    private val newRoute = Route(Destination("Callao", GeoPoint(40.4199, -3.7058)))

    private val allStates = listOf(
        S.Idle,
        S.Searching,
        S.DestinationSelected(destination),
        S.RouteLoading(destination),
        S.NavigationActive(route),
        S.OffRoute(route),
        S.Recalculating(route),
        S.Arrived(route),
        S.Completed(route),
    )

    private val allEvents = listOf(
        E.StartSearch,
        E.SelectDestination(destination),
        E.RequestRoute,
        E.RouteRequestRejected(SessionError.NO_CONNECTION),
        E.RouteLoaded(route),
        E.RouteFailed(SessionError.ROUTE_UNAVAILABLE),
        E.LeaveRoute,
        E.ReturnToRoute,
        E.Recalculate,
        E.RouteRecalculated(newRoute),
        E.RecalculationFailed(SessionError.NO_CONNECTION),
        E.Arrive,
        E.Finish,
        E.Cancel,
    )

    /** Única fuente de verdad de las transiciones válidas; todo lo demás debe ignorarse. */
    private val validTransitions: Map<Pair<S, E>, S> = mapOf(
        (S.Idle to E.StartSearch) to S.Searching,
        (S.Searching to E.SelectDestination(destination)) to S.DestinationSelected(destination),
        (S.DestinationSelected(destination) to E.RequestRoute) to S.RouteLoading(destination),
        (S.DestinationSelected(destination) to E.RouteRequestRejected(SessionError.NO_CONNECTION)) to
            S.DestinationSelected(destination, SessionError.NO_CONNECTION),
        (S.RouteLoading(destination) to E.RouteLoaded(route)) to S.NavigationActive(route),
        (S.RouteLoading(destination) to E.RouteFailed(SessionError.ROUTE_UNAVAILABLE)) to
            S.DestinationSelected(destination, SessionError.ROUTE_UNAVAILABLE),
        (S.NavigationActive(route) to E.LeaveRoute) to S.OffRoute(route),
        (S.NavigationActive(route) to E.Arrive) to S.Arrived(route),
        (S.OffRoute(route) to E.ReturnToRoute) to S.NavigationActive(route),
        (S.OffRoute(route) to E.Recalculate) to S.Recalculating(route),
        (S.Recalculating(route) to E.RouteRecalculated(newRoute)) to S.NavigationActive(newRoute),
        (S.Recalculating(route) to E.RecalculationFailed(SessionError.NO_CONNECTION)) to
            S.OffRoute(route, SessionError.NO_CONNECTION),
        (S.Arrived(route) to E.Finish) to S.Completed(route),
    )

    @Test
    fun `every valid transition reaches the expected state`() {
        validTransitions.forEach { (input, expected) ->
            assertEquals("${input.first} + ${input.second}", expected, reduce(input.first, input.second))
        }
    }

    @Test
    fun `cancel returns to Idle from every state`() {
        allStates.forEach { assertEquals("$it + Cancel", S.Idle, reduce(it, E.Cancel)) }
    }

    @Test
    fun `retrying after a route failure loads the route again`() {
        val failed = reduce(S.RouteLoading(destination), E.RouteFailed(SessionError.NO_CONNECTION))
        assertEquals(S.RouteLoading(destination), reduce(failed, E.RequestRoute))
    }

    @Test
    fun `a rejected request keeps the destination and can be retried`() {
        val rejected = reduce(S.DestinationSelected(destination), E.RouteRequestRejected(SessionError.NO_CONNECTION))
        assertEquals(S.DestinationSelected(destination, SessionError.NO_CONNECTION), rejected)
        assertEquals(S.RouteLoading(destination), reduce(rejected, E.RequestRoute))
    }

    @Test
    fun `recalculating again after a recalculation failure is allowed`() {
        val failed = reduce(S.Recalculating(route), E.RecalculationFailed(SessionError.NO_CONNECTION))
        assertEquals(S.Recalculating(route), reduce(failed, E.Recalculate))
    }

    @Test
    fun `every other event leaves the state untouched`() {
        for (state in allStates) for (event in allEvents) {
            if (event == E.Cancel || (state to event) in validTransitions) continue
            assertSame("$state + $event", state, reduce(state, event))
        }
    }

    @Test
    fun `full trip from Idle to Completed`() {
        val trip = listOf(
            E.StartSearch, E.SelectDestination(destination), E.RequestRoute, E.RouteLoaded(route),
            E.LeaveRoute, E.Recalculate, E.RouteRecalculated(newRoute), E.Arrive, E.Finish,
        )
        assertEquals(S.Completed(newRoute), trip.fold(S.Idle as S, ::reduce))
    }
}
