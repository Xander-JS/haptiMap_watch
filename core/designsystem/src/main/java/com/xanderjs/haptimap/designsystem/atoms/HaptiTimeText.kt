package com.xanderjs.haptimap.designsystem.atoms

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.timeTextCurvedText
import androidx.wear.compose.material3.timeTextSeparator
import com.xanderjs.haptimap.designsystem.R
import com.xanderjs.haptimap.designsystem.preview.HaptiPreviews
import com.xanderjs.haptimap.designsystem.preview.PreviewSurface
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/** Sin conexión a internet. La app lo provee en la raíz; cada HaptiTimeText lo lee sin que las pantallas lo pasen. */
val LocalOffline = compositionLocalOf { false }

/**
 * Hora curva en el borde superior. Wear la pinta por defecto al 80 % de luminancia, ilegible
 * sobre el fondo blanco del tema claro; aquí usa el token onBackground.
 *
 * Con [offline], antepone "Sin conexión" en warning: la franja de la hora es la zona de estado de
 * Wear, así el aviso se ve en todas las pantallas sin tapar el contenido de navegación.
 */
@Composable
fun HaptiTimeText(offline: Boolean = LocalOffline.current) {
    val style = CurvedTextStyle(color = HaptiMapTheme.colors.onBackground)
    val offlineStyle = CurvedTextStyle(color = HaptiMapTheme.colors.warning)
    val offlineText = stringResource(R.string.offline_indicator)
    TimeText { time ->
        if (offline) {
            timeTextCurvedText(offlineText, offlineStyle)
            timeTextSeparator(style)
        }
        timeTextCurvedText(time, style)
    }
}

@HaptiPreviews
@Composable
private fun HaptiTimeTextPreview() = PreviewSurface { HaptiTimeText() }

@HaptiPreviews
@Composable
private fun HaptiTimeTextOfflinePreview() = PreviewSurface { HaptiTimeText(offline = true) }
