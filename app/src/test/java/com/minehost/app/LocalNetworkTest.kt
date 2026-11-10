package com.minehost.app

import com.minehost.app.networking.LocalNetwork
import com.minehost.app.networking.LocalNetwork.Candidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalNetworkTest {

    private fun c(
        name: String,
        addr: String,
        up: Boolean = true,
        loopback: Boolean = false,
        p2p: Boolean = false,
        siteLocal: Boolean = true,
        linkLocal: Boolean = false
    ) = Candidate(name, addr, up, loopback, p2p, siteLocal, linkLocal)

    @Test
    fun picksTheWifiAddressAndIgnoresLoopbackVpnAndCellular() {
        val result = LocalNetwork.pickLanAddresses(
            listOf(
                c("lo", "127.0.0.1", loopback = true, siteLocal = false),
                c("rmnet0", "100.64.3.9", siteLocal = false),            // carrier-grade NAT, not a LAN
                c("tun0", "10.8.0.2", p2p = true),                       // VPN
                c("wlan0", "192.168.1.23")
            )
        )
        assertEquals(listOf("192.168.1.23"), result)
    }

    @Test
    fun ignoresInterfacesThatAreDownAndLinkLocalAddresses() {
        val result = LocalNetwork.pickLanAddresses(
            listOf(
                c("wlan0", "192.168.1.23", up = false),
                c("eth0", "169.254.10.4", linkLocal = true, siteLocal = false)
            )
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun wifiComesBeforeHotspotAndOthers() {
        val result = LocalNetwork.pickLanAddresses(
            listOf(
                c("dummy0", "10.1.1.1"),
                c("ap0", "192.168.43.1"),
                c("wlan0", "192.168.1.23")
            )
        )
        assertEquals(listOf("192.168.1.23", "192.168.43.1", "10.1.1.1"), result)
    }

    @Test
    fun noDuplicatesAndEmptyInputGivesEmptyResult() {
        assertEquals(listOf("192.168.1.5"), LocalNetwork.pickLanAddresses(listOf(c("wlan0", "192.168.1.5"), c("wlan1", "192.168.1.5"))))
        assertTrue(LocalNetwork.pickLanAddresses(emptyList()).isEmpty())
    }
}
