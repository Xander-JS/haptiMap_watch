package com.xanderjs.haptimap.core.model

import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SlowLoadTest {

    @Test
    fun `a load faster than the threshold never signals slow`() = runTest {
        var slow = false
        val result = withSlowNotice(onSlow = { slow = true }) {
            delay(SLOW_LOAD_THRESHOLD - 1.milliseconds)
            42
        }
        assertEquals(42, result)
        assertFalse(slow)
    }

    @Test
    fun `a load slower than the threshold signals slow once`() = runTest {
        var signals = 0
        withSlowNotice(onSlow = { signals++ }) { delay(SLOW_LOAD_THRESHOLD * 3) }
        assertEquals(1, signals)
    }

    @Test
    fun `a failing load cancels the signal and rethrows`() = runTest {
        var slow = false
        val error = runCatching { withSlowNotice(onSlow = { slow = true }) { error("boom") } }.exceptionOrNull()
        delay(SLOW_LOAD_THRESHOLD * 2)
        assertTrue(error is IllegalStateException)
        assertFalse(slow)
    }
}
