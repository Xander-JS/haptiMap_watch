package com.xanderjs.haptimap.feature.navigationui

import com.xanderjs.haptimap.core.haptics.HapticEngine
import com.xanderjs.haptimap.core.haptics.HapticPlayer
import com.xanderjs.haptimap.core.model.GeoPoint
import com.xanderjs.haptimap.core.model.HapticEvent
import com.xanderjs.haptimap.core.model.HapticPattern
import com.xanderjs.haptimap.core.model.NavigationProfile
import com.xanderjs.haptimap.core.model.Place
import com.xanderjs.haptimap.core.navigation.NavigationEvent
import com.xanderjs.haptimap.core.navigation.NavigationSession
import com.xanderjs.haptimap.core.navigation.NavigationSessionStore
import com.xanderjs.haptimap.core.navigation.ProfilePreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileSelectorViewModelTest {

    private val sol = Place("sol", "Sol", GeoPoint(40.4168, -3.7038), null, null, 0)

    /** DataStore falso: guarda en memoria y emite el último guardado, como el real (paso 16). */
    private class FakePreference(initial: NavigationProfile = NavigationProfile.FOCUS) : ProfilePreference {
        private val state = MutableStateFlow(initial)
        override val profile: Flow<NavigationProfile> = state
        override suspend fun save(profile: NavigationProfile) {
            state.value = profile
        }
        val saved: NavigationProfile get() = state.value
    }

    private class FakePlayer : HapticPlayer {
        val events = mutableListOf<HapticEvent>()
        override val canVibrate = true
        override fun play(event: HapticEvent, pattern: HapticPattern) {
            events += event
        }
        override fun cancel() = Unit
    }

    private val store = NavigationSessionStore()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        // llegamos al selector con un destino ya elegido
        store.dispatch(NavigationEvent.StartSearch)
        store.dispatch(NavigationEvent.SelectDestination(sol))
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.setup(preference: ProfilePreference): Pair<ProfileSelectorViewModel, FakePlayer> {
        val player = FakePlayer()
        val engine = HapticEngine(player)
        backgroundScope.launch { engine.follow(emptyFlow(), flowOf(false)) }
        return ProfileSelectorViewModel(store, engine, preference) to player
    }

    @Test
    fun `entering the selector moves the session to ProfileSelection`() = runTest {
        setup(FakePreference())
        assertEquals(NavigationSession.ProfileSelection(sol), store.state.value)
    }

    @Test
    fun `the last chosen profile is preselected`() = runTest {
        val (viewModel, _) = setup(FakePreference(NavigationProfile.TOUR))
        assertEquals(NavigationProfile.TOUR, viewModel.preselected.value)
    }

    @Test
    fun `choosing Tour saves it, confirms with a haptic and loads the route with the profile`() = runTest {
        val preference = FakePreference(NavigationProfile.FOCUS)
        val (viewModel, player) = setup(preference)

        viewModel.select(NavigationProfile.TOUR)

        // la sesión lleva el perfil elegido a RouteLoading
        assertEquals(NavigationSession.RouteLoading(sol, NavigationProfile.TOUR), store.state.value)
        // se recuerda para la próxima vez
        assertEquals(NavigationProfile.TOUR, preference.saved)
        // confirmación háptica por evento semántico (no el vibrador directo)
        advanceTimeBy(3_000)
        assertTrue(HapticEvent.PROFILE_SELECTED in player.events)
    }
}
