package com.xanderjs.haptimap.core.network

import java.time.LocalDate

/** Qué API de Google se llamó, para el contador de depuración (paso 17). */
enum class ApiKind { PLACES, ROUTES }

/**
 * Presupuesto de llamadas a Google Maps Platform (paso 17, prioridad: coste). ÚNICO sitio donde se cuentan las
 * llamadas y se decide si se puede seguir consultando POIs, para que un gasto inesperado sea imposible.
 *
 * Un único límite configurable de llamadas (Places + Routes) por trayecto y por día. Al alcanzarlo, las consultas
 * de POIs se cortan en silencio (la navegación sigue: pedir o recalcular la ruta no se bloquea, solo se cuenta).
 * No sobrevive al proceso; los topes por trayecto se reinician con [startTrip].
 */
class ApiCallBudget(
    private val maxPerTrip: Int = DEFAULT_MAX_PER_TRIP,
    private val maxPerDay: Int = DEFAULT_MAX_PER_DAY,
    private val today: () -> LocalDate = LocalDate::now,
) {
    private val lock = Any()
    private var day: LocalDate = today()
    private var tripCount = 0
    private var dayCount = 0
    private var places = 0
    private var routes = 0

    /** Nuevo trayecto: reinicia el tope por trayecto (el del día y el total de la sesión siguen). */
    fun startTrip() = synchronized(lock) { tripCount = 0 }

    /** ¿Queda presupuesto para otra consulta de POIs? No la cuenta; solo mira los topes por trayecto y por día. */
    fun canQuery(): Boolean = synchronized(lock) {
        rollDay()
        tripCount < maxPerTrip && dayCount < maxPerDay
    }

    /** Registra una llamada ya hecha: cuenta para los topes y para el contador de depuración de la sesión. */
    fun record(kind: ApiKind) = synchronized(lock) {
        rollDay()
        tripCount++
        dayCount++
        when (kind) {
            ApiKind.PLACES -> places++
            ApiKind.ROUTES -> routes++
        }
    }

    /** Resumen para el log de depuración (contador de sesión y topes). */
    fun debugSummary(): String = synchronized(lock) {
        "Places=$places Routes=$routes (trip $tripCount/$maxPerTrip, day $dayCount/$maxPerDay)"
    }

    private fun rollDay() {
        val t = today()
        if (t != day) {
            day = t
            dayCount = 0
        }
    }

    companion object {
        /** Topes por defecto, holgados para una ruta + varios recálculos + consultas de POIs de un paseo. */
        const val DEFAULT_MAX_PER_TRIP = 40
        const val DEFAULT_MAX_PER_DAY = 200

        /** Sin tope: para los tests y repos existentes que solo cuentan para el total. */
        val UNLIMITED = ApiCallBudget(Int.MAX_VALUE, Int.MAX_VALUE)
    }
}
