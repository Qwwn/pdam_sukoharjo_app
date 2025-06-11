package com.metromultindo.tirtamakmur.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.metromultindo.tirtamakmur.MainActivity
import com.metromultindo.tirtamakmur.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PdamFCMService : FirebaseMessagingService() {

    @Inject
    lateinit var fcmTokenManager: FCMTokenManager

    companion object {
        private const val TAG = "PdamFCMService"

        const val CHANNEL_ID = "pdam_consistent_news"
        const val CHANNEL_NAME = "PDAM Sukoharjo"
        const val APP_NAME = "Aplikasi Layanan Pelanggan"

        private const val NOTIFICATION_COLOR = 0xFF0088FF.toInt()
        private val VIBRATION_PATTERN = longArrayOf(0, 250, 250, 250)
    }

    override fun onCreate() {
        super.onCreate()
        createConsistentChannel()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val hasNotificationPayload = remoteMessage.notification != null
        val hasDataPayload = remoteMessage.data.isNotEmpty()

        if (hasNotificationPayload) {
            Log.w(TAG, "⚠️ WARNING: Backend still sends notification payload!")
            Log.w(TAG, "⚠️ This causes inconsistency - backend should send DATA-ONLY")
            Log.w(TAG, "⚠️ Backend title: '${remoteMessage.notification?.title}'")
            Log.w(TAG, "⚠️ Backend body: '${remoteMessage.notification?.body}'")
        }

        if (hasDataPayload) {
            clearSystemNotifications()

            processDataWithConsistentStyling(remoteMessage)
        } else {
            Log.e(TAG, "❌ No data payload - cannot process consistently")
        }
    }

    private fun clearSystemNotifications() {
        try {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancelAll()
            Log.d(TAG, "🧹 Cleared any system notifications")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error clearing notifications", e)
        }
    }

    private fun createConsistentChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                try {
                    val existingChannels = notificationManager.notificationChannels
                    existingChannels.forEach { channel ->
                        if (channel.id.contains("pdam", ignoreCase = true) && channel.id != CHANNEL_ID) {
                            notificationManager.deleteNotificationChannel(channel.id)
                            Log.d(TAG, "🗑️ Deleted old channel: ${channel.id}")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Warning deleting old channels: ${e.message}")
                }

                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifikasi berita dan pengumuman PDAM Sukoharjo"

                    enableVibration(true)
                    vibrationPattern = VIBRATION_PATTERN
                    enableLights(true)
                    lightColor = NOTIFICATION_COLOR
                    setShowBadge(true)
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                    setSound(null, null)
                    setBypassDnd(false)
                }

                notificationManager.createNotificationChannel(channel)

                val verifyChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
                Log.d(TAG, "✅ CONSISTENT CHANNEL CREATED")
                Log.d(TAG, "📛 Channel ID: ${verifyChannel?.id}")
                Log.d(TAG, "📛 Channel Name: '${verifyChannel?.name}'")
                Log.d(TAG, "📛 Importance: ${verifyChannel?.importance}")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error creating consistent channel", e)
            }
        }
    }

    private fun processDataWithConsistentStyling(remoteMessage: RemoteMessage) {
        try {
            Log.d(TAG, "🚀 === PROCESSING WITH CONSISTENT STYLING ===")

            val title = remoteMessage.data["title"]?.trim() ?: "PDAM Sukoharjo"
            val body = remoteMessage.data["body"]?.trim() ?: "Ada informasi terbaru"
            val newsId = remoteMessage.data["news_id"]

            Log.d(TAG, "📰 Title: '$title'")
            Log.d(TAG, "📄 Body: '$body'")
            Log.d(TAG, "🆔 News ID: '$newsId'")

            NotificationDataHelper.saveNotificationData(this, newsId, title, body)

            showConsistentNotification(title, body, newsId)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in consistent processing", e)
        }
    }

    private fun showConsistentNotification(title: String, body: String, newsId: String?) {
        try {
            Log.d(TAG, "🚀 === SHOWING CONSISTENT NOTIFICATION ===")

            val notificationId = generateNotificationId(newsId)
            val intent = createConsistentIntent(title, body, newsId)
            val pendingIntent = createPendingIntent(intent, notificationId)

            val smallIcon = getConsistentSmallIcon()
            val largeIcon = getConsistentLargeIcon()

            Log.d(TAG, "🎨 Icons - Small: $smallIcon, Large: ${largeIcon != null}")

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(smallIcon)
                .setLargeIcon(largeIcon)

                .setColor(NOTIFICATION_COLOR)
                .setColorized(false)

                .setContentTitle(title)
                .setContentText(body)

                .setSubText(APP_NAME)
                .setTicker(APP_NAME)

                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText(body)
                    .setBigContentTitle(title)
                    .setSummaryText(APP_NAME))

                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)

                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)

                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)

                .setGroup(null)
                .setGroupSummary(false)

                .setLocalOnly(false)
                .setOngoing(false)
                .setBadgeIconType(NotificationCompat.BADGE_ICON_LARGE)

                .build()

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = notificationManager.getNotificationChannel(CHANNEL_ID)
                if (channel == null) {
                    Log.e(TAG, "❌ CHANNEL MISSING! Recreating...")
                    createConsistentChannel()
                }
            }

            notificationManager.notify(notificationId, notification)

            Log.d(TAG, "🚀 === CONSISTENT NOTIFICATION SHOWN ===")
            Log.d(TAG, "🆔 ID: $notificationId")
            Log.d(TAG, "📛 Channel: $CHANNEL_ID")
            Log.d(TAG, "📱 App Name: '$APP_NAME'")
            Log.d(TAG, "📰 Title: '$title'")
            Log.d(TAG, "📄 Body: '$body'")
            Log.d(TAG, "🕐 Timestamp: ${System.currentTimeMillis()}")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error showing consistent notification", e)
        }
    }

    private fun getConsistentSmallIcon(): Int {
        return when {
            resourceExists(R.drawable.ic_notification) -> R.drawable.ic_notification
            resourceExists(R.mipmap.ic_launcher_foreground) -> R.mipmap.ic_launcher_foreground
            else -> R.mipmap.ic_launcher
        }
    }

    private fun getConsistentLargeIcon(): Bitmap? {
        return try {
            val drawable = ContextCompat.getDrawable(this, R.mipmap.ic_launcher)
            if (drawable != null) {
                val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating large icon: ${e.message}")
            null
        }
    }

    private fun resourceExists(resourceId: Int): Boolean {
        return try {
            resources.getResourceName(resourceId)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun createConsistentIntent(title: String, body: String, newsId: String?): Intent {
        return Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP

            if (newsId != null && newsId.isNotBlank()) {
                putExtra("navigate_to", "news_detail")
                putExtra("news_id", newsId)
                putExtra("newsId", newsId.toIntOrNull() ?: 0)
                action = "com.metromultindo.tirtamakmur.NOTIFICATION_NEWS_DETAIL"
                data = android.net.Uri.parse("pdam://news_detail/$newsId?ts=${System.currentTimeMillis()}")
            } else {
                putExtra("navigate_to", "news")
                action = "com.metromultindo.tirtamakmur.NOTIFICATION_NEWS_LIST"
                data = android.net.Uri.parse("pdam://news?ts=${System.currentTimeMillis()}")
            }

            putExtra("from_notification", true)
            putExtra("notification_title", title)
            putExtra("notification_body", body)
            putExtra("notification_timestamp", System.currentTimeMillis())
        }
    }

    private fun createPendingIntent(intent: Intent, notificationId: Int): PendingIntent {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(this, notificationId, intent, flags)
    }

    private fun generateNotificationId(newsId: String?): Int {
        return if (newsId != null) {
            try {
                newsId.toInt() + 70000
            } catch (e: Exception) {
                (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
            }
        } else {
            (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token received: $token")

        fcmTokenManager.saveToken(token)
        fcmTokenManager.sendTokenToServer(token)
    }
}