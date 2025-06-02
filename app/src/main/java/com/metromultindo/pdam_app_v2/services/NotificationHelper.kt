package com.metromultindo.pdam_app_v2.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.metromultindo.pdam_app_v2.MainActivity
import com.metromultindo.pdam_app_v2.R
import kotlin.random.Random

object NotificationHelper {

    private const val CHANNEL_ID = "pdam_news_channel"
    private const val CHANNEL_NAME = "PDAM News"
    private const val CHANNEL_DESCRIPTION = "Notifications for PDAM news updates"
    private const val TAG = "NotificationHelper"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }

    fun showNewsNotification(
        context: Context,
        newsId: Int,
        title: String,
        content: String,
        author: String
    ) {
        Log.d(TAG, "Attempting to show notification for news ID: $newsId")

        // Check notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Notification permission not granted")
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("news_id", newsId.toString())
            putExtra("navigate_to", "news_detail")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            newsId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val shortContent = if (content.length > 100) {
            content.substring(0, 100) + "..."
        } else {
            content
        }

        try {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Berita Baru PDAM")
                .setContentText(title)
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("$title\n\n$shortContent\n\nOleh: $author"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(longArrayOf(1000, 1000, 1000))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build()

            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(newsId, notification)

            Log.d(TAG, "Notification displayed successfully for news ID: $newsId")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception showing notification", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification", e)
        }
    }

    fun showMultipleNewsNotification(
        context: Context,
        newsCount: Int
    ) {
        Log.d(TAG, "Attempting to show multiple news notification: $newsCount news")

        // Check notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Notification permission not granted")
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "news")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            Random.nextInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("PDAM News")
                .setContentText("Ada $newsCount berita baru untuk Anda")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build()

            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(Random.nextInt(), notification)

            Log.d(TAG, "Multiple news notification displayed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing multiple news notification", e)
        }
    }
}