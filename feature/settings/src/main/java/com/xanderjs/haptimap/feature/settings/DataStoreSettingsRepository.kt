package com.xanderjs.haptimap.feature.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.xanderjs.haptimap.core.model.DetourTolerance
import com.xanderjs.haptimap.core.model.DisplayMode
import com.xanderjs.haptimap.core.model.ExplorationCategory
import com.xanderjs.haptimap.core.model.HapticIntensity
import com.xanderjs.haptimap.core.model.LanguageOption
import com.xanderjs.haptimap.core.model.SettingsRepository
import com.xanderjs.haptimap.core.model.ThemeOption
import com.xanderjs.haptimap.core.model.UserSettings
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Los [UserSettings] en DataStore (paso 25), única fuente de verdad. Cada preferencia es una clave; una que
 * falte (versión vieja, primer arranque) o con un valor inválido (enum desconocido) vuelve a su valor por
 * defecto sin lanzar, y el timeout se recorta a su rango vía [UserSettings.sanitized]. Recibe el DataStore por
 * constructor para poder probar persistencia sobre un fichero temporal sin Android.
 */
@Singleton
class DataStoreSettingsRepository @Inject constructor(private val dataStore: DataStore<Preferences>) : SettingsRepository {

    override val settings: Flow<UserSettings> = dataStore.data.map { it.toUserSettings() }

    override suspend fun update(transform: (UserSettings) -> UserSettings) {
        dataStore.edit { prefs ->
            prefs.write(transform(prefs.toUserSettings()))
        }
    }

    private fun Preferences.toUserSettings(): UserSettings = UserSettings(
        hapticIntensity = enum(this[HAPTIC_INTENSITY], HapticIntensity.MEDIUM),
        suggestionTimeoutSeconds = this[SUGGESTION_TIMEOUT] ?: UserSettings().suggestionTimeoutSeconds,
        favoriteCategories = this[FAVORITE_CATEGORIES].orEmpty().mapNotNull { enumOrNull<ExplorationCategory>(it) }.toSet(),
        detourTolerance = enum(this[DETOUR_TOLERANCE], DetourTolerance.MEDIUM),
        displayMode = enum(this[DISPLAY_MODE], DisplayMode.ECO_AOD),
        theme = enum(this[THEME], ThemeOption.SYSTEM),
        language = enum(this[LANGUAGE], LanguageOption.SYSTEM),
    ).sanitized()

    private fun MutablePreferences.write(s: UserSettings) {
        this[HAPTIC_INTENSITY] = s.hapticIntensity.name
        this[SUGGESTION_TIMEOUT] = s.suggestionTimeoutSeconds
        this[FAVORITE_CATEGORIES] = s.favoriteCategories.map { it.name }.toSet()
        this[DETOUR_TOLERANCE] = s.detourTolerance.name
        this[DISPLAY_MODE] = s.displayMode.name
        this[THEME] = s.theme.name
        this[LANGUAGE] = s.language.name
    }

    private companion object {
        val HAPTIC_INTENSITY = stringPreferencesKey("haptic_intensity")
        val SUGGESTION_TIMEOUT = intPreferencesKey("suggestion_timeout_seconds")
        val FAVORITE_CATEGORIES = stringSetPreferencesKey("favorite_categories")
        val DETOUR_TOLERANCE = stringPreferencesKey("detour_tolerance")
        val DISPLAY_MODE = stringPreferencesKey("display_mode")
        val THEME = stringPreferencesKey("theme")
        val LANGUAGE = stringPreferencesKey("language")
    }
}

/** Enum por nombre, o [fallback] si falta o no existe (dato de una versión vieja o corrupto). */
private inline fun <reified E : Enum<E>> enum(name: String?, fallback: E): E = enumOrNull<E>(name) ?: fallback

private inline fun <reified E : Enum<E>> enumOrNull(name: String?): E? =
    name?.let { runCatching { enumValueOf<E>(it) }.getOrNull() }

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SettingsRepositoryModule {
    @Binds abstract fun settingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    companion object {
        @Provides @Singleton
        fun dataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.settingsDataStore
    }
}
