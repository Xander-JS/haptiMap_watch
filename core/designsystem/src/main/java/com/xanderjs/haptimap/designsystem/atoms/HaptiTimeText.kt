package com.xanderjs.haptimap.designsystem.atoms

import androidx.compose.runtime.Composable
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.timeTextCurvedText
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/**
 * Hora curva en el borde superior. Wear la pinta por defecto al 80 % de luminancia, ilegible
 * sobre el fondo blanco del tema claro; aquí usa el token onBackground.
 */
@Composable
fun HaptiTimeText() {
    val style = CurvedTextStyle(color = HaptiMapTheme.colors.onBackground)
    TimeText { time -> timeTextCurvedText(time, style) }
}

@HaptiPreviews
@Composable
private fun HaptiTimeTextPreview() = PreviewSurface { HaptiTimeText() }
