package com.xanderjs.haptimap.feature.settings

import com.xanderjs.haptimap.core.haptics.HapticEngine
import com.xanderjs.haptimap.core.haptics.HapticPlayer
import com.xanderjs.haptimap.core.haptics.amplitudeScale
import com.xanderjs.haptimap.core.model.ExplorationCategory
import com.xanderjs.haptimap.core.model.HapticEvent
import com.xanderjs.haptimap.core.model.HapticIntensity
import com.xanderjs.haptimap.core.model.HapticPattern
import com.xanderjs.haptimap.core.model.SettingsRepository
import com.xanderjs.haptimap.core.model.ThemeOption
import com.xanderjs.haptimap.core.model.UserSettings
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    /** Repositorio falso en memoria, como el real: emite el último guardado y aplica sanitized(). */
    private class FakeSettingsRepository(initial: UserSettings = UserSettings()) : SettingsRepository {
        private val state = MutableStateFlow(initial)
        override val settings: Flow<UserSettings> = state
        override suspend fun update(transform: (UserSettings) -> UserSettings) {
            state.value = transform(state.value).sanitized()
        }
        val current: UserSettings get() = state.value
    }

    private class FakePlayer : HapticPlayer {
        val events = mutableListOf<HapticEvent>()
        override fun play(event: HapticEvent, pattern: HapticPattern) { events += event }
        override fun cancel() = Unit
    }

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.setup(repo: SettingsRepository): Triple<SettingsViewModel, HapticEngine, FakePlayer> {
        val player = FakePlayer()
        val engine = HapticEngine(player)
        backgroundScope.launch { engine.follow(emptyFlow(), flowOf(false)) }
        return Triple(SettingsViewModel(repo, engine), engine, player)
    }

    @Test
    fun `changing intensity scales the engine now, previews it and persists`() = runTest {
        val repo = FakeSettingsRepository()
        val (viewModel, engine, player) = setup(repo)

        viewModel.setHapticIntensity(HapticIntensity.HIGH)

        assertEquals(HapticIntensity.HIGH.amplitudeScale, engine.intensity)
        assertEquals(HapticIntensity.HIGH, repo.current.hapticIntensity)
        advanceTimeBy(3_000)
        assertTrue(HapticEvent.PROFILE_SELECTED in player.events)
    }

    @Test
    fun `toggling a favorite adds it and toggling again removes it`() = runTest {
        val repo = FakeSettingsRepository()
        val (viewModel, _, _) = setup(repo)

        viewModel.toggleFavorite(ExplorationCategory.MUSEUM)
        assertTrue(ExplorationCategory.MUSEUM in repo.current.favoriteCategories)
        viewModel.toggleFavorite(ExplorationCategory.MUSEUM)
        assertFalse(ExplorationCategory.MUSEUM in repo.current.favoriteCategories)
    }

    @Test
    fun `the suggestion timeout stays within its range`() = runTest {
        val repo = FakeSettingsRepository(UserSettings(suggestionTimeoutSeconds = 10))
        val (viewModel, _, _) = setup(repo)

        viewModel.setSuggestionTimeout(11)
        assertEquals(10, repo.current.suggestionTimeoutSeconds)
    }

    @Test
    fun `changing the theme persists it for the rest of the app to read`() = runTest {
        val repo = FakeSettingsRepository()
        val (viewModel, _, _) = setup(repo)

        viewModel.setTheme(ThemeOption.DARK)
        assertEquals(ThemeOption.DARK, repo.current.theme)
    }
}
