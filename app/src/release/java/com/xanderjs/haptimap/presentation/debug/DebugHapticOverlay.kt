package com.xanderjs.haptimap.presentation.debug

import androidx.compose.runtime.Composable
import com.xanderjs.haptimap.core.haptics.PlayedHaptic
import kotlinx.coroutines.flow.StateFlow

/** En release no hay indicador háptico (src/debug): la vibración es la única señal. */
@Suppress("UNUSED_PARAMETER")
@Composable
fun DebugHapticOverlay(played: StateFlow<PlayedHaptic?>) = Unit
