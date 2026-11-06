package com.xanderjs.haptimap.core.navigation

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.transformLatest

/** Cuánto sigue el servicio tras llegar: que el patrón de llegada suene entero y la notificación lleve a la pantalla de llegada. */
val ARRIVED_LINGER = 60.seconds

/**
 * Cuánto sigue el bloqueo de CPU del servicio tras llegar (paso 15A): navigationServiceRuns(states, ARRIVED_WAKE_LOCK_LINGER).
 * Con Arrived ya no hay GPS ni esperas del recálculo; solo falta que el patrón de llegada (~1 s, con la ventana de
 * fusión) empiece a sonar, y una vez pedido al vibrador suena entero. Antes el bloqueo duraba los 60 s de [ARRIVED_LINGER].
 */
val ARRIVED_WAKE_LOCK_LINGER = 5.seconds

/**
 * Cuándo debe correr el servicio en primer plano (paso 10): mientras se navega (NavigationActive, OffRoute,
 * Recalculating) y [arrivedLinger] tras Arrived; Finish o Cancel lo paran al momento. RouteReady no: aún no hay
 * nada que seguir, y es donde aterriza una ruta retomada tras la muerte del proceso, que nunca debe arrancar sola.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun navigationServiceRuns(states: Flow<NavigationSession>, arrivedLinger: Duration = ARRIVED_LINGER): Flow<Boolean> =
    states.transformLatest { session ->
        emit(session.navigatingRoute != null || session is NavigationSession.Arrived)
        if (session is NavigationSession.Arrived) {
            delay(arrivedLinger)
            emit(false)
        }
    }.distinctUntilChanged()
