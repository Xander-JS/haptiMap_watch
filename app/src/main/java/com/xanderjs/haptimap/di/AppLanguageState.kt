package com.xanderjs.haptimap.di

import android.content.Context
import com.xanderjs.haptimap.core.model.SettingsRepository
import com.xanderjs.haptimap.core.model.resolvedLanguageCode
import com.xanderjs.haptimap.designsystem.theme.appLanguage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * El código de idioma ("en"/"es") que deben usar las peticiones a Google (paso 25/17), siguiendo el ajuste de
 * idioma: así los nombres de lugares y las rutas llegan en el idioma elegido, no solo la UI. SYSTEM usa el idioma
 * del sistema. Se lee sin suspender ([current]) en cada petición; un recolector a nivel de proceso lo mantiene al día.
 */
@Singleton
class AppLanguageState @Inject constructor(
    settings: SettingsRepository,
    @ApplicationContext context: Context,
) {
    @Volatile
    var current: String = context.appLanguage() // el del sistema hasta que cargue el ajuste
        private set

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            settings.settings.map { it.language.resolvedLanguageCode(context.appLanguage()) }.collect { current = it }
        }
    }
}
