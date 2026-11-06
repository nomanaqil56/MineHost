package com.minehost.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.minehost.app.core.logging.AppLogger
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MineHostApplication : Application() {

    companion object {
        const val SERVER_CHANNEL_ID = "minehost_server_channel"
        const val ALERT_CHANNEL_ID = "minehost_alert_channel"
    }

    override fun onCreate() {
        super.onCreate()
        AppLogger.i("MineHostApplication", "MineHost Application initializing...")
        createNotificationChannels()
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
