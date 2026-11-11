package com.xanderjs.haptimap.designsystem.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Tamaños de componentes. */
@Immutable
data class HaptiDimens(
    /** Área táctil mínima global (accesibilidad): ningún elemento interactivo baja de esto. */
    val minTouchTarget: Dp = 48.dp,
    val iconSmall: Dp = 16.dp,
    val iconMedium: Dp = 24.dp,
    val iconLarge: Dp = 56.dp,
    val iconDisc: Dp = 32.dp,
    val borderWidth: Dp = 1.dp,
    /** Contorno de tarjetas y botones sobre surface (color outline): visible de un vistazo, no un hilo. */
    val outlineWidth: Dp = 1.5.dp,
    /** Disco de la flecha de maniobra (Pulso) y el anillo de reposo a su alrededor. */
    val maneuverDisc: Dp = 52.dp,
    val maneuverRingGap: Dp = 6.dp,
    /** Disco del POI dentro del anillo de la cuenta atrás (cabe con el trazo del anillo). */
    val poiDisc: Dp = 44.dp,
    /** Hasta dónde se expande un pulso más allá del disco; se dibuja fuera de su sitio, sin empujar nada. */
    val pulseReach: Dp = 22.dp,
    /** Botón redondo de la acción principal sin texto (voz). */
    val iconButtonLarge: Dp = 60.dp,
    /** HapticWaveform: alto en la tarjeta del banco de pruebas y tamaño compacto del indicador de debug. */
    val waveformHeight: Dp = 24.dp,
    val waveformCompactHeight: Dp = 14.dp,
    val waveformCompactWidth: Dp = 36.dp,
    /** StepIndicator (paso 13): el punto de cada paso y el ancho del actual. */
    val stepDot: Dp = 6.dp,
    val stepDotCurrentWidth: Dp = 14.dp,
    /**
     * Margen extra bajo la franja de la hora: el aviso curvo "Sin red" baja por los lados y tapaba el primer elemento.
     * Depende de la forma (ver [haptiDimens]).
     */
    val clockBandSafeTop: Dp = 8.dp,
    /**
     * Lo que se añade a [clockBandSafeTop] sin conexión con la letra del sistema por encima de 1 (paso 15A): "Sin red"
     * crece en alto y en arco y sus extremos bajan más por los lados. Sin el aviso la hora sola no llega al contenido.
     * Se usa a través de clockBandTop(offline).
     */
    val clockBandOfflineGrowth: Dp = 0.dp,
    /**
     * Ancho del contenido centrado (CenteredScreenTemplate) respecto a la pantalla. Con 0,8 un bloque de
     * hasta ~0,6 veces el alto (mensaje de 3 líneas + botón) cabe entero dentro del círculo.
     */
    val centeredContentWidthFraction: Float = 0.8f,
)

/**
 * Los tamaños para la forma de la pantalla. En la cuadrada la hora curva baja igual por los lados, pero el padding
 * de ScreenScaffold es menor y el contenido llega hasta las esquinas: con 8 dp "10:10" tocaba "¿A dónde vas?". Sin conexión
 * y con la letra del sistema más grande la franja crece ([fontScale], paso 15A).
 */
internal fun haptiDimens(round: Boolean, fontScale: Float = 1f) = HaptiDimens(
    clockBandSafeTop = if (round) 8.dp else 16.dp,
    clockBandOfflineGrowth = OFFLINE_CLOCK_BAND_GROWTH * (fontScale - 1f).coerceAtLeast(0f),
)

/** Por cada 1,0 de escala de letra por encima de 1; medido en las capturas a 1,24 (22 dp), las tres formas. */
private val OFFLINE_CLOCK_BAND_GROWTH = 90.dp
