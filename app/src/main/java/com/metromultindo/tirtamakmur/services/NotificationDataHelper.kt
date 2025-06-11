package com.metromultindo.tirtamakmur.services

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.metromultindo.tirtamakmur.model.NotificationData

/**
 * Enhanced helper class untuk menangani data notifikasi yang persisten
 * Berguna untuk cold start navigation dan robust notification handling
 */
object NotificationDataHelper {

    private const val TAG = "NotificationDataHelper"
    private const val PREFS_NAME = "notification_data"
    private const val KEY_NEWS_ID = "last_notification_news_id"
    private const val KEY_TITLE = "last_notification_title"
    private const val KEY_BODY = "last_notification_body"
    private const val KEY_TIMESTAMP = "last_notification_time"
    private const val KEY_HANDLED = "notification_handled"
    private const val KEY_ATTEMPT_COUNT = "notification_attempt_count"
    private const val KEY_LAST_ATTEMPT = "notification_last_attempt"

    // Timeout untuk cold start notification (60 detik - diperpanjang)
    private const val COLD_START_TIMEOUT = 60_000L

    // Max attempts untuk retry notification handling
    private const val MAX_ATTEMPT_COUNT = 3

    // Minimum interval between attempts (5 detik)
    private const val MIN_ATTEMPT_INTERVAL = 5_000L

    /**
     * Simpan data notifikasi untuk cold start recovery
     */
    fun saveNotificationData(
        context: Context,
        newsId: String?,
        title: String,
        body: String
    ) {
        try {
            val prefs = getPrefs(context)
            with(prefs.edit()) {
                putString(KEY_NEWS_ID, newsId)
                putString(KEY_TITLE, title)
                putString(KEY_BODY, body)
                putLong(KEY_TIMESTAMP, System.currentTimeMillis())
                putBoolean(KEY_HANDLED, false)
                putInt(KEY_ATTEMPT_COUNT, 0)
                putLong(KEY_LAST_ATTEMPT, 0)
                apply()
            }
            Log.d(TAG, "Notification data saved: newsId=$newsId, title=$title")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving notification data", e)
        }
    }

    /**
     * Ambil data notifikasi untuk cold start dengan enhanced validation
     */
    fun getNotificationDataForColdStart(context: Context): NotificationData? {
        return try {
            val prefs = getPrefs(context)
            val newsId = prefs.getString(KEY_NEWS_ID, null)
            val title = prefs.getString(KEY_TITLE, null)
            val body = prefs.getString(KEY_BODY, null)
            val timestamp = prefs.getLong(KEY_TIMESTAMP, 0)
            val handled = prefs.getBoolean(KEY_HANDLED, true)
            val attemptCount = prefs.getInt(KEY_ATTEMPT_COUNT, 0)
            val lastAttempt = prefs.getLong(KEY_LAST_ATTEMPT, 0)

            val timeDiff = System.currentTimeMillis() - timestamp
            val isRecent = timeDiff < COLD_START_TIMEOUT
            val canRetry = attemptCount < MAX_ATTEMPT_COUNT
            val attemptIntervalOk = (System.currentTimeMillis() - lastAttempt) > MIN_ATTEMPT_INTERVAL

            Log.d(TAG, "Checking notification data:")
            Log.d(TAG, "  newsId=$newsId")
            Log.d(TAG, "  handled=$handled")
            Log.d(TAG, "  timeDiff=${timeDiff}ms")
            Log.d(TAG, "  isRecent=$isRecent")
            Log.d(TAG, "  attemptCount=$attemptCount")
            Log.d(TAG, "  canRetry=$canRetry")
            Log.d(TAG, "  attemptIntervalOk=$attemptIntervalOk")

            if (newsId != null && !handled && isRecent && canRetry && attemptIntervalOk) {
                // Increment attempt count
                incrementAttemptCount(context)

                NotificationData(
                    newsId = newsId,
                    title = title ?: "",
                    body = body ?: "",
                    timestamp = timestamp
                )
            } else {
                // Clear expired, invalid, atau max attempts reached
                if (!isRecent || !canRetry) {
                    Log.d(TAG, "Clearing notification data - expired or max attempts reached")
                    clearNotificationData(context)
                }
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting notification data", e)
            null
        }
    }

    /**
     * Mark notifikasi sebagai sudah di-handle
     */
    fun markNotificationHandled(context: Context) {
        try {
            val prefs = getPrefs(context)
            with(prefs.edit()) {
                putBoolean(KEY_HANDLED, true)
                putLong(KEY_LAST_ATTEMPT, System.currentTimeMillis())
                apply()
            }
            Log.d(TAG, "Notification marked as handled")
        } catch (e: Exception) {
            Log.e(TAG, "Error marking notification as handled", e)
        }
    }

    /**
     * Increment attempt count
     */
    private fun incrementAttemptCount(context: Context) {
        try {
            val prefs = getPrefs(context)
            val currentCount = prefs.getInt(KEY_ATTEMPT_COUNT, 0)
            with(prefs.edit()) {
                putInt(KEY_ATTEMPT_COUNT, currentCount + 1)
                putLong(KEY_LAST_ATTEMPT, System.currentTimeMillis())
                apply()
            }
            Log.d(TAG, "Notification attempt count incremented to: ${currentCount + 1}")
        } catch (e: Exception) {
            Log.e(TAG, "Error incrementing attempt count", e)
        }
    }

    /**
     * Clear semua data notifikasi
     */
    fun clearNotificationData(context: Context) {
        try {
            val prefs = getPrefs(context)
            prefs.edit().clear().apply()
            Log.d(TAG, "Notification data cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing notification data", e)
        }
    }

    /**
     * Check apakah ada pending notification untuk cold start
     */
    fun hasPendingNotification(context: Context): Boolean {
        return getNotificationDataForColdStart(context) != null
    }

    /**
     * Get detailed notification status untuk debugging
     */
    fun getNotificationStatus(context: Context): String {
        return try {
            val prefs = getPrefs(context)
            val newsId = prefs.getString(KEY_NEWS_ID, "null")
            val handled = prefs.getBoolean(KEY_HANDLED, true)
            val timestamp = prefs.getLong(KEY_TIMESTAMP, 0)
            val attemptCount = prefs.getInt(KEY_ATTEMPT_COUNT, 0)
            val timeDiff = System.currentTimeMillis() - timestamp

            "NotificationStatus(newsId=$newsId, handled=$handled, timeDiff=${timeDiff}ms, attempts=$attemptCount)"
        } catch (e: Exception) {
            "NotificationStatus(error=${e.message})"
        }
    }

    /**
     * Force reset notification data untuk debugging
     */
    fun forceResetNotificationData(context: Context) {
        try {
            clearNotificationData(context)
            Log.d(TAG, "Notification data force reset")
        } catch (e: Exception) {
            Log.e(TAG, "Error in force reset", e)
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}