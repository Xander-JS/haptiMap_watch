package com.xanderjs.haptimap.di

import android.content.Context
import com.xanderjs.haptimap.core.haptics.AndroidHapticPlayer
import com.xanderjs.haptimap.core.haptics.HapticPlayer
import com.xanderjs.haptimap.core.model.HapticEvent
import com.xanderjs.haptimap.core.model.HapticPattern
import com.xanderjs.haptimap.presentation.debug.WalkSimulationSetting
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * El vibrador del sistema (paso 09). En debug, "Simular sin vibrador" del catálogo (paso 13) hace como si el reloj
 * no lo tuviera: el emulador siempre tiene uno.
 */
@Module
@InstallIn(SingletonComponent::class)
object HapticsModule {
    @Provides
    @Singleton
    fun hapticPlayer(@ApplicationContext context: Context, setting: WalkSimulationSetting): HapticPlayer {
        val real = AndroidHapticPlayer(context)
        return object : HapticPlayer {
            override val canVibrate: Boolean get() = real.canVibrate && !setting.noVibrator.value
            override fun play(event: HapticEvent, pattern: HapticPattern) {
                if (canVibrate) real.play(event, pattern)
            }
            override fun cancel() = real.cancel()
        }
    }
}
