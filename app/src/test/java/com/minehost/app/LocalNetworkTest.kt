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
    fun picksWifiAndIgnoresLoopbackVpnAndCellular() {
        val result = LocalNetwork.pickLanAddresses(
            listOf(
                c("lo", "127.0.0.1", loopback = true, siteLocal = false),
                c("rmnet0", "10.64.3.9"),                              // cellular private address
                c("tun0", "10.8.0.2", p2p = true),                     // VPN
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
    fun prioritizesWifiThenHotspotThenEthernet() {
        val result = LocalNetwork.pickLanAddresses(
            listOf(
                c("eth0", "10.1.1.1"),
                c("ap0", "192.168.43.1"),
                c("wlan0", "192.168.1.23")
            )
        )
        assertEquals(listOf("192.168.1.23", "192.168.43.1", "10.1.1.1"), result)
    }

    @Test
    fun rejectsUnknownAndCellularInterfacesEvenWhenAddressIsPrivate() {
        val result = LocalNetwork.pickLanAddresses(
            listOf(
                c("rmnet_data0", "10.12.0.5"),
                c("dummy0", "10.1.1.1"),
                c("tun0", "192.168.8.2")
            )
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun supportsEthernetAndUsbTetherInterfaces() {
        val result = LocalNetwork.pickLanAddresses(
            listOf(c("eth0", "192.168.10.2"), c("rndis0", "192.168.42.2"))
        )
        assertEquals(listOf("192.168.10.2", "192.168.42.2"), result)
    }

    @Test
    fun removesDuplicateAddresses() {
        assertEquals(
            listOf("192.168.1.5"),
            LocalNetwork.pickLanAddresses(listOf(c("wlan0", "192.168.1.5"), c("wlan1", "192.168.1.5")))
        )
    }

    @Test
    fun emptyInputGivesEmptyResult() {
        assertTrue(LocalNetwork.pickLanAddresses(emptyList()).isEmpty())
    }
}
