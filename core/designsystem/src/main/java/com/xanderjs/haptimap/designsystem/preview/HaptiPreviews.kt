package com.xanderjs.haptimap.designsystem.preview

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.content.res.Configuration.UI_MODE_TYPE_WATCH
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.tooling.preview.devices.WearDevices

/**
 * Multipreview: {pantalla redonda pequeña, grande, cuadrada} × {claro, oscuro} × {inglés, español}.
 * El tema se resuelve con ThemeMode.SYSTEM, que lee el uiMode de cada preview.
 */
@Preview(name = "round-small-light-en", device = WearDevices.SMALL_ROUND, showSystemUi = true, uiMode = UI_MODE_TYPE_WATCH or UI_MODE_NIGHT_NO, locale = "en")
@Preview(name = "round-small-light-es", device = WearDevices.SMALL_ROUND, showSystemUi = true, uiMode = UI_MODE_TYPE_WATCH or UI_MODE_NIGHT_NO, locale = "es")
@Preview(name = "round-small-dark-en", device = WearDevices.SMALL_ROUND, showSystemUi = true, uiMode = UI_MODE_TYPE_WATCH or UI_MODE_NIGHT_YES, locale = "en")
@Preview(name = "round-small-dark-es", device = WearDevices.SMALL_ROUND, showSystemUi = true, uiMode = UI_MODE_TYPE_WATCH or UI_MODE_NIGHT_YES, locale = "es")
@Preview(name = "round-large-light-en", device = WearDevices.LARGE_ROUND, showSystemUi = true, uiMode = UI_MODE_TYPE_WATCH or UI_MODE_NIGHT_NO, locale = "en")
@Preview(name = "round-large-light-es", device = WearDevices.LARGE_ROUND, showSystemUi = true, uiMode = UI_MODE_TYPE_WATCH or UI_MODE_NIGHT_NO, locale = "es")
@Preview(name = "round-large-dark-en", device = WearDevices.LARGE_ROUND, showSystemUi = true, uiMode = UI_MODE_TYPE_WATCH or UI_MODE_NIGHT_YES, locale = "en")
@Preview(name = "round-large-dark-es", device = WearDevices.LARGE_ROUND, showSystemUi = true, uiMode = UI_MODE_TYPE_WATCH or UI_MODE_NIGHT_YES, locale = "es")
@Preview(name = "square-light-en", device = WearDevices.SQUARE, showSystemUi = true, uiMode = UI_MODE_TYPE_WATCH or UI_MODE_NIGHT_NO, locale = "en")
@Preview(name = "square-light-es", device = WearDevices.SQUARE, showSystemUi = true, uiMode = UI_MODE_TYPE_WATCH or UI_MODE_NIGHT_NO, locale = "es")
@Preview(name = "square-dark-en", device = WearDevices.SQUARE, showSystemUi = true, uiMode = UI_MODE_TYPE_WATCH or UI_MODE_NIGHT_YES, locale = "en")
@Preview(name = "square-dark-es", device = WearDevices.SQUARE, showSystemUi = true, uiMode = UI_MODE_TYPE_WATCH or UI_MODE_NIGHT_YES, locale = "es")
annotation class HaptiPreviews

/** El tamaño de letra más grande que ofrece Wear OS (Ajustes → Pantalla → Tamaño de fuente → "Largest", API 37). */
const val LARGEST_FONT_SCALE = 1.24f

/** Las mismas 12 combinaciones que [HaptiPreviews] con la letra más grande del sistema (paso 15A, accesibilidad). */
@Preview(name = "round-small-light-en", device = WearDevices.SMALL_ROUND, showSystemUi = true, fontScale = LARGEST_FONT_SCALE, uiMode = UI_MODE_TYPE_WATCH or UI_MODE_NIGHT_NO, locale = "en")
@Preview(name = "round-small-light-es", device = WearDevices.SMALL_ROUND, showSystemUi = true, fontScale = LARGEST_FONT_SCALE, uiMode = UI_MODE_TYPE_WATCH or UI_MODE_NIGHT_NO, locale = "es")
@Preview(name = "round-small-dark-en", device = WearDevices.SMALL_ROUND, showSystemUi = true, fontScale = LARGEST_FONT_SCALE, uiMode = UI_MODE_TYPE_WATCH or UI_MODE_NIGHT_YES, locale = "en")
@Preview(name = "round-small-dark-es", device = WearDevices.SMALL_ROUND, showSystemUi = true, fontScale = LARGEST_FONT_SCALE, uiMode = UI_MODE_TYPE_WATCH or UI_MODE_NIGHT_YES, locale = "es")
@Preview(name = "round-large-light-en", device = WearDevices.LARGE_ROUND, showSystemUi = true, fontScale = LARGEST_FONT_SCALE, uiMode = UI_MODE_TYPE_WATCH or UI_MODE_NIGHT_NO, locale = "en")
@Preview(name = "round-large-light-es", device = WearDevices.LARGE_ROUND, showSystemUi = true, fontScale = LARGEST_FONT_SCALE, uiMode = UI_MODE_TYPE_WATCH or UI_MODE_NIGHT_NO, locale = "es")
@Preview(name = "round-large-dark-en", device = WearDevices.LARGE_ROUND, showSystemUi = true, fontScale = LARGEST_FONT_SCALE, uiMode = UI_MODE_TYPE_WATCH or UI_MODE_NIGHT_YES, locale = "en")
@Preview(name = "round-large-dark-es", device = WearDevices.LARGE_ROUND, showSystemUi = true, fontScale = LARGEST_FONT_SCALE, uiMode = UI_MODE_TYPE_WATCH or UI_MODE_NIGHT_YES, locale = "es")
@Preview(name = "square-light-en", device = WearDevices.SQUARE, showSystemUi = true, fontScale = LARGEST_FONT_SCALE, uiMode = UI_MODE_TYPE_WATCH or UI_MODE_NIGHT_NO, locale = "en")
@Preview(name = "square-light-es", device = WearDevices.SQUARE, showSystemUi = true, fontScale = LARGEST_FONT_SCALE, uiMode = UI_MODE_TYPE_WATCH or UI_MODE_NIGHT_NO, locale = "es")
@Preview(name = "square-dark-en", device = WearDevices.SQUARE, showSystemUi = true, fontScale = LARGEST_FONT_SCALE, uiMode = UI_MODE_TYPE_WATCH or UI_MODE_NIGHT_YES, locale = "en")
@Preview(name = "square-dark-es", device = WearDevices.SQUARE, showSystemUi = true, fontScale = LARGEST_FONT_SCALE, uiMode = UI_MODE_TYPE_WATCH or UI_MODE_NIGHT_YES, locale = "es")
annotation class HaptiLargeFontPreviews
