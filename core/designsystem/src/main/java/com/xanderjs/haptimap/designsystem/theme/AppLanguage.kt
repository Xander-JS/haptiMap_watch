package com.xanderjs.haptimap.designsystem.theme

import android.content.Context
import com.xanderjs.haptimap.designsystem.R

/**
 * Idioma de la app ("en" / "es") según la Configuration del Context, que incluye el idioma por app
 * de Android 13+. Se lee del recurso que Android resolvió, así un idioma no soportado cae en inglés
 * igual que los textos. Única fuente para el languageCode de Places y el formato de distancias.
 */
fun Context.appLanguage(): String = getString(R.string.app_language)
