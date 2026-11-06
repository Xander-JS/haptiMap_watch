package com.xanderjs.haptimap.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import android.content.res.Configuration
import com.xanderjs.haptimap.core.model.LanguageOption
import com.xanderjs.haptimap.core.model.ThemeOption
import com.xanderjs.haptimap.designsystem.theme.ThemeMode
import java.util.Locale

/**
 * Puente entre los ajustes del dominio (paso 25) y la presentación: el design system habla en [ThemeMode]
 * (valor de UI, no depende de :core:model). El ajuste persistido sigue siendo la única fuente; aquí solo se traduce.
 */
fun ThemeOption.toThemeMode(): ThemeMode = when (this) {
    ThemeOption.SYSTEM -> ThemeMode.SYSTEM
    ThemeOption.LIGHT -> ThemeMode.LIGHT
    ThemeOption.DARK -> ThemeMode.DARK
}

fun ThemeMode.toThemeOption(): ThemeOption = when (this) {
    ThemeMode.SYSTEM -> ThemeOption.SYSTEM
    ThemeMode.LIGHT -> ThemeOption.LIGHT
    ThemeMode.DARK -> ThemeOption.DARK
}

private fun LanguageOption.toLocale(): Locale? = when (this) {
    LanguageOption.SYSTEM -> null
    LanguageOption.ENGLISH -> Locale.forLanguageTag("en")
    LanguageOption.SPANISH -> Locale.forLanguageTag("es")
}

/**
 * Aplica el idioma por app (paso 25) a toda la UI envolviendo el contenido con una Configuration y un Context
 * localizados: `stringResource` lee de ahí, así que todos los textos (y la voz, vía LocalContext) cambian al
 * idioma elegido sin recrear la Activity. Funciona en toda la gama de API (createConfigurationContext, API 17+)
 * y no toca el tema de Wear. SYSTEM no envuelve nada: deja el idioma del sistema.
 */
@Composable
fun ProvideAppLanguage(language: LanguageOption, content: @Composable () -> Unit) {
    val locale = language.toLocale()
    if (locale == null) {
        content()
        return
    }
    val baseContext = LocalContext.current
    val baseConfig = LocalConfiguration.current
    val config = remember(baseConfig, locale) { Configuration(baseConfig).apply { setLocale(locale) } }
    val localizedContext = remember(baseContext, config) { baseContext.createConfigurationContext(config) }
    CompositionLocalProvider(
        LocalConfiguration provides config,
        LocalContext provides localizedContext,
        content = content,
    )
}
