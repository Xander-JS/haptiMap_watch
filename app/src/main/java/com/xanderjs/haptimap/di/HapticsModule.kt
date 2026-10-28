package com.xanderjs.haptimap.di

import android.content.Context
import com.xanderjs.haptimap.core.haptics.AndroidHapticPlayer
import com.xanderjs.haptimap.core.haptics.HapticPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** El vibrador del sistema (paso 09). En :app para que :core:haptics no dependa de Hilt. */
@Module
@InstallIn(SingletonComponent::class)
object HapticsModule {
    @Provides
    @Singleton
    fun hapticPlayer(@ApplicationContext context: Context): HapticPlayer = AndroidHapticPlayer(context)
}
