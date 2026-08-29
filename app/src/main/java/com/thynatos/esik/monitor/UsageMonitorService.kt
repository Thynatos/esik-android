package com.thynatos.esik.monitor

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.thynatos.esik.MainActivity
import com.thynatos.esik.R
import com.thynatos.esik.ai.AnthropicAiGateway
import com.thynatos.esik.data.JsonEsikRepository
import com.thynatos.esik.overlay.OverlayController
import com.thynatos.esik.usage.CooldownPolicy
import com.thynatos.esik.usage.UsageStatsReader
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class UsageMonitorService : Service() {
    private lateinit var repository: JsonEsikRepository
    private lateinit var usageStatsReader: UsageStatsReader
    private lateinit var overlayController: OverlayController
    private lateinit var executor: ScheduledExecutorService

    override fun onCreate() {
        super.onCreate()
        repository = JsonEsikRepository(this)
        usageStatsReader = UsageStatsReader(this)
        overlayController = OverlayController(this, repository, AnthropicAiGateway())
        executor = Executors.newSingleThreadScheduledExecutor()
        promoteToForeground()
        executor.scheduleAtFixedRate(
            ::pollSafely,
            INITIAL_DELAY_SECONDS,
            POLL_INTERVAL_SECONDS,
            TimeUnit.SECONDS,
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        if (::executor.isInitialized) executor.shutdownNow()
        if (::overlayController.isInitialized) overlayController.dismiss()
        super.onDestroy()
    }

    private fun pollSafely() {
        runCatching { poll() }
    }

    private fun poll() {
        val profile = repository.loadProfile() ?: return
        if (profile.targetPackage.isBlank()) return
        if (!usageStatsReader.hasUsageAccess()) return
        if (!Settings.canDrawOverlays(this)) return
        if (!isScreenAvailableForIntervention()) return
        if (overlayController.isShowing) return

        val foregroundPackage = usageStatsReader.currentForegroundPackage() ?: return
        if (foregroundPackage != profile.targetPackage) return

        val usageMinutes = usageStatsReader.todayUsageMinutes(profile.targetPackage)
        if (usageMinutes < profile.dailyLimitMinutes) return

        val preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
        val lastShown = preferences
            .getLong(KEY_LAST_SHOWN_AT, NO_TIMESTAMP)
            .takeUnless { it == NO_TIMESTAMP }
        val now = System.currentTimeMillis()
        if (!CooldownPolicy.shouldShow(now, lastShown)) return

        ContextCompat.getMainExecutor(this).execute {
            if (overlayController.show(profile, usageMinutes)) {
                preferences.edit().putLong(KEY_LAST_SHOWN_AT, now).apply()
            }
        }
    }

    private fun isScreenAvailableForIntervention(): Boolean {
        val powerManager = getSystemService(PowerManager::class.java)
        val keyguardManager = getSystemService(KeyguardManager::class.java)
        return powerManager.isInteractive && !keyguardManager.isKeyguardLocked
    }

    private fun promoteToForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.monitor_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(getString(R.string.monitor_notification_title))
            .setContentText(getString(R.string.monitor_notification_text))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val CHANNEL_ID = "esik_usage_monitor"
        private const val NOTIFICATION_ID = 1001
        private const val INITIAL_DELAY_SECONDS = 2L
        private const val POLL_INTERVAL_SECONDS = 60L
        private const val PREFERENCES_NAME = "esik_monitor"
        private const val KEY_LAST_SHOWN_AT = "last_overlay_at"
        private const val NO_TIMESTAMP = -1L

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, UsageMonitorService::class.java),
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, UsageMonitorService::class.java))
        }

        fun resetCooldown(context: Context) {
            context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_LAST_SHOWN_AT)
                .apply()
        }
    }
}
