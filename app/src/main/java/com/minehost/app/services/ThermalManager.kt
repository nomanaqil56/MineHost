package com.minehost.app.services

import android.content.Context
import android.os.Build
import android.os.PowerManager
import com.minehost.app.core.logging.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class DeviceThermalState {
    NONE,
    LIGHT,
    MODERATE,
    SEVERE,
    CRITICAL,
    EMERGENCY,
    SHUTDOWN
}

@Singleton
class ThermalManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _thermalState = MutableStateFlow(DeviceThermalState.NONE)
    val thermalState: StateFlow<DeviceThermalState> = _thermalState.asStateFlow()

    private var listener: PowerManager.OnThermalStatusChangedListener? = null

    fun startListening() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
            _thermalState.value = mapThermalStatus(powerManager.currentThermalStatus)
            listener = PowerManager.OnThermalStatusChangedListener { status ->
                val state = mapThermalStatus(status)
                _thermalState.value = state
                if (state >= DeviceThermalState.SEVERE) {
                    AppLogger.w("ThermalManager", "High thermal status detected: $state")
                }
            }
            powerManager.addThermalStatusListener(listener!!)
        }
    }

    private fun mapThermalStatus(status: Int): DeviceThermalState = when (status) {
        PowerManager.THERMAL_STATUS_NONE -> DeviceThermalState.NONE
        PowerManager.THERMAL_STATUS_LIGHT -> DeviceThermalState.LIGHT
        PowerManager.THERMAL_STATUS_MODERATE -> DeviceThermalState.MODERATE
        PowerManager.THERMAL_STATUS_SEVERE -> DeviceThermalState.SEVERE
        PowerManager.THERMAL_STATUS_CRITICAL -> DeviceThermalState.CRITICAL
        PowerManager.THERMAL_STATUS_EMERGENCY -> DeviceThermalState.EMERGENCY
        PowerManager.THERMAL_STATUS_SHUTDOWN -> DeviceThermalState.SHUTDOWN
        else -> DeviceThermalState.NONE
    }

    fun stopListening() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && listener != null) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            powerManager?.removeThermalStatusListener(listener!!)
            listener = null
        }
    }
}

