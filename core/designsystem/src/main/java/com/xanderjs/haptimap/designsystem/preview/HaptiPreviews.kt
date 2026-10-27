package com.xanderjs.haptimap.designsystem.preview

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.content.res.Configuration.UI_MODE_TYPE_WATCH
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.tooling.preview.devices.WearDevices

/**
 * Multipreview: {pantalla redonda pequeña, grande} × {claro, oscuro} × {inglés, español}.
 * El tema se resuelve con ThemeMode.SYSTEM, que lee el uiMode de cada preview.
 */
@Preview(name = "S · light · en", device = WearDevices.SMALL_ROUND, showSystemUi = true, uiMode = UI_MODE_TYPE_WATCH or UI_MODE_NIGHT_NO, locale = "en")
@Preview(name = "S · light · es", device = WearDevices.SMALL_ROUND, showSystemUi = true, uiMode = UI_MODE_TYPE_WATCH or UI_MODE_NIGHT_NO, locale = "es")
@Preview(name = "S · dark · en", device = WearDevices.SMALL_ROUND, showSystemUi = true, uiMode = UI_MODE_TYPE_WATCH or UI_MODE_NIGHT_YES, locale = "en")
@Preview(name = "S · dark · es", device = WearDevices.SMALL_ROUND, showSystemUi = true, uiMode = UI_MODE_TYPE_WATCH or UI_MODE_NIGHT_YES, locale = "es")
@Preview(name = "L · light · en", device = WearDevices.LARGE_ROUND, showSystemUi = true, uiMode = UI_MODE_TYPE_WATCH or UI_MODE_NIGHT_NO, locale = "en")
@Preview(name = "L · light · es", device = WearDevices.LARGE_ROUND, showSystemUi = true, uiMode = UI_MODE_TYPE_WATCH or UI_MODE_NIGHT_NO, locale = "es")
@Preview(name = "L · dark · en", device = WearDevices.LARGE_ROUND, showSystemUi = true, uiMode = UI_MODE_TYPE_WATCH or UI_MODE_NIGHT_YES, locale = "en")
@Preview(name = "L · dark · es", device = WearDevices.LARGE_ROUND, showSystemUi = true, uiMode = UI_MODE_TYPE_WATCH or UI_MODE_NIGHT_YES, locale = "es")
annotation class HaptiPreviews
