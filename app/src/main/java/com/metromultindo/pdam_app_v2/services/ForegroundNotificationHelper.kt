package com.metromultindo.pdam_app_v2.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.metromultindo.pdam_app_v2.MainActivity
import com.metromultindo.pdam_app_v2.R

/**
 * Helper untuk show notification di foreground dengan styling yang IDENTIK
 * dengan background/killed notifications
 */
object ForegroundNotificationHelper {

    private const val TAG = "ForegroundNotificationHelper"

    // CRITICAL: Use same constants as FCM Service
    private const val CHANNEL_ID = "pdam_consistent_news"
    private const val APP_NAME = "PDAM Sukoharjo"
    private const val NOTIFICATION_COLOR = 0xFF0088FF.toInt()

    /**
     * Show notification di foreground dengan styling IDENTIK seperti background/killed
     */
    fun showForegroundNotification(
        context: Context,
        title: String,
        body: String,
        newsId: String? = null
    ) {
        try {
            Log.d(TAG, "🚀 === SHOWING FOREGROUND NOTIFICATION ===")
            Log.d(TAG, "📰 Title: '$title'")
            Log.d(TAG, "📄 Body: '$body'")
            Log.d(TAG, "🆔 News ID: '$newsId'")

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationId = generateNotificationId(newsId)

            // Create intent - IDENTICAL to FCM Service
            val intent = createConsistentIntent(context, title, body, newsId)
            val pendingIntent = createPendingIntent(context, intent, notificationId)

            // Get icons - IDENTICAL to FCM Service
            val smallIcon = getConsistentSmallIcon(context)
            val largeIcon = getConsistentLargeIcon(context)

            // CRITICAL: IDENTICAL notification builder to FCM Service
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                // === IDENTICAL ICONS ===
                .setSmallIcon(smallIcon)
                .setLargeIcon(largeIcon)

                // === IDENTICAL COLORS ===
                .setColor(NOTIFICATION_COLOR)
                .setColorized(false)

                // === IDENTICAL CONTENT ===
                .setContentTitle(title)
                .setContentText(body)

                // === CRITICAL: IDENTICAL APP NAME ===
                .setSubText(APP_NAME)
                .setTicker(APP_NAME)

                // === IDENTICAL BIG TEXT STYLE ===
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText(body)
                    .setBigContentTitle(title)
                    .setSummaryText(APP_NAME))

                // === IDENTICAL BEHAVIOR ===
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)

                // === IDENTICAL TIMESTAMP ===
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)

                // === IDENTICAL VISIBILITY ===
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)

                // === IDENTICAL GROUPING ===
                .setGroup(null)
                .setGroupSummary(false)

                // === IDENTICAL ADDITIONAL SETTINGS ===
                .setLocalOnly(false)
                .setOngoing(false)
                .setBadgeIconType(NotificationCompat.BADGE_ICON_LARGE)

                .build()

            // Show notification
            notificationManager.notify(notificationId, notification)

            // Save for cold start - IDENTICAL to FCM Service
            NotificationDataHelper.saveNotificationData(context, newsId, title, body)

            Log.d(TAG, "✅ Foreground notification shown with IDENTICAL styling")
            Log.d(TAG, "🆔 ID: $notificationId")
            Log.d(TAG, "📛 Channel: $CHANNEL_ID")
            Log.d(TAG, "📱 App Name: '$APP_NAME'")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error showing foreground notification", e)
        }
    }

    /**
     * IDENTICAL icon logic to FCM Service
     */
    private fun getConsistentSmallIcon(context: Context): Int {
        return when {
            resourceExists(context, R.drawable.ic_notification) -> R.drawable.ic_notification
            resourceExists(context, R.mipmap.ic_launcher_foreground) -> R.mipmap.ic_launcher_foreground
            else -> R.mipmap.ic_launcher
        }
    }

    /**
     * IDENTICAL large icon logic to FCM Service
     */
    private fun getConsistentLargeIcon(context: Context): Bitmap? {
        return try {
            val drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
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

    /**
     * IDENTICAL intent logic to FCM Service
     */
    private fun createConsistentIntent(context: Context, title: String, body: String, newsId: String?): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP

            if (newsId != null && newsId.isNotBlank()) {
                putExtra("navigate_to", "news_detail")
                putExtra("news_id", newsId)
                putExtra("newsId", newsId.toIntOrNull() ?: 0)
                action = "com.metromultindo.pdam_app_v2.NOTIFICATION_NEWS_DETAIL"
                data = android.net.Uri.parse("pdam://news_detail/$newsId?ts=${System.currentTimeMillis()}")
            } else {
                putExtra("navigate_to", "news")
                action = "com.metromultindo.pdam_app_v2.NOTIFICATION_NEWS_LIST"
                data = android.net.Uri.parse("pdam://news?ts=${System.currentTimeMillis()}")
            }

            putExtra("from_notification", true)
            putExtra("notification_title", title)
            putExtra("notification_body", body)
            putExtra("notification_timestamp", System.currentTimeMillis())
        }
    }

    /**
     * IDENTICAL pending intent logic to FCM Service
     */
    private fun createPendingIntent(context: Context, intent: Intent, notificationId: Int): PendingIntent {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(context, notificationId, intent, flags)
    }

    /**
     * IDENTICAL notification ID logic to FCM Service
     */
    private fun generateNotificationId(newsId: String?): Int {
        return if (newsId != null) {
            try {
                newsId.toInt() + 70000 // Same offset as FCM Service
            } catch (e: Exception) {
                (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
            }
        } else {
            (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        }
    }

    private fun resourceExists(context: Context, resourceId: Int): Boolean {
        return try {
            context.resources.getResourceName(resourceId)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Clear any existing notifications before showing new one
     */
    fun clearExistingNotifications(context: Context) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancelAll()
            Log.d(TAG, "🧹 Cleared existing notifications")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error clearing notifications", e)
        }
    }

    /**
     * Simplified method untuk quick notification
     */
    fun showQuickNotification(context: Context, title: String, body: String) {
        showForegroundNotification(context, title, body, null)
    }

    /**
     * Method untuk news notification dengan ID
     */
    fun showNewsNotification(context: Context, title: String, body: String, newsId: Int) {
        showForegroundNotification(context, title, body, newsId.toString())
    }
}