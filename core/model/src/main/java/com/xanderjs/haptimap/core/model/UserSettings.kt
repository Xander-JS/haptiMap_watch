package com.xanderjs.haptimap.core.model

import kotlinx.coroutines.flow.Flow

/** Intensidad háptica global: escala las amplitudes del Haptic Engine (paso 25). */
enum class HapticIntensity { LOW, MEDIUM, HIGH }

/**
 * Tolerancia al desvío del Modo Tour, en presets (paso 25): el usuario no ve metros, sino "corto / medio /
 * largo", que la capa de inteligencia traduce a umbrales coherentes de [PoiEvaluationConfig].
 */
enum class DetourTolerance { SHORT, MEDIUM, LONG }

/**
 * Cómo se muestra la navegación (paso 25). El mapa continuo llega en el paso 22; hasta entonces la UI lo
 * ofrece como "próximamente" y la app usa [ECO_AOD].
 */
enum class DisplayMode { ECO_AOD, CONTINUOUS_MAP }

/** Preferencia de tema (paso 25). Se traduce al ThemeMode del design system en la capa de presentación. */
enum class ThemeOption { SYSTEM, LIGHT, DARK }

/** Preferencia de idioma (paso 25). SYSTEM = el del reloj; el resto fija el idioma por app (localeConfig). */
enum class LanguageOption { SYSTEM, ENGLISH, SPANISH }

/** Segundos de espera de una sugerencia de POI antes de descartarla (paso 19). El ajuste lo limita a este rango. */
val SUGGESTION_TIMEOUT_SECONDS: IntRange = 5..10

/**
 * Ajustes del usuario (paso 25): la ÚNICA fuente de verdad de estas preferencias. Cada módulo las lee del
 * [SettingsRepository]; ninguno guarda su propia copia. Modelo puro (sin Android) para que la lógica y los
 * tests dependan de él, no de DataStore.
 */
data class UserSettings(
    val hapticIntensity: HapticIntensity = HapticIntensity.MEDIUM,
    /** Espera de sugerencias de POI (paso 19), siempre dentro de [SUGGESTION_TIMEOUT_SECONDS]. */
    val suggestionTimeoutSeconds: Int = 7,
    /** Categorías de exploración favoritas: dan un plus de relevancia a sus POIs (RuleBasedRelevanceProvider). */
    val favoriteCategories: Set<ExplorationCategory> = emptySet(),
    val detourTolerance: DetourTolerance = DetourTolerance.MEDIUM,
    val displayMode: DisplayMode = DisplayMode.ECO_AOD,
    val theme: ThemeOption = ThemeOption.SYSTEM,
    val language: LanguageOption = LanguageOption.SYSTEM,
) {
    /** Corrige valores fuera de rango que puedan venir de disco (versión vieja, dato corrupto): el timeout al rango. */
    fun sanitized(): UserSettings =
        copy(suggestionTimeoutSeconds = suggestionTimeoutSeconds.coerceIn(SUGGESTION_TIMEOUT_SECONDS.first, SUGGESTION_TIMEOUT_SECONDS.last))
}

/**
 * Lee y escribe los [UserSettings] (paso 25). Contrato puro; la implementación con DataStore vive en la capa
 * Android. [settings] emite el valor actual y cada cambio; sin nada guardado o con datos inválidos, los
 * valores por defecto de [UserSettings].
 */
interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun update(transform: (UserSettings) -> UserSettings)
}
