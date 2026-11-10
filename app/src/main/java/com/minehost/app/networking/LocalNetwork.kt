package com.minehost.app.networking

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.NetworkCapabilities
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

/**
 * Finds IPv4 addresses other devices on the same Wi-Fi/Ethernet LAN can use.
 *
 * A private/carrier-grade-NAT cellular address is not a local multiplayer address.
 * Being reachable on the LAN does NOT imply internet reachability; that needs a
 * public route, port forwarding, or a supported tunnel.
 */
object LocalNetwork {

    data class Candidate(
        val interfaceName: String,
        val address: String,
        val isUp: Boolean,
        val isLoopback: Boolean,
        val isPointToPoint: Boolean,
        val isSiteLocal: Boolean,
        val isLinkLocal: Boolean
    )

    private fun isLanInterface(name: String): Boolean {
        val n = name.lowercase()
        return n.startsWith("wlan") || n.startsWith("wifi") ||
            n.startsWith("eth") || n.startsWith("en") ||
            n.startsWith("ap") || n.startsWith("swlan") ||
            n.startsWith("br-lan") || n.startsWith("rndis")
    }

    private fun interfacePriority(name: String): Int {
        val n = name.lowercase()
        return when {
            n.startsWith("wlan") || n.startsWith("wifi") -> 0
            n.startsWith("ap") || n.startsWith("swlan") -> 1
            else -> 2
        }
    }

    /** Pure selection logic: only known Wi-Fi, hotspot, or Ethernet interfaces qualify. */
    fun pickLanAddresses(candidates: List<Candidate>): List<String> =
        candidates
            .filter {
                isLanInterface(it.interfaceName) && it.isUp && !it.isLoopback &&
                    !it.isLinkLocal && !it.isPointToPoint && it.isSiteLocal &&
                    it.address.isNotBlank()
            }
            .sortedBy { interfacePriority(it.interfaceName) }
            .map { it.address }
            .distinct()

    /**
     * Prefer LinkProperties from the active Wi-Fi/Ethernet network. If the active
     * network is cellular or a VPN, do not mistake its private address for a LAN
     * address; enumerate only recognized LAN interfaces as a fallback.
     */
    fun detect(context: Context? = null): List<String> {
        val activeNetworkAddresses = runCatching {
            val connectivity = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = connectivity?.activeNetwork
            val capabilities = activeNetwork?.let { connectivity.getNetworkCapabilities(it) }
            val isLanTransport = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true
            if (!isLanTransport) return@runCatching emptyList()

            val properties: LinkProperties? = activeNetwork?.let { connectivity.getLinkProperties(it) }
            val interfaceName = properties?.interfaceName.orEmpty()
            if (!isLanInterface(interfaceName)) return@runCatching emptyList()

            properties?.linkAddresses.orEmpty()
                .mapNotNull { it.address as? Inet4Address }
                .filter { it.isSiteLocalAddress && !it.isLoopbackAddress && !it.isLinkLocalAddress }
                .mapNotNull { address -> address.hostAddress?.takeIf { it.isNotBlank() } }
                .distinct()
                .sortedBy { interfacePriority(interfaceName) }
        }.getOrDefault(emptyList())
        if (activeNetworkAddresses.isNotEmpty()) return activeNetworkAddresses

        return try {
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
        } catch (_: Exception) {
            emptyList()
        }
    }
}
