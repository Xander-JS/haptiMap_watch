package com.xanderjs.haptimap.core.intelligence

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RejectedPoiMemoryTest {

    @Test
    fun `reject and contains and clear work`() {
        val memory = RejectedPoiMemory()
        assertFalse("a" in memory)
        memory.reject("a")
        assertTrue("a" in memory)
        memory.clear()
        assertFalse("a" in memory)
    }

    /**
     * Varias corrutinas rechazan ids distintos y otra lee a la vez (paso 19: la tubería evalúa mientras el
     * controlador rechaza): sin sincronizar, un HashSet lanzaría ConcurrentModificationException o perdería ids.
     */
    @Test
    fun `concurrent rejects from many coroutines all land and no reader crashes`() = runBlocking {
        val memory = RejectedPoiMemory()
        val ids = (0 until 2_000).map { "poi-$it" }

        val writers = ids.map { id ->
            async(Dispatchers.Default) { memory.reject(id) }
        }
        // lectores concurrentes recorriendo el set mientras crece: no deben lanzar
        val readers = (0 until 8).map {
            async(Dispatchers.Default) { repeat(2_000) { i -> memory.contains("poi-$i") } }
        }
        (writers + readers).awaitAll()

        assertEquals(ids, ids.filter { it in memory })
    }

    @Test
    fun `clear racing with rejects leaves a consistent set without crashing`() = runBlocking {
        val memory = RejectedPoiMemory()
        val writers = (0 until 1_000).map { i ->
            async(Dispatchers.Default) { memory.reject("poi-$i") }
        }
        val clearer = async(Dispatchers.Default) { repeat(1_000) { memory.clear() } }
        (writers + clearer).awaitAll()
        // no se comprueba el contenido (la carrera lo deja indeterminado), solo que nada lanzó
        memory.reject("final")
        assertTrue("final" in memory)
    }
}
