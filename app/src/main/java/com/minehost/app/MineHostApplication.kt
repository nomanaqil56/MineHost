package com.minehost.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.minehost.app.core.logging.AppLogger
import com.minehost.app.domain.repository.ServerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MineHostApplication : Application() {

    companion object {
        const val SERVER_CHANNEL_ID = "minehost_server_channel"
        const val ALERT_CHANNEL_ID = "minehost_alert_channel"
    }

    @Inject
    lateinit var serverRepository: ServerRepository

    private val appScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        AppLogger.i("MineHostApplication", "MineHost Application initializing...")
        createNotificationChannels()

        // A freshly created process cannot have a running server: fix any status left over from a
        // previous process that was killed.
        appScope.launch {
            try {
                val fixed = serverRepository.reconcileAfterProcessRestart()
                if (fixed > 0) AppLogger.w("MineHostApplication", "Marked $fixed server(s) as CRASHED: the app process ended while they were active")
            } catch (e: Exception) {
                AppLogger.e("MineHostApplication", "Could not reconcile server states: ${e.message}", e)
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Server execution channel
            val serverChannel = NotificationChannel(
                SERVER_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
            }

            // Alert channel
            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "MineHost Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical alerts regarding thermal warnings and server status"
            }

            notificationManager?.createNotificationChannel(serverChannel)
            notificationManager?.createNotificationChannel(alertChannel)
        }
    }
}

