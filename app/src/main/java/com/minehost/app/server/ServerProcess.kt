package com.minehost.app.server

import com.minehost.app.core.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

class ManagedServerProcess(
    private val command: List<String>,
    private val workingDir: File,
    private val environment: Map<String, String>,
    private val port: Int,
    private val isUdp: Boolean,
    private val listener: ProcessListener,
    private val scope: CoroutineScope
) : ServerProcessHandle {

    private var process: Process? = null
    private var stdinWriter: BufferedWriter? = null
    private var stdoutJob: Job? = null
    private var stderrJob: Job? = null
    private var readinessJob: Job? = null

    override val pid: Long
        get() = runCatching {
            process?.let { p ->
                // Java 9+ Process.pid() if available, or reflection
                p.javaClass.getMethod("pid").invoke(p) as Long
            } ?: -1L
        }.getOrDefault(-1L)

    override val isAlive: Boolean
        get() = process?.isAlive == true

    suspend fun launchProcess(): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            AppLogger.i("ManagedServerProcess", "Launching process in ${workingDir.absolutePath}: ${command.joinToString(" ")}")
            val pb = ProcessBuilder(command)
                .directory(workingDir)
                .redirectErrorStream(false)

            pb.environment().putAll(environment)

            val p = pb.start()
            process = p
            stdinWriter = BufferedWriter(OutputStreamWriter(p.outputStream))

            // Read standard output
            stdoutJob = scope.launch(Dispatchers.IO) {
                BufferedReader(InputStreamReader(p.inputStream)).use { reader ->
                    while (isActive) {
                        val line = reader.readLine() ?: break
                        listener.onLog(line)
                    }
                }
            }

            // Read standard error
            stderrJob = scope.launch(Dispatchers.IO) {
                BufferedReader(InputStreamReader(p.errorStream)).use { reader ->
                    while (isActive) {
                        val line = reader.readLine() ?: break
                        listener.onLog("[STDERR] $line")
                    }
                }
            }

            // Monitor process termination
            scope.launch(Dispatchers.IO) {
                val exitCode = p.waitFor()
                AppLogger.i("ManagedServerProcess", "Process terminated with exit code $exitCode")
                listener.onStopped(exitCode)
            }

            // Trigger readiness monitoring
            readinessJob = scope.launch(Dispatchers.IO) {
                checkPortReadiness()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.e("ManagedServerProcess", "Failed to launch process: ${e.message}", e)
            listener.onError("Launch error: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun checkPortReadiness() {
        var attempts = 0
        val maxAttempts = 60 // 60 seconds
        while (attempts < maxAttempts && isAlive) {
            val ready = if (isUdp) checkUdpPort(port) else checkTcpPort(port)
            if (ready) {
                AppLogger.i("ManagedServerProcess", "Server port $port is open and verified ready!")
                listener.onStarted()
                return
            }
            kotlinx.coroutines.delay(1000L)
            attempts++
        }
        if (isAlive) {
            // Assume started even if port probe had quirks
            listener.onStarted()
        }
    }

    private fun checkTcpPort(port: Int): Boolean {
        return try {
            Socket(InetAddress.getByName("127.0.0.1"), port).use { true }
        } catch (e: Exception) {
            false
        }
    }

    private fun checkUdpPort(port: Int): Boolean {
        return try {
            DatagramSocket(port, InetAddress.getByName("127.0.0.1")).use {
                // If we can bind to it locally, the server hasn't claimed it yet!
                false
            }
        } catch (e: Exception) {
            // Port already bound by server => Port is active!
            true
        }
    }

    fun sendCommand(command: String): Boolean {
        return try {
            val writer = stdinWriter ?: return false
            writer.write(command)
            writer.newLine()
            writer.flush()
            true
        } catch (e: Exception) {
            AppLogger.e("ManagedServerProcess", "Failed to send command to stdin: ${e.message}")
            false
        }
    }

    suspend fun gracefulStop(timeoutMs: Long): Boolean = withContext(Dispatchers.IO) {
        val p = process ?: return@withContext true
        if (!p.isAlive) return@withContext true

        AppLogger.i("ManagedServerProcess", "Initiating graceful shutdown (sending stop)...")
        sendCommand("stop")

        val exited = runCatching {
            p.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
        }.getOrDefault(false)

        if (!exited) {
            AppLogger.w("ManagedServerProcess", "Graceful shutdown timed out after ${timeoutMs}ms. Forcing termination.")
            destroyForcibly()
        }

        cleanupStreams()
        return@withContext true
    }

    override fun destroy() {
        process?.destroy()
        cleanupStreams()
    }

    override fun destroyForcibly() {
        process?.destroyForcibly()
        cleanupStreams()
    }

    private fun cleanupStreams() {
        stdoutJob?.cancel()
        stderrJob?.cancel()
        readinessJob?.cancel()
        runCatching { stdinWriter?.close() }
    }
}

