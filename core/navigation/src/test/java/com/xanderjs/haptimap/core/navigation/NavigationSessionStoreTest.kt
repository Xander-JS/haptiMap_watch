package com.xanderjs.haptimap.core.navigation

import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.Place
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationSessionStoreTest {

    private val destination = Place("sol", "Sol", GeoPoint(40.4168, -3.7038), null, null, 0)

    @Test
    fun `starts Idle and exposes every dispatched transition`() = runTest {
        val store = NavigationSessionStore()
        val collected = mutableListOf<NavigationSession>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { store.state.take(3).toList(collected) }

        store.dispatch(NavigationEvent.StartSearch)
        store.dispatch(NavigationEvent.SelectDestination(destination))
        job.join()

        assertEquals(
            listOf(NavigationSession.Idle, NavigationSession.Searching, NavigationSession.DestinationSelected(destination)),
            collected,
        )
    }

    @Test
    fun `an invalid event does not change the state`() {
        val store = NavigationSessionStore()
        store.dispatch(NavigationEvent.Arrive)
        assertEquals(NavigationSession.Idle, store.state.value)
    }
}
