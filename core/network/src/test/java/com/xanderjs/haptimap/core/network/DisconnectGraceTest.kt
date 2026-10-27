package com.xanderjs.haptimap.core.network

import com.xanderjs.haptimap.core.model.ConnectivityStatus.Connected
import com.xanderjs.haptimap.core.model.ConnectivityStatus.Disconnected
import com.xanderjs.haptimap.core.model.ConnectivityStatus
import com.xanderjs.haptimap.core.model.Transport
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DisconnectGraceTest {

    private val wifi = Connected(Transport.WIFI)
    private val bluetooth = Connected(Transport.BLUETOOTH)

    private val upstream = MutableSharedFlow<ConnectivityStatus>()

    /** Recolecta la salida del operador mientras dure el test (tiempo virtual). */
    private fun TestScope.collectOutput(): List<ConnectivityStatus> {
        val out = mutableListOf<ConnectivityStatus>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { upstream.withDisconnectGrace().collect { out += it } }
        return out
    }

    private fun TestScope.wait(d: Duration) { advanceTimeBy(d); runCurrent() }

    @Test
    fun `first value is emitted immediately, even when offline`() = runTest {
        val out = collectOutput()
        upstream.emit(Disconnected)
        assertEquals(listOf(Disconnected), out)
    }

    @Test
    fun `disconnect is reported only after the grace period`() = runTest {
        val out = collectOutput()
        upstream.emit(wifi)
        upstream.emit(Disconnected)
        wait(DISCONNECT_GRACE - 1.milliseconds)
        assertEquals(listOf(wifi), out)
        wait(1.milliseconds)
        assertEquals(listOf(wifi, Disconnected), out)
    }

    @Test
    fun `flicker shorter than the grace period is not reported`() = runTest {
        val out = collectOutput()
        upstream.emit(wifi)
        upstream.emit(Disconnected)
        wait(DISCONNECT_GRACE - 1.milliseconds)
        upstream.emit(wifi)
        wait(DISCONNECT_GRACE * 2)
        assertEquals(listOf(wifi), out)
    }

    @Test
    fun `reconnection is reported immediately, including a transport switch`() = runTest {
        val out = collectOutput()
        upstream.emit(wifi)
        upstream.emit(Disconnected)
        wait(DISCONNECT_GRACE)
        upstream.emit(bluetooth)
        assertEquals(listOf(wifi, Disconnected, bluetooth), out)
    }
}
