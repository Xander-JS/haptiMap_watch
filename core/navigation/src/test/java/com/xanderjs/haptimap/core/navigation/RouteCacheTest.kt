package com.xanderjs.haptimap.core.navigation

import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.Maneuver
import com.xanderjs.haptimap.core.model.NavigationProfile
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.PlaceCategory
import com.xanderjs.haptimap.core.model.Route
import com.xanderjs.haptimap.core.model.RouteStep
import com.xanderjs.haptimap.core.navigation.NavigationEvent as E
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class RouteCacheTest {

    @get:Rule
    val folder = TemporaryFolder()

    private var now = 1_000_000L
    private val file by lazy { folder.root.resolve("active_route.json") }
    private val cache by lazy { RouteCache(file, maxAge = 6.hours, nowMillis = { now }) }

    private val sol = GeoPoint(40.41678, -3.70379)
    private val prado = Place("ChIJ-prado", "Museo del Prado", GeoPoint(40.4137818, -3.6921271), PlaceCategory.STORE, "Calle de Ruiz de Alarcón 23", 1100)

    /** Un paso por cada Maneuver, el último la llegada de 0 m; nulos donde el dominio los admite. */
    private val route = Route(
        destination = prado,
        distanceMeters = 1089,
        duration = 14.minutes + 30.seconds,
        points = listOf(sol, GeoPoint(40.41546, -3.69928), GeoPoint(40.41378, -3.69213)),
        steps = listOf(
            RouteStep(Maneuver.CONTINUE, 120, 90.seconds, listOf(sol, GeoPoint(40.41611, -3.70253)), sol, GeoPoint(40.41611, -3.70253), null, "DEPART"),
            RouteStep(Maneuver.TURN_LEFT, 500, 6.minutes, listOf(GeoPoint(40.41611, -3.70253), GeoPoint(40.41546, -3.69928)), GeoPoint(40.41611, -3.70253), GeoPoint(40.41546, -3.69928), "Gira a la izquierda", "TURN_SHARP_LEFT"),
            RouteStep(Maneuver.TURN_RIGHT, 469, 1234.milliseconds, listOf(GeoPoint(40.41546, -3.69928), GeoPoint(40.41378, -3.69213)), GeoPoint(40.41546, -3.69928), GeoPoint(40.41378, -3.69213), "Gira a la derecha", null),
            RouteStep(Maneuver.ARRIVAL, 0, 0.seconds, emptyList(), GeoPoint(40.41378, -3.69213), GeoPoint(40.41378, -3.69213), null, null),
        ),
    )
    private val otherRoute = route.copy(destination = prado.copy(id = "sol", name = "Sol", category = null, address = null))

    @Test
    fun `a route survives the round trip, every maneuver and the arrival step included`() {
        cache.save(route)
        assertEquals(route, cache.load()!!.route)
        assertEquals(Maneuver.entries, cache.load()!!.route.steps.map { it.maneuver }.distinct().sortedBy { it.ordinal })
        assertEquals(Maneuver.ARRIVAL, cache.load()!!.route.steps.last().maneuver)
        assertEquals(otherRoute, cache.apply { save(otherRoute) }.load()!!.route)
    }

    @Test
    fun `the navigation profile is saved with the route and restored with it`() {
        cache.save(route, NavigationProfile.TOUR)
        assertEquals(NavigationProfile.TOUR, cache.load()!!.profile)
        cache.save(route)
        assertEquals(NavigationProfile.FOCUS, cache.load()!!.profile)
    }

    @Test
    fun `following the session persists the trip profile so a resumed trip keeps it`() = withFollowingStore { store ->
        store.dispatch(E.StartSearch)
        store.dispatch(E.SelectDestination(route.destination))
        store.dispatch(E.ChooseProfile)
        store.dispatch(E.SelectProfile(NavigationProfile.TOUR))
        store.dispatch(E.RouteLoaded(route))
        assertEquals(NavigationProfile.TOUR, cache.load()!!.profile)
    }

    @Test
    fun `polylines are stored encoded, not as decoded points`() {
        cache.save(route)
        val text = file.readText()
        assertTrue(text, "\"polyline\":\"{{tuFtkrU" in text)
        // solo el destino y el inicio/fin de cada paso van como coordenadas sueltas
        assertEquals(1 + 2 * route.steps.size, "\"latitude\"".toRegex().findAll(text).count())
    }

    @Test
    fun `no file is no cached route`() {
        assertNull(cache.load())
    }

    @Test
    fun `corrupted or truncated content is no cached route and is deleted`() {
        file.writeText("not json")
        assertNull(cache.load())
        assertFalse(file.exists())

        cache.save(route)
        file.writeText(file.readText().dropLast(20))
        assertNull(cache.load())
    }

    @Test
    fun `another schema version is no cached route`() {
        cache.save(route)
        file.writeText(file.readText().replace("\"version\":$ROUTE_CACHE_VERSION", "\"version\":${ROUTE_CACHE_VERSION + 1}"))
        assertNull(cache.load())
        assertFalse(file.exists())
    }

    @Test
    fun `an entry older than the maximum age is no cached route`() {
        cache.save(route)
        now += 6.hours.inWholeMilliseconds
        assertEquals(route, cache.load()!!.route)
        now += 1
        assertNull(cache.load())
        assertFalse(file.exists())
    }

    @Test
    fun `a route without steps is discarded`() {
        cache.save(route.copy(steps = emptyList()))
        assertNull(cache.load())
    }

    // Ciclo de vida: la caché sigue las transiciones de la sesión

    private fun withFollowingStore(block: (NavigationSessionStore) -> Unit) = runTest(UnconfinedTestDispatcher()) {
        val store = NavigationSessionStore()
        backgroundScope.launch { cache.follow(store.state) }
        block(store)
    }

    private fun NavigationSessionStore.loadRoute(route: Route) {
        dispatch(E.StartSearch)
        dispatch(E.SelectDestination(route.destination))
        dispatch(E.RequestRoute)
        dispatch(E.RouteLoaded(route))
    }

    @Test
    fun `RouteReady saves the route and it stays while navigating`() = withFollowingStore { store ->
        store.loadRoute(route)
        assertEquals(route, cache.load()!!.route)
        store.dispatch(E.StartNavigation)
        store.dispatch(E.LeaveRoute)
        assertEquals(route, cache.load()!!.route)
    }

    @Test
    fun `a recalculated route replaces the saved one`() = withFollowingStore { store ->
        store.loadRoute(route)
        listOf(E.StartNavigation, E.LeaveRoute, E.Recalculate, E.RouteRecalculated(otherRoute)).forEach(store::dispatch)
        assertEquals(otherRoute, cache.load()!!.route)
    }

    @Test
    fun `arriving and completing clears the cache`() = withFollowingStore { store ->
        store.loadRoute(route)
        listOf(E.StartNavigation, E.Arrive, E.Finish).forEach(store::dispatch)
        assertFalse(file.exists())
    }

    @Test
    fun `cancel clears the cache`() = withFollowingStore { store ->
        store.loadRoute(route)
        store.dispatch(E.StartNavigation)
        store.dispatch(E.Cancel)
        assertFalse(file.exists())
    }

    @Test
    fun `a new destination replaces the saved route`() = withFollowingStore { store ->
        store.loadRoute(route)
        // elegir otro lugar vuelve primero a Searching (PlacesDebugViewModel.selectPlace)
        store.dispatch(E.Cancel)
        store.dispatch(E.StartSearch)
        assertFalse(file.exists())
        store.dispatch(E.SelectDestination(otherRoute.destination))
        store.dispatch(E.RequestRoute)
        store.dispatch(E.RouteLoaded(otherRoute))
        assertEquals(otherRoute, cache.load()!!.route)
    }

    @Test
    fun `the initial Idle keeps the saved route so it can be resumed`() {
        cache.save(route)
        withFollowingStore { store ->
            assertEquals(route, cache.load()!!.route)
            store.dispatch(E.RestoreRoute(route))
            assertEquals(NavigationSession.RouteReady(route), store.state.value)
            assertEquals(route, cache.load()!!.route)
        }
    }
}
