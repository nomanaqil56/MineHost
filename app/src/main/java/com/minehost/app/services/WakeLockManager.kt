package com.minehost.app.services

import android.content.Context
import android.net.wifi.WifiManager
import android.os.PowerManager
import com.minehost.app.core.logging.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WakeLockManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    fun acquireLocks(tag: String = "MineHost:ServerWakeLock") {
        try {
            if (wakeLock == null) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag)?.apply {
                    setReferenceCounted(false)
                }
            }
            wakeLock?.acquire(12 * 60 * 60 * 1000L) // 12 hours max safety timeout
            AppLogger.i("WakeLockManager", "Partial WakeLock acquired")

            if (wifiLock == null) {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                wifiLock = wifiManager?.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, tag)?.apply {
                    setReferenceCounted(false)
                }
            }
            wifiLock?.acquire()
            AppLogger.i("WakeLockManager", "High-performance WiFi lock acquired")
        } catch (e: Exception) {
            AppLogger.e("WakeLockManager", "Failed to acquire locks: ${e.message}", e)
        }
    }

    fun releaseLocks() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                AppLogger.i("WakeLockManager", "WakeLock released")
            }
            if (wifiLock?.isHeld == true) {
                wifiLock?.release()
                AppLogger.i("WakeLockManager", "WiFi lock released")
            }
        } catch (e: Exception) {
            AppLogger.e("WakeLockManager", "Error releasing locks: ${e.message}", e)
        }
    }
}
