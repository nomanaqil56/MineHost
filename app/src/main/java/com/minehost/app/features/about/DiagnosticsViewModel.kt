package com.minehost.app.features.about

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minehost.app.core.common.ServerStatus
import com.minehost.app.core.common.TunnelStatus
import com.minehost.app.networking.TunnelManager
import com.minehost.app.server.RuntimeManager
import com.minehost.app.server.ServerController
import com.minehost.app.services.DeviceThermalState
import com.minehost.app.services.ThermalManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiagnosticsUiState(
    val deviceArch: String = "unknown",
    val is64BitSupported: Boolean = false,
    val androidVersion: String = "",
    val totalRamMb: Long = 0L,
    val availRamMb: Long = 0L,
    val freeStorageGb: Long = 0L,
    val isPocketMineInstalled: Boolean = false,
    val isPaperInstalled: Boolean = false,
    val isPlayitInstalled: Boolean = false,
    val serverStatus: ServerStatus = ServerStatus.STOPPED,
    val tunnelStatus: TunnelStatus = TunnelStatus.DISCONNECTED,
    val thermalState: DeviceThermalState = DeviceThermalState.NONE,
    val downloadProgress: Float? = null,
    val downloadStatusMessage: String? = null,
    val isDownloading: Boolean = false,
    /** Why the last download/install failed, or null. Never swallowed. */
    val downloadError: String? = null,
    /** libphp.so (or another PHP) found. Without it a Bedrock server cannot start. */
    val isPhpRuntimeAvailable: Boolean = false,
    /** libjava.so found. Without it a Java server cannot start. */
    val isJavaRuntimeAvailable: Boolean = false
)

private data class DownloadState(
    val progress: Float? = null,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val runtimeManager: RuntimeManager,
    private val serverController: ServerController,
    private val tunnelManager: TunnelManager,
    val thermalManager: ThermalManager
) : ViewModel() {

    private val _downloadState = MutableStateFlow(DownloadState())
    private val _isDownloading = MutableStateFlow(false)

    val uiState: StateFlow<DiagnosticsUiState> = combine(
        serverController.serverStatus,
        tunnelManager.tunnelStatus,
        thermalManager.thermalState,
        _downloadState,
        _isDownloading
    ) { status, tunnel, thermal, download, downloading ->
        val memInfo = ActivityManager.MemoryInfo()
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        am?.getMemoryInfo(memInfo)

        val stat = StatFs(context.filesDir.absolutePath)
        val freeBytes = stat.availableBlocksLong * stat.blockSizeLong

        DiagnosticsUiState(
            deviceArch = runtimeManager.getDeviceArchitecture(),
            is64BitSupported = runtimeManager.isSupportedArchitecture(),
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            totalRamMb = memInfo.totalMem / (1024 * 1024),
            availRamMb = memInfo.availMem / (1024 * 1024),
            freeStorageGb = freeBytes / (1024 * 1024 * 1024),
            isPocketMineInstalled = runtimeManager.isBedrockPocketMineInstalled(),
            isPaperInstalled = runtimeManager.isPaperJavaInstalled(),
            isPlayitInstalled = runtimeManager.isPlayitAgentInstalled(),
            serverStatus = status,
            tunnelStatus = tunnel,
            thermalState = thermal,
            downloadProgress = download.progress,
            downloadStatusMessage = download.message,
            isDownloading = downloading,
            downloadError = download.error,
            isPhpRuntimeAvailable = runtimeManager.getPhpExecutable() != null,
            isJavaRuntimeAvailable = java.io.File(context.applicationInfo.nativeLibraryDir, "libjava.so").exists()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DiagnosticsUiState()
    )

    /** Runs one install at a time and reports the real outcome, including failures. */
    private fun runDownload(label: String, block: suspend ((Float, String) -> Unit) -> Result<*>) {
        if (_isDownloading.value) return
        viewModelScope.launch {
            _isDownloading.value = true
            _downloadState.value = DownloadState()
            val result: Result<*> = try {
                block { progress, msg -> _downloadState.value = DownloadState(progress, msg) }
            } catch (e: Exception) {
                Result.failure<Unit>(e)
            }
            _isDownloading.value = false
            _downloadState.value = DownloadState(
                error = result.exceptionOrNull()?.let { "$label failed: ${it.message ?: it.javaClass.simpleName}" }
            )
        }
    }

    fun downloadPocketMine() = runDownload("PocketMine-MP download") { cb -> runtimeManager.downloadPocketMinePhar(cb) }

    fun downloadPlayitAgent() = runDownload("Playit agent install") { cb -> runtimeManager.downloadPlayitAgent(cb) }

    fun downloadPaper() = runDownload("Paper download") { cb -> runtimeManager.downloadPaperJar("1.20.4", cb) }
}
