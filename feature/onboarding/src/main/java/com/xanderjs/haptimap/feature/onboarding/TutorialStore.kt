package com.xanderjs.haptimap.feature.onboarding

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/** Si el tutorial háptico ya se vio (terminado o saltado): solo se muestra solo la primera vez (paso 13). */
interface TutorialStore {
    val seen: Boolean
    fun markSeen()
}

/**
 * Una preferencia. getSharedPreferences empieza a leer el fichero en segundo plano al crearse (con la Activity);
 * la primera lectura espera a que acabe: un fichero de unos bytes.
 */
@Singleton
class PrefsTutorialStore @Inject constructor(@ApplicationContext context: Context) : TutorialStore {
    private val prefs: SharedPreferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    override val seen: Boolean get() = prefs.getBoolean(KEY_SEEN, false)

    override fun markSeen() = prefs.edit { putBoolean(KEY_SEEN, true) }

    private companion object {
        const val FILE = "onboarding"
        const val KEY_SEEN = "haptic_tutorial_seen"
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class TutorialStoreModule {
    @Binds abstract fun tutorialStore(store: PrefsTutorialStore): TutorialStore
}
