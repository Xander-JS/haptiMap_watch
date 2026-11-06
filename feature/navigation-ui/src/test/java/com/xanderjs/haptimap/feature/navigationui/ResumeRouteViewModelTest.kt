package com.xanderjs.haptimap.feature.navigationui

import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.Maneuver
import com.xanderjs.haptimap.core.model.NavigationProfile
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.model.Route
import com.xanderjs.haptimap.core.model.RouteStep
import com.xanderjs.haptimap.core.model.SLOW_LOAD_THRESHOLD
import com.xanderjs.haptimap.core.navigation.NavigationEvent
import com.xanderjs.haptimap.core.navigation.NavigationSession
import com.xanderjs.haptimap.core.navigation.NavigationSessionStore
import com.xanderjs.haptimap.core.navigation.RouteCache
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class ResumeRouteViewModelTest {

    @get:Rule
    val folder = TemporaryFolder()

    private var now = 0L
    private val file by lazy { folder.root.resolve("active_route.json") }
    private val cache by lazy { RouteCache(file, nowMillis = { now }) }
    private val store = NavigationSessionStore()
    // la lectura de la caché corre aquí: hasta advanceUntilIdle() no termina
    private val io = StandardTestDispatcher()
    // el main va en otro planificador: su reloj (el retraso del indicador) avanza sin que la lectura termine
    private val main = UnconfinedTestDispatcher()

    private val prado = Place("prado", "Museo del Prado", GeoPoint(40.4138, -3.6921), null, null, 1000)
    private val arrival = GeoPoint(40.4138, -3.6921)
    private val route = Route(prado, 1089, Duration.ZERO, emptyList(), listOf(RouteStep(Maneuver.ARRIVAL, 0, Duration.ZERO, emptyList(), arrival, arrival, null, null)))

    @Before
    fun setUp() = Dispatchers.setMain(main)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun loadedViewModel() = ResumeRouteViewModel(store, cache, io).also { io.scheduler.advanceUntilIdle() }

    @Test
    fun `a fast cache read goes straight to the route without showing loading`() {
        cache.save(route)
        val viewModel = ResumeRouteViewModel(store, cache, io)
        assertEquals(ResumeRouteUiState.Pending, viewModel.uiState.value)
        io.scheduler.advanceUntilIdle()
        assertEquals(ResumeRouteUiState.Ready(route), viewModel.uiState.value)
        main.scheduler.advanceTimeBy(SLOW_LOAD_THRESHOLD * 2)
        assertEquals(ResumeRouteUiState.Ready(route), viewModel.uiState.value)
    }

    @Test
    fun `a slow cache read shows loading after the threshold, then the route`() {
        cache.save(route)
        val viewModel = ResumeRouteViewModel(store, cache, io)
        main.scheduler.advanceTimeBy(SLOW_LOAD_THRESHOLD - 1.milliseconds)
        main.scheduler.runCurrent()
        assertEquals(ResumeRouteUiState.Pending, viewModel.uiState.value)
        main.scheduler.advanceTimeBy(2.milliseconds)
        assertEquals(ResumeRouteUiState.Loading, viewModel.uiState.value)
        io.scheduler.advanceUntilIdle()
        assertEquals(ResumeRouteUiState.Ready(route), viewModel.uiState.value)
    }

    @Test
    fun `resume restores the cached route as RouteReady`() {
        cache.save(route)
        loadedViewModel().resume()
        assertEquals(NavigationSession.RouteReady(route), store.state.value)
    }

    @Test
    fun `resume keeps the profile the trip was cached with`() {
        cache.save(route, NavigationProfile.TOUR)
        loadedViewModel().resume()
        assertEquals(NavigationSession.RouteReady(route, NavigationProfile.TOUR), store.state.value)
    }

    @Test
    fun `discard clears the cache and leaves the session Idle`() {
        cache.save(route)
        loadedViewModel().discard()
        assertFalse(file.exists())
        assertEquals(NavigationSession.Idle, store.state.value)
    }

    @Test
    fun `no cached route goes on without offering anything`() {
        assertEquals(ResumeRouteUiState.NoRoute, loadedViewModel().uiState.value)
    }

    @Test
    fun `an expired route offers nothing and resume does nothing`() {
        cache.save(route)
        now += 7.hours.inWholeMilliseconds
        val viewModel = loadedViewModel()
        assertEquals(ResumeRouteUiState.NoRoute, viewModel.uiState.value)
        viewModel.resume()
        assertEquals(NavigationSession.Idle, store.state.value)
    }

    @Test
    fun `with the session already past Idle there is nothing to offer`() {
        cache.save(route)
        store.dispatch(NavigationEvent.StartSearch)
        assertEquals(ResumeRouteUiState.NoRoute, loadedViewModel().uiState.value)
    }
}
