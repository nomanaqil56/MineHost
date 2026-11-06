package com.minehost.app.networking

import android.util.Log
import com.minehost.app.domain.model.PublicEndpoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ConnectionMonitor"

@Singleton
class ConnectionMonitor @Inject constructor() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var monitorJob: Job? = null

    private val _latencyMs = MutableStateFlow<Long?>(null)
    val latencyMs: StateFlow<Long?> = _latencyMs.asStateFlow()

    fun startMonitoring(endpoint: PublicEndpoint) {
        stopMonitoring()
        monitorJob = scope.launch {
            while (isActive) {
                val ping = measureLatency(endpoint.hostname, endpoint.port)
                _latencyMs.value = ping
                delay(10_000L)
            }
        }
    }

    fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
        _latencyMs.value = null
    }

    private fun measureLatency(hostname: String, port: Int): Long? {
        return try {
            val start = System.currentTimeMillis()
            Socket().use { socket ->
                socket.connect(InetSocketAddress(hostname, port), 3000)
            }
            val elapsed = System.currentTimeMillis() - start
            Log.d(TAG, "Connection latency to $hostname:$port is ${elapsed}ms")
            elapsed
        } catch (e: Exception) {
            Log.d(TAG, "Failed to connect to $hostname:$port for latency check: ${e.message}")
            null
        }
    }
}

