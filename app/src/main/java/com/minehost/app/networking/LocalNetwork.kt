package com.minehost.app.networking

import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

/**
 * Finds the address other devices on the same Wi-Fi/LAN can use to reach the server.
 *
 * Being reachable on the LAN does NOT mean being reachable from the internet; that needs a tunnel or
 * router port forwarding.
 */
object LocalNetwork {

    /** One address on one interface, reduced to what matters for the choice. */
    data class Candidate(
        val interfaceName: String,
        val address: String,
        val isUp: Boolean,
        val isLoopback: Boolean,
        val isPointToPoint: Boolean,
        val isSiteLocal: Boolean,
        val isLinkLocal: Boolean
    )

    /**
     * Pure selection logic (unit-tested). Keeps IPv4 LAN-style addresses of interfaces that are up,
     * not loopback, not link-local (169.254.x.x) and not point-to-point (VPN/cellular tunnels), and
     * lists Wi-Fi style names (wlan/eth) before anything else.
     */
    fun pickLanAddresses(candidates: List<Candidate>): List<String> =
        candidates
            .filter { it.isUp && !it.isLoopback && !it.isLinkLocal && !it.isPointToPoint && it.isSiteLocal }
            .sortedBy { c ->
                when {
                    c.interfaceName.startsWith("wlan") || c.interfaceName.startsWith("eth") -> 0
                    c.interfaceName.startsWith("ap") || c.interfaceName.startsWith("swlan") -> 1
                    else -> 2
                }
            }
            .map { it.address }
            .distinct()

    /** Reads the device's interfaces. Returns an empty list when there is no LAN connection. */
    fun detect(): List<String> = try {
        val found = mutableListOf<Candidate>()
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces != null && interfaces.hasMoreElements()) {
            val nif = interfaces.nextElement()
            for (addr: InetAddress in nif.inetAddresses.toList()) {
                if (addr !is Inet4Address) continue
                found += Candidate(
                    interfaceName = nif.name.orEmpty(),
                    address = addr.hostAddress.orEmpty(),
                    isUp = nif.isUp,
                    isLoopback = nif.isLoopback || addr.isLoopbackAddress,
                    isPointToPoint = nif.isPointToPoint,
                    isSiteLocal = addr.isSiteLocalAddress,
                    isLinkLocal = addr.isLinkLocalAddress
                )
            }
        }
        pickLanAddresses(found)
    } catch (e: Exception) {
        emptyList()
    }
}
