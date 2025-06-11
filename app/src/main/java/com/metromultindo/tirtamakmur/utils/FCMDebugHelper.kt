package com.metromultindo.tirtamakmur.utils

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging

object FCMDebugHelper {
    private const val TAG = "FCMDebugHelper"

    fun logCurrentToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            Log.d(TAG, "Current FCM token: $token")
        }
    }

    fun subscribeToTopic(topic: String) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
            .addOnCompleteListener { task ->
                var msg = "Subscribed to $topic"
                if (!task.isSuccessful) {
                    msg = "Failed to subscribe to $topic"
                }
                Log.d(TAG, msg)
            }
    }

    fun unsubscribeFromTopic(topic: String) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
            .addOnCompleteListener { task ->
                var msg = "Unsubscribed from $topic"
                if (!task.isSuccessful) {
                    msg = "Failed to unsubscribe from $topic"
                }
                Log.d(TAG, msg)
            }
    }
}