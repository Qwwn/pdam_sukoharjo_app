package com.metromultindo.pdam_app_v2.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.metromultindo.pdam_app_v2.R

/**
 * Helper untuk membuat notification channel dan mengecek status notifikasi
 * Konsisten untuk semua kondisi aplikasi (foreground, background, killed)
 */
object NotificationHelperFCM {

    private const val TAG = "NotificationHelperFCM"

    const val CHANNEL_ID = "pdam_news_channel"
    const val CHANNEL_NAME = "PDAM News"
    const val CHANNEL_DESCRIPTION = "Notifikasi berita dan pengumuman PDAM"

    // Notification configuration constants
    private const val IMPORTANCE_LEVEL = NotificationManager.IMPORTANCE_HIGH
    private const val LIGHT_COLOR = 0xFF0088FF.toInt()

    // Vibration pattern - tidak bisa const karena bukan primitive type
    private val VIBRATION_PATTERN = longArrayOf(0, 250, 250, 250)

    /**
     * Buat notification channel dengan konfigurasi yang konsisten
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Check if channel already exists
            val existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (existingChannel != null) {
                Log.d(TAG, "Notification channel already exists: $CHANNEL_ID")
                return
            }

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                IMPORTANCE_LEVEL,
            ).apply {
                description = CHANNEL_DESCRIPTION

                // Vibration settings
                enableVibration(true)
                vibrationPattern = VIBRATION_PATTERN

                // LED light settings
                enableLights(true)
                lightColor = LIGHT_COLOR

                // Badge settings
                setShowBadge(true)

                // Lock screen visibility
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC

                // Sound settings (use default)
                setSound(null, null) // Use default notification sound
            }

            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created successfully: $CHANNEL_ID")

            // Log channel details untuk debugging
            logChannelDetails(context)
        } else {
            Log.d(TAG, "Notification channel creation skipped (API < 26)")
        }
    }

    /**
     * Check apakah notifikasi diizinkan dan channel aktif
     */
    fun areNotificationsEnabled(context: Context): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val globalEnabled = notificationManager.areNotificationsEnabled()
            val channel = notificationManager.getNotificationChannel(CHANNEL_ID)
            val channelEnabled = channel?.importance != NotificationManager.IMPORTANCE_NONE

            val result = globalEnabled && channelEnabled
            Log.d(TAG, "Notifications check - Global: $globalEnabled, Channel: $channelEnabled, Result: $result")
            result
        } else {
            val result = notificationManager.areNotificationsEnabled()
            Log.d(TAG, "Notifications enabled (API < 26): $result")
            result
        }
    }

    /**
     * Get notification channel status untuk debugging
     */
    fun getChannelStatus(context: Context): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = notificationManager.getNotificationChannel(CHANNEL_ID)

            if (channel != null) {
                """
                Channel Status:
                - ID: ${channel.id}
                - Name: ${channel.name}
                - Description: ${channel.description}
                - Importance: ${channel.importance} (${getImportanceDescription(channel.importance)})
                - Vibration: ${channel.shouldVibrate()}
                - Lights: ${channel.shouldShowLights()}
                - Badge: ${channel.canShowBadge()}
                - Sound: ${channel.sound}
                - Bypass DND: ${channel.canBypassDnd()}
                """.trimIndent()
            } else {
                "Channel not found: $CHANNEL_ID"
            }
        } else {
            "Channel status not available (API < 26)"
        }
    }

    /**
     * Log channel details untuk debugging
     */
    fun logChannelDetails(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val status = getChannelStatus(context)
            Log.d(TAG, "=== Notification Channel Details ===\n$status")
        }
    }

    /**
     * Check dan log notification permissions
     */
    fun checkAndLogPermissions(context: Context) {
        val enabled = areNotificationsEnabled(context)
        Log.d(TAG, "=== Notification Permissions ===")
        Log.d(TAG, "Notifications enabled: $enabled")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            Log.d(TAG, "Global notifications enabled: ${notificationManager.areNotificationsEnabled()}")

            val channel = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (channel != null) {
                Log.d(TAG, "Channel importance: ${channel.importance}")
                Log.d(TAG, "Channel enabled: ${channel.importance != NotificationManager.IMPORTANCE_NONE}")
            }
        }

        Log.d(TAG, "=== End Permissions Check ===")
    }

    /**
     * Get default notification builder dengan konfigurasi konsisten
     */
    fun getDefaultNotificationBuilder(context: Context): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_fore) // Icon aplikasi yang konsisten
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // For Android < 8.0
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVibrate(VIBRATION_PATTERN)
            .setLights(LIGHT_COLOR, 300, 300)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    }

    /**
     * Create test notification untuk debugging
     */
    fun createTestNotification(context: Context, title: String = "Test Notification", body: String = "This is a test notification") {
        if (!areNotificationsEnabled(context)) {
            Log.w(TAG, "Cannot create test notification - notifications not enabled")
            return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = getDefaultNotificationBuilder(context)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .build()

        val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        notificationManager.notify(notificationId, notification)

        Log.d(TAG, "Test notification created with ID: $notificationId")
    }

    /**
     * Get importance level description
     */
    private fun getImportanceDescription(importance: Int): String {
        return when (importance) {
            NotificationManager.IMPORTANCE_NONE -> "None"
            NotificationManager.IMPORTANCE_MIN -> "Min"
            NotificationManager.IMPORTANCE_LOW -> "Low"
            NotificationManager.IMPORTANCE_DEFAULT -> "Default"
            NotificationManager.IMPORTANCE_HIGH -> "High"
            NotificationManager.IMPORTANCE_MAX -> "Max"
            else -> "Unknown"
        }
    }

    /**
     * Force recreate notification channel (untuk troubleshooting)
     */
    fun recreateNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Delete existing channel
            notificationManager.deleteNotificationChannel(CHANNEL_ID)
            Log.d(TAG, "Deleted existing notification channel")

            // Recreate channel
            createNotificationChannel(context)
            Log.d(TAG, "Recreated notification channel")
        }
    }

    /**
     * Get all notification channels untuk debugging
     */
    fun logAllChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channels = notificationManager.notificationChannels

            Log.d(TAG, "=== All Notification Channels ===")
            channels.forEach { channel ->
                Log.d(TAG, "Channel: ${channel.id} - ${channel.name} (Importance: ${channel.importance})")
            }
            Log.d(TAG, "=== End All Channels ===")
        }
    }
}