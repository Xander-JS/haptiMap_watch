package com.xanderjs.haptimap.designsystem.tokens

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp

/** Radios de esquina. [full] = forma de píldora, la de los botones en Wear. */
@Immutable
data class HaptiShapes(
    val small: CornerBasedShape = RoundedCornerShape(8.dp),
    val medium: CornerBasedShape = RoundedCornerShape(16.dp),
    val large: CornerBasedShape = RoundedCornerShape(24.dp),
    val full: CornerBasedShape = CircleShape,
)
