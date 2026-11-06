package com.xanderjs.haptimap.core.network

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiCallBudgetTest {

    @Test
    fun `queries are allowed until the per-trip limit is reached`() {
        val budget = ApiCallBudget(maxPerTrip = 3, maxPerDay = 100)
        repeat(3) {
            assertTrue(budget.canQuery())
            budget.record(ApiKind.PLACES)
        }
        assertFalse(budget.canQuery())
    }

    @Test
    fun `routes calls count against the shared budget too`() {
        val budget = ApiCallBudget(maxPerTrip = 2, maxPerDay = 100)
        budget.record(ApiKind.ROUTES)
        assertTrue(budget.canQuery())
        budget.record(ApiKind.ROUTES)
        // dos llamadas de rutas agotan el tope por trayecto: no quedan POIs
        assertFalse(budget.canQuery())
    }

    @Test
    fun `starting a new trip resets the per-trip count but not the day count`() {
        val budget = ApiCallBudget(maxPerTrip = 2, maxPerDay = 3)
        repeat(2) { budget.record(ApiKind.PLACES) }
        assertFalse(budget.canQuery())
        budget.startTrip()
        assertTrue(budget.canQuery()) // tope por trayecto reiniciado
        budget.record(ApiKind.PLACES)
        // 3 en el día alcanza el tope diario pese a reiniciar el trayecto
        assertFalse(budget.canQuery())
    }

    @Test
    fun `the day count resets when the date changes`() {
        var date = LocalDate.of(2026, 1, 1)
        val budget = ApiCallBudget(maxPerTrip = 100, maxPerDay = 2, today = { date })
        repeat(2) { budget.record(ApiKind.PLACES) }
        assertFalse(budget.canQuery())
        date = date.plusDays(1)
        assertTrue(budget.canQuery())
    }

    @Test
    fun `the debug summary counts places and routes separately`() {
        val budget = ApiCallBudget()
        budget.record(ApiKind.PLACES)
        budget.record(ApiKind.PLACES)
        budget.record(ApiKind.ROUTES)
        assertEquals("Places=2 Routes=1 (trip 3/40, day 3/200)", budget.debugSummary())
    }

    @Test
    fun `unlimited never blocks`() {
        repeat(10_000) { ApiCallBudget.UNLIMITED.record(ApiKind.PLACES) }
        assertTrue(ApiCallBudget.UNLIMITED.canQuery())
    }
}
