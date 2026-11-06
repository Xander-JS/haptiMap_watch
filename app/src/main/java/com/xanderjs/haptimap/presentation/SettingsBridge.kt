package com.xanderjs.haptimap.presentation

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.xanderjs.haptimap.core.model.LanguageOption
import com.xanderjs.haptimap.core.model.ThemeOption
import com.xanderjs.haptimap.designsystem.theme.ThemeMode

/**
 * Puente entre los ajustes del dominio (paso 25) y la presentación: el design system habla en [ThemeMode]
 * (valor de UI, no depende de :core:model) y el idioma se aplica con el soporte de idioma por app de Android.
 * El ajuste persistido sigue siendo la única fuente; aquí solo se traduce en un sitio.
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

private fun LanguageOption.localeList(): LocaleListCompat = when (this) {
    LanguageOption.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
    LanguageOption.ENGLISH -> LocaleListCompat.forLanguageTags("en")
    LanguageOption.SPANISH -> LocaleListCompat.forLanguageTags("es")
}

/**
 * Fija el idioma por app (AppCompat, compatible con minSdk 30 y con el localeConfig del manifest). Idempotente:
 * si ya está aplicado no hace nada, así la recreación de la Activity que provoca el cambio no entra en bucle.
 */
fun applyAppLanguage(language: LanguageOption) {
    val target = language.localeList()
    if (AppCompatDelegate.getApplicationLocales() != target) AppCompatDelegate.setApplicationLocales(target)
}
