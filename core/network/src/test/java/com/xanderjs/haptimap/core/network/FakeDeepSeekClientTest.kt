package com.xanderjs.haptimap.core.network

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeDeepSeekClientTest {

    private val messages = listOf(ChatMessage(ChatRole.SYSTEM, "Eres un guía."), ChatMessage(ChatRole.USER, "¿Merece la pena el desvío?"))

    @Test
    fun `with a key it returns the configured answer and records the request`() = runTest {
        val client = FakeDeepSeekClient(ChatResult.Success("Sí, 2 min más."))
        assertEquals(ChatResult.Success("Sí, 2 min más."), client.complete(messages))
        assertEquals(1, client.requests)
        assertEquals(messages, client.lastMessages)
    }

    @Test
    fun `without a key it fails with MISSING_KEY and makes no request`() = runTest {
        val client = FakeDeepSeekClient(ChatResult.Success("no debería llegar"), hasApiKey = false)
        assertEquals(ChatResult.Failure(ChatError.MISSING_KEY), client.complete(messages))
        assertEquals(0, client.requests)
        assertNull(client.lastMessages)
    }

    @Test
    fun `service failures are returned as they are`() = runTest {
        val client = FakeDeepSeekClient(ChatResult.Failure(ChatError.API_QUOTA))
        assertEquals(ChatResult.Failure(ChatError.API_QUOTA), client.complete(messages))
    }

    @Test
    fun `the placeholder and a blank value count as no key, like the Google key`() {
        assertFalse(isApiKeyConfigured(PLACEHOLDER_API_KEY))
        assertFalse(isApiKeyConfigured(""))
        assertTrue(isApiKeyConfigured("sk-test"))
    }
}
