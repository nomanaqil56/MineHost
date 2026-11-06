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
    val isDownloading: Boolean = false
)

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val runtimeManager: RuntimeManager,
    private val serverController: ServerController,
    private val tunnelManager: TunnelManager,
    val thermalManager: ThermalManager
) : ViewModel() {

    private val _downloadState = MutableStateFlow<Pair<Float?, String?>>(null to null)
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
            downloadProgress = download.first,
            downloadStatusMessage = download.second,
            isDownloading = downloading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DiagnosticsUiState()
    )

    fun downloadPocketMine() {
        viewModelScope.launch {
            _isDownloading.value = true
            runtimeManager.downloadPocketMinePhar { progress, msg ->
                _downloadState.value = progress to msg
            }
            _isDownloading.value = false
            _downloadState.value = null to null
        }
    }

    fun downloadPlayitAgent() {
        viewModelScope.launch {
            _isDownloading.value = true
            runtimeManager.downloadPlayitAgent { progress, msg ->
                _downloadState.value = progress to msg
            }
            _isDownloading.value = false
            _downloadState.value = null to null
        }
    }

    fun downloadPaper() {
        viewModelScope.launch {
            _isDownloading.value = true
            runtimeManager.downloadPaperJar("1.20.4") { progress, msg ->
                _downloadState.value = progress to msg
            }
            _isDownloading.value = false
            _downloadState.value = null to null
        }
    }
}
