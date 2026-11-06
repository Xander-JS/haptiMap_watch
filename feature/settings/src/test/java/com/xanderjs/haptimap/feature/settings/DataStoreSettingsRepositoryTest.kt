package com.xanderjs.haptimap.feature.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.xanderjs.haptimap.core.model.DetourTolerance
import com.xanderjs.haptimap.core.model.ExplorationCategory
import com.xanderjs.haptimap.core.model.HapticIntensity
import com.xanderjs.haptimap.core.model.LanguageOption
import com.xanderjs.haptimap.core.model.ThemeOption
import com.xanderjs.haptimap.core.model.UserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreSettingsRepositoryTest {

    @get:Rule val tmp = TemporaryFolder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @After fun tearDown() = scope.cancel()

    private fun store(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = scope) { tmp.newFile("settings_${System.nanoTime()}.preferences_pb") }

    @Test
    fun `an empty store returns the defaults`() = runBlocking {
        val repo = DataStoreSettingsRepository(store())
        assertEquals(UserSettings(), repo.settings.first())
    }

    @Test
    fun `values persist across repository instances over the same store`() = runBlocking {
        val ds = store()
        val saved = UserSettings(
            hapticIntensity = HapticIntensity.HIGH,
            suggestionTimeoutSeconds = 9,
            favoriteCategories = setOf(ExplorationCategory.MUSEUM, ExplorationCategory.VIEWPOINT),
            detourTolerance = DetourTolerance.LONG,
            theme = ThemeOption.DARK,
            language = LanguageOption.SPANISH,
        )
        DataStoreSettingsRepository(ds).update { saved }
        // una instancia nueva sobre el mismo fichero lee lo guardado
        assertEquals(saved, DataStoreSettingsRepository(ds).settings.first())
    }

    @Test
    fun `an out-of-range timeout is clamped on read`() = runBlocking {
        val ds = store()
        ds.edit { it[intPreferencesKey("suggestion_timeout_seconds")] = 60 }
        assertEquals(10, DataStoreSettingsRepository(ds).settings.first().suggestionTimeoutSeconds)
    }

    @Test
    fun `an unknown enum value falls back to its default`() = runBlocking {
        val ds = store()
        ds.edit { it[stringPreferencesKey("theme")] = "NEON" }
        assertEquals(ThemeOption.SYSTEM, DataStoreSettingsRepository(ds).settings.first().theme)
    }
}
