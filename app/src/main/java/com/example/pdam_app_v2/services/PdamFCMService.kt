package com.example.pdam_app_v2.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.pdam_app_v2.MainActivity
import com.example.pdam_app_v2.R
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
        private const val CHANNEL_ID = "pdam_news_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "FCM Service created")
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token received: $token")

        // Save token locally
        fcmTokenManager.saveToken(token)

        // Send token to server
        fcmTokenManager.sendTokenToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Message received from: ${remoteMessage.from}")

        // Handle data payload
        remoteMessage.data.isNotEmpty().let {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }

        // Handle notification payload (when app is in foreground)
        remoteMessage.notification?.let {
            Log.d(TAG, "Message notification body: ${it.body}")
            showNotification(
                title = it.title ?: "PDAM Layanan",
                body = it.body ?: "",
                data = remoteMessage.data
            )
        }
    }

    private fun handleDataMessage(data: Map<String, String>) {
        val type = data["type"]
        val title = data["title"] ?: "PDAM Layanan"
        val body = data["body"] ?: "Ada update terbaru"
        val newsId = data["news_id"]

        when (type) {
            "news" -> {
                showNotification(
                    title = title,
                    body = body,
                    data = data
                )
            }
            "announcement" -> {
                showNotification(
                    title = title,
                    body = body,
                    data = data
                )
            }
            // Add more types as needed
        }
    }

    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            // Add data for navigation
            when (data["type"]) {
                "news" -> {
                    putExtra("navigate_to", "news_detail")
                    putExtra("news_id", data["news_id"])
                }
                "announcement" -> {
                    putExtra("navigate_to", "news")
                }
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Pastikan icon ini ada
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PDAM News Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for PDAM news and announcements"
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}