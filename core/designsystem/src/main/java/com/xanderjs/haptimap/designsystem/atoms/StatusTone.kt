package com.xanderjs.haptimap.designsystem.atoms

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * Tono de un mensaje de estado: Default (texto normal, p. ej. cargando), Muted (no es un fallo, p. ej. sin
 * resultados), Warning (una condición que el usuario puede esperar, como sin red) o Error.
 * Lo usan StatusMessage y [NoticeLabel].
 */
enum class StatusTone { Default, Muted, Warning, Error }

/** Color del texto en cada tono; todos pasan AA sobre background y sobre surface. */
val StatusTone.color: Color
    @Composable get() = when (this) {
        StatusTone.Default -> HaptiMapTheme.colors.onBackground
        StatusTone.Muted -> HaptiMapTheme.colors.onSurfaceVariant
        StatusTone.Warning -> HaptiMapTheme.colors.warning
        StatusTone.Error -> HaptiMapTheme.colors.error
    }
