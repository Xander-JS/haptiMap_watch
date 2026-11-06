package com.xanderjs.haptimap.core.haptics

import com.xanderjs.haptimap.core.model.HapticEvent
import com.xanderjs.haptimap.core.model.HapticPattern
import com.xanderjs.haptimap.core.model.HapticTokens
import java.time.LocalTime
import java.util.concurrent.atomic.AtomicIntegerArray
import java.util.logging.Logger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/** Lo último que ha sonado, con el patrón ya escalado. Clase y no data class: dos iguales seguidos son dos avisos. */
class PlayedHaptic(val event: HapticEvent, val pattern: HapticPattern)

/**
 * Haptic Engine (paso 09): recibe solo [HapticEvent] y decide qué suena y cuándo. No conoce la navegación:
 * cada fuente trae su adaptador (HapticCues para los avisos de ruta; las sugerencias de POI de la versión 2, el suyo).
 *
 * Reglas: nunca dos patrones a la vez ni uno cortado; entre dos, [MIN_GAP] de silencio. Lo que llega mientras
 * suena espera, y lo que llega junto (un mismo fix, o durante la espera) se funde en el más prioritario;
 * a igual prioridad, el más reciente. Ver docs/steps/09-motor-haptico.md.
 */
@Singleton
class HapticEngine @Inject constructor(private val player: HapticPlayer) {
    private val queue = Channel<HapticEvent>(Channel.UNLIMITED)
    private val playCounts = AtomicIntegerArray(HapticEvent.entries.size)
    private val _played = MutableStateFlow<PlayedHaptic?>(null)

    /** El último patrón reproducido: el indicador de debug y el tutorial del paso 13. */
    val played: StateFlow<PlayedHaptic?> = _played.asStateFlow()

    /**
     * Si el reloj puede vibrar (paso 13). No sabe si el usuario apagó la vibración en ajustes: Android no lo expone,
     * y con USAGE_ALARM el sistema la calla sin avisar.
     */
    val canVibrate: Boolean get() = player.canVibrate

    /** Intensidad global: multiplica las amplitudes. shortcut: en memoria, el paso 25 la guardará en ajustes. */
    @Volatile var intensity: Float = 1f

    /** Pide un patrón (banco de pruebas, tutorial). Sigue las mismas reglas que los de [follow]. */
    fun play(event: HapticEvent) {
        queue.trySend(event)
    }

    /** Cuántas veces ha sonado [event] desde que arrancó el proceso: los adaptadores saben así si algo se llegó a notar. */
    fun playCount(event: HapticEvent): Int = playCounts[event.ordinal]

    /**
     * Reproduce [events] hasta que se cancele quien llama. Cuando [active] pasa a false (se deja de navegar)
     * corta lo que suena y descarta lo pendiente.
     */
    suspend fun follow(events: Flow<HapticEvent>, active: Flow<Boolean>) = coroutineScope {
        launch { events.collect(::play) }
        var wasActive = false
        active.distinctUntilChanged().collectLatest { isActive ->
            if (wasActive && !isActive) {
                player.cancel()
                while (queue.tryReceive().isSuccess) Unit
                logger.info("cancel at=${LocalTime.now()}")
            }
            wasActive = isActive
            while (true) {
                val first = queue.receive()
                // los avisos de un mismo fix llegan seguidos: se esperan para fundirlos
                delay(COLLAPSE_WINDOW)
                val dropped = mutableListOf<HapticEvent>()
                var event = first
                while (true) {
                    val next = queue.tryReceive().getOrNull() ?: break
                    if (next.priority >= event.priority) {
                        dropped += event
                        event = next
                    } else {
                        dropped += next
                    }
                }
                val pattern = HapticTokens.patternFor(event).scaled(intensity)
                player.play(event, pattern)
                playCounts.incrementAndGet(event.ordinal)
                _played.value = PlayedHaptic(event, pattern)
                logger.info(
                    "play event=$event pattern=${pattern.timings.joinToString("/")}ms amp=${pattern.amplitudes.joinToString("/")} " +
                        "at=${LocalTime.now()}" + if (dropped.isEmpty()) "" else " dropped=$dropped",
                )
                delay(pattern.totalDurationMs.milliseconds + MIN_GAP)
            }
        }
    }

    companion object {
        /** Silencio mínimo entre dos patrones para que no se lean como uno. A calibrar en el paso 14. */
        val MIN_GAP = 600.milliseconds

        /** Lo que se espera a más avisos tras el primero; los de un mismo fix llegan en microsegundos. */
        val COLLAPSE_WINDOW = 100.milliseconds

        /** Una sola etiqueta para todo lo háptico en logcat. */
        internal val logger: Logger = Logger.getLogger("HaptiHaptics")
    }
}

/** Mayor gana. Fuera de ruta y llegada no coinciden (fuera de ruta no hay llegada); la dirección cede ante la urgencia. */
internal val HapticEvent.priority: Int
    get() = when (this) {
        HapticEvent.OFF_ROUTE, HapticEvent.ARRIVAL -> 4
        HapticEvent.BACK_ON_ROUTE -> 3
        HapticEvent.TURN_IMMINENT -> 2
        HapticEvent.TURN_LEFT, HapticEvent.TURN_RIGHT, HapticEvent.CONTINUE -> 1
        // nunca van en el flujo de cues de navegación: no colisionan con nada
        HapticEvent.POI_SUGGESTION, HapticEvent.PROFILE_SELECTED -> 0
    }
