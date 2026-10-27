package com.xanderjs.haptimap.di

import android.content.Context
import com.xanderjs.haptimap.core.location.FusedLocationSource
import com.xanderjs.haptimap.core.location.LocationSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** El enlace vive en :app para que :core:location no dependa de Hilt. */
@Module
@InstallIn(SingletonComponent::class)
object LocationModule {
    @Provides
    @Singleton
    fun locationSource(@ApplicationContext context: Context): LocationSource = FusedLocationSource(context)
}
