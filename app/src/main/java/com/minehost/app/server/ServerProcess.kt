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
import java.util.concurrent.TimeUnit

private const val LOCALHOST = "127.0.0.1"

class ManagedServerProcess(
    private val command: List<String>,
    private val workingDir: File,
    private val environment: Map<String, String>,
    private val port: Int,
    private val isUdp: Boolean,
    private val listener: ProcessListener,
    private val scope: CoroutineScope,
    private val startupTimeoutMs: Long = 180_000L
) : ServerProcessHandle {

    /** True once the user (or timeout logic) asked the process to stop; exit is then expected. */
    @Volatile private var stopRequested = false

    /** True when startup failed and the error was already reported; suppress the follow-up exit event. */
    @Volatile private var startupFailureReported = false

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
                readinessJob?.cancel()
                if (!startupFailureReported) {
                    // A requested stop is a clean shutdown even though the OS reports a signal exit code.
                    listener.onStopped(if (stopRequested) 0 else exitCode)
                }
            }

            // Trigger readiness monitoring
            readinessJob = scope.launch(Dispatchers.IO) {
                awaitReadiness()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.e("ManagedServerProcess", "Failed to launch process: ${e.message}", e)
            listener.onError("Launch error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * The server is reported ready ONLY after it answers a genuine Minecraft protocol probe
     * (NetherNet capability probe with legacy RakNet fallback for Bedrock, Server List Ping for Java). If the process stays alive but never
     * answers within [startupTimeoutMs], startup is reported as a failure and the process is killed.
     */
    private suspend fun awaitReadiness() {
        val protocolName = if (isUdp) "Bedrock (NetherNet HTTP signaling, RakNet fallback)" else "Java (Server List Ping/TCP)"
        val deadline = System.currentTimeMillis() + startupTimeoutMs
        while (isAlive && System.currentTimeMillis() < deadline) {
            val ready = if (isUdp) {
                ReadinessProbe.probeBedrock(LOCALHOST, port)
            } else {
                ReadinessProbe.probeJava(LOCALHOST, port)
            }
            if (ready) {
                AppLogger.i("ManagedServerProcess", "Server answered $protocolName probe on port $port")
                listener.onStarted()
                return
            }
            kotlinx.coroutines.delay(1000L)
        }
        if (isAlive) {
            startupFailureReported = true
            stopRequested = true
            val seconds = startupTimeoutMs / 1000
            listener.onError(
                "Server process is running but did not answer a $protocolName status probe on port $port " +
                    "within ${seconds}s, so it was stopped. Check the console for startup errors."
            )
            process?.destroyForcibly()
            cleanupStreams()
        }
        // If the process is no longer alive, the exit monitor reports the real exit code.
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
        stopRequested = true
        sendCommand("stop")

        var exited = runCatching {
            p.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
        }.getOrDefault(false)

        if (!exited) {
            AppLogger.w("ManagedServerProcess", "Graceful shutdown timed out after ${timeoutMs}ms. Forcing termination.")
            destroyForcibly()
            // Do not report a successful stop merely because destroyForcibly() was
            // requested. Wait briefly for the OS process to actually terminate.
            exited = runCatching { p.waitFor(2_000L, TimeUnit.MILLISECONDS) }.getOrDefault(false)
        }

        cleanupStreams()
        return@withContext exited
    }

    override fun destroy() {
        stopRequested = true
        process?.destroy()
        cleanupStreams()
    }

    override fun destroyForcibly() {
        stopRequested = true
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

