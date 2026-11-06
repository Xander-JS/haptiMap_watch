package com.xanderjs.haptimap.feature.navigationui

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.xanderjs.haptimap.core.model.NavigationProfile
import com.xanderjs.haptimap.core.navigation.ProfilePreference
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.profileDataStore: DataStore<Preferences> by preferencesDataStore(name = "navigation_profile")

/**
 * El último perfil elegido, en DataStore (paso 16). Un valor desconocido en disco (versión vieja, dato corrupto)
 * vuelve como FOCUS, el comportamiento de la v1, sin lanzar.
 */
@Singleton
class DataStoreProfilePreference @Inject constructor(@ApplicationContext private val context: Context) : ProfilePreference {
    override val profile: Flow<NavigationProfile> = context.profileDataStore.data.map { prefs ->
        prefs[KEY]?.let { name -> runCatching { NavigationProfile.valueOf(name) }.getOrNull() } ?: NavigationProfile.FOCUS
    }

    override suspend fun save(profile: NavigationProfile) {
        context.profileDataStore.edit { it[KEY] = profile.name }
    }

    private companion object {
        val KEY = stringPreferencesKey("last_profile")
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ProfilePreferenceModule {
    @Binds abstract fun profilePreference(impl: DataStoreProfilePreference): ProfilePreference
}
