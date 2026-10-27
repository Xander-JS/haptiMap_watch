package com.xanderjs.haptimap.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectivityPolicyTest {

    private val connected = Transport.entries.map { ConnectivityStatus.Connected(it) }

    @Test
    fun `every transport allows search, routes and POIs`() {
        connected.forEach {
            assertTrue("$it", ConnectivityPolicy.canSearch(it))
            assertTrue("$it", ConnectivityPolicy.canRequestRoute(it))
            assertTrue("$it", ConnectivityPolicy.canFetchPois(it))
        }
    }

    @Test
    fun `offline blocks search, new routes and new POIs`() {
        val offline = ConnectivityStatus.Disconnected
        assertFalse(ConnectivityPolicy.canSearch(offline))
        assertFalse(ConnectivityPolicy.canRequestRoute(offline))
        assertFalse(ConnectivityPolicy.canFetchPois(offline))
    }

    @Test
    fun `a loaded route continues with or without connection`() {
        (connected + ConnectivityStatus.Disconnected).forEach {
            assertTrue("$it", ConnectivityPolicy.canContinueLoadedRoute(it))
        }
    }
}
