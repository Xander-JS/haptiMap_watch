package com.xanderjs.haptimap.designsystem.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.xanderjs.haptimap.designsystem.theme.HaptiMapTheme

/** Lienzo de preview: aplica el tema (o el de ambiente) y pinta el fondo semántico detrás del componente. */
@Composable
fun PreviewSurface(ambient: Boolean = false, content: @Composable () -> Unit) {
    HaptiMapTheme(ambient = ambient) {
        Box(
            modifier = Modifier.fillMaxSize().background(HaptiMapTheme.colors.background),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}
