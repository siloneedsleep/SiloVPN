package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import android.content.pm.ServiceInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AutoVpnService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var isVpnActive = false

    companion object {
        const val CHANNEL_ID = "AutoVpnServiceChannel"
        const val NOTIFICATION_ID = 101
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        VpnTunnelManager.initBackend(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification("Silo VPN đang theo dõi ứng dụng...")
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) 
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE 
            else 0
        )

        serviceScope.launch {
            // Whitelist self
            ShizukuHelper.whitelistApp(packageName)
            val prefs = AppPreferences(applicationContext)

            while (isActive) {
                try {
                    val foregroundApp = ShizukuHelper.getForegroundPackageName()
                    val targetAppsStr = prefs.targetApps.first()
                    val targetApps = targetAppsStr.split(",").filter { it.isNotBlank() }
                    val vpnConfig = prefs.vpnConfig.first()

                    if (foregroundApp != null && targetApps.contains(foregroundApp)) {
                        if (!isVpnActive && vpnConfig.isNotBlank()) {
                            val success = VpnTunnelManager.turnOn(vpnConfig)
                            if (success) {
                                isVpnActive = true
                                updateNotification("VPN đang bật: $foregroundApp")
                            }
                        }
                    } else {
                        if (isVpnActive) {
                            val success = VpnTunnelManager.turnOff()
                            if (success) {
                                isVpnActive = false
                                updateNotification("Silo VPN đang theo dõi...")
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(500)
            }
        }
        return Service.START_STICKY
    }

    private fun createNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Silo VPN")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_secure)
            .setContentIntent(pendingIntent)
            .build()
    }
    
    private fun updateNotification(contentText: String) {
        val notification = createNotification(contentText)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Auto VPN Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.launch {
            if (isVpnActive) {
                VpnTunnelManager.turnOff()
            }
        }
    }
}
