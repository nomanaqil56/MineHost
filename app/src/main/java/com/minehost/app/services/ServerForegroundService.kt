package com.minehost.app.services

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.minehost.app.core.logging.AppLogger
import com.minehost.app.server.ServerController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ServerForegroundService : Service() {

    companion object {
        const val EXTRA_SERVER_NAME = "extra_server_name"
        const val EXTRA_PORT = "extra_port"
        const val EXTRA_PUBLIC_ENDPOINT = "extra_public_endpoint"
    }

    @Inject
    lateinit var notificationManager: ServerNotificationManager

    @Inject
    lateinit var serverController: ServerController

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        AppLogger.i("ServerForegroundService", "Server Foreground Service created, establishing foreground immediately")
        val initialNotification = notificationManager.buildForegroundNotification("Minecraft Server", 19132, null)
        promoteToForeground(initialNotification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ServerNotificationManager.ACTION_STOP_SERVER) {
            AppLogger.i("ServerForegroundService", "Stop action triggered from notification")
            serverController.activeServerId.value?.let { serverId ->
                scope.launch {
                    serverController.stopServer(serverId)
                    stopSelf()
                }
            }
            return START_NOT_STICKY
        }

        val serverName = intent?.getStringExtra(EXTRA_SERVER_NAME) ?: "Minecraft Server"
        val port = intent?.getIntExtra(EXTRA_PORT, 19132) ?: 19132
        val publicEndpoint = intent?.getStringExtra(EXTRA_PUBLIC_ENDPOINT)

        val notification = notificationManager.buildForegroundNotification(serverName, port, publicEndpoint)
        promoteToForeground(notification)

        // NOT sticky: if Android kills the app the server process dies with it, and a restarted
        // service would show a "server running" notification for a server that no longer exists.
        return START_NOT_STICKY
    }

    private fun promoteToForeground(notification: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    ServerNotificationManager.NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    ServerNotificationManager.NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(ServerNotificationManager.NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            AppLogger.e("ServerForegroundService", "Failed to start foreground with declared type: ${e.message}", e)
            try {
                startForeground(ServerNotificationManager.NOTIFICATION_ID, notification)
            } catch (fallbackEx: Exception) {
                AppLogger.e("ServerForegroundService", "Fallback startForeground failed: ${fallbackEx.message}", fallbackEx)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AppLogger.i("ServerForegroundService", "Server Foreground Service destroyed")
    }
}
