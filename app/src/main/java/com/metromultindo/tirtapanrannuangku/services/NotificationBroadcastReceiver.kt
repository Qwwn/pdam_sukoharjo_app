package com.metromultindo.tirtapanrannuangku.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Optional BroadcastReceiver untuk handling notification events
 * Berguna untuk tracking dan analytics
 */
class NotificationBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationBroadcastReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "Broadcast received: ${intent?.action}")

        context?.let { ctx ->
            intent?.let { i ->
                when (i.action) {
                    "com.metromultindo.pdam_app_v2.NOTIFICATION_CLICKED" -> {
                        handleNotificationClicked(ctx, i)
                    }
                }
            }
        }
    }

    private fun handleNotificationClicked(context: Context, intent: Intent) {
        Log.d(TAG, "Notification clicked")

        val newsId = intent.getStringExtra("news_id")
        val title = intent.getStringExtra("title")

        Log.d(TAG, "Notification clicked - newsId: $newsId, title: $title")

        // Simpan analytics atau tracking data jika diperlukan
        // Analytics.track("notification_clicked", mapOf("news_id" to newsId))
    }
}