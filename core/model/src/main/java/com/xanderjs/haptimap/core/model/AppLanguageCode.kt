package com.xanderjs.haptimap.core.model

/**
 * Código de idioma ("en"/"es") que debe usar la app para una preferencia (paso 25/17). SYSTEM usa el idioma
 * que haya resuelto el sistema ([systemLanguage], p. ej. el que Android eligió del localeConfig). Única fuente
 * del idioma de las peticiones a Google (Places/Routes) y de la voz, para que los resultados sigan al ajuste.
 */
fun LanguageOption.resolvedLanguageCode(systemLanguage: String): String = when (this) {
    LanguageOption.SYSTEM -> systemLanguage
    LanguageOption.ENGLISH -> "en"
    LanguageOption.SPANISH -> "es"
}
