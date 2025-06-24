package com.metromultindo.tirtapanrannuangku.services

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.navigation.NavController
import kotlinx.coroutines.*

/**
 * Simplified notification navigation manager dengan loading state
 */
object SimplifiedNotificationManager {

    private const val TAG = "SimplifiedNotificationManager"
    private var navController: NavController? = null
    private var pendingNavigation: PendingNavigation? = null

    // Loading state callback
    var onLoadingStateChanged: ((Boolean) -> Unit)? = null

    data class PendingNavigation(
        val newsId: Int,
        val title: String = "",
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean {
            return (System.currentTimeMillis() - timestamp) > 60_000L // 1 minute
        }
    }

    /**
     * Set navigation controller
     */
    fun setNavController(controller: NavController) {
        navController = controller
        Log.d(TAG, "NavController set")

        // Process pending navigation if exists
        pendingNavigation?.let { pending ->
            if (!pending.isExpired()) {
                Log.d(TAG, "Processing pending navigation: ${pending.newsId}")
                CoroutineScope(Dispatchers.Main).launch {
                    delay(200) // Short delay for stability
                    processNavigation(pending.newsId, pending.title)
                }
            } else {
                clearPendingNavigation()
            }
        }
    }

    /**
     * Process notification intent - main entry point
     */
    fun processNotificationIntent(context: Context, intent: Intent?): Boolean {
        Log.d(TAG, "Processing notification intent")

        // Extract news ID from intent
        val newsId = extractNewsIdFromIntent(intent)
        val title = intent?.getStringExtra("notification_title") ?: ""

        return if (newsId != null) {
            Log.d(TAG, "Found news ID: $newsId")

            if (isNavigationReady()) {
                // Navigate immediately
                CoroutineScope(Dispatchers.Main).launch {
                    processNavigation(newsId, title)
                }
                true
            } else {
                // Store for later
                Log.d(TAG, "Navigation not ready, storing pending")
                storePendingNavigation(newsId, title)
                true
            }
        } else {
            // Check saved data as fallback
            processSavedNotificationData(context)
        }
    }

    /**
     * Process saved notification data
     */
    private fun processSavedNotificationData(context: Context): Boolean {
        val savedData = NotificationDataHelper.getNotificationDataForColdStart(context)

        return if (savedData != null && isValidNotificationData(savedData)) {
            Log.d(TAG, "Using saved notification data: ${savedData.newsId}")

            savedData.getNewsIdAsInt()?.let { newsId ->
                if (isNavigationReady()) {
                    CoroutineScope(Dispatchers.Main).launch {
                        processNavigation(newsId, savedData.title)
                        NotificationDataHelper.markNotificationHandled(context)
                    }
                    true
                } else {
                    storePendingNavigation(newsId, savedData.title)
                    true
                }
            } ?: false
        } else {
            Log.d(TAG, "No valid notification data found")
            false
        }
    }

    /**
     * Process navigation with loading state - Enhanced untuk killed state
     */
    private suspend fun processNavigation(newsId: Int, title: String = "") {
        try {
            Log.d(TAG, "Starting navigation to news: $newsId")

            // IMPORTANT: Show loading first - dengan delay untuk killed state
            onLoadingStateChanged?.invoke(true)

            // Add delay untuk killed state agar loading terlihat
            delay(800) // Increase delay untuk killed state visibility

            val controller = navController
            if (controller != null) {
                // Wait for navigation to be ready
                if (!isNavigationReady()) {
                    Log.d(TAG, "Waiting for navigation to be ready...")

                    var attempts = 0
                    while (attempts < 50 && !isNavigationReady()) { // Max 5 seconds
                        delay(100)
                        attempts++
                    }
                }

                if (isNavigationReady()) {
                    // Navigate to news detail
                    controller.navigate("newsDetail/$newsId") {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
                        restoreState = false
                    }

                    Log.d(TAG, "Navigation successful to news: $newsId")
                    clearPendingNavigation()

                    // Keep loading visible during transition untuk smooth UX
                    delay(1200) // Keep loading longer untuk ensure smooth loading
                } else {
                    Log.e(TAG, "Navigation failed - controller not ready")
                    // Store as pending for retry
                    storePendingNavigation(newsId, title)
                }
            } else {
                Log.e(TAG, "NavController is null")
                storePendingNavigation(newsId, title)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Navigation error", e)
            // Store as pending for retry
            storePendingNavigation(newsId, title)
        } finally {
            // Hide loading
            onLoadingStateChanged?.invoke(false)
        }
    }

    /**
     * Extract news ID from intent
     */
    private fun extractNewsIdFromIntent(intent: Intent?): Int? {
        if (intent == null) return null

        try {
            // Method 1: String extra
            intent.getStringExtra("news_id")?.toIntOrNull()?.let { return it }

            // Method 2: Integer extra
            val newsIdInt = intent.getIntExtra("newsId", -1)
            if (newsIdInt != -1) return newsIdInt

            // Method 3: URI data
            intent.data?.let { uri ->
                if (uri.scheme == "pdam") {
                    val path = uri.path
                    if (path?.startsWith("/news_detail/") == true) {
                        val newsIdString = path.substring("/news_detail/".length).split("?")[0]
                        return newsIdString.toIntOrNull()
                    }
                }
            }

            Log.d(TAG, "No news ID found in intent")
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting news ID", e)
        }

        return null
    }

    /**
     * Check if navigation is ready
     */
    private fun isNavigationReady(): Boolean {
        val controller = navController
        return controller != null && controller.currentDestination != null
    }

    /**
     * Store pending navigation
     */
    private fun storePendingNavigation(newsId: Int, title: String) {
        pendingNavigation = PendingNavigation(newsId, title)
        Log.d(TAG, "Stored pending navigation: $newsId")
    }

    /**
     * Clear pending navigation
     */
    fun clearPendingNavigation() {
        pendingNavigation = null
        Log.d(TAG, "Cleared pending navigation")
    }

    /**
     * Check if has pending navigation
     */
    fun hasPendingNavigation(): Boolean {
        return pendingNavigation != null && !pendingNavigation!!.isExpired()
    }

    /**
     * Get pending navigation for debugging
     */
    fun getPendingNavigation(): PendingNavigation? = pendingNavigation

    /**
     * Force process pending navigation
     */
    suspend fun forceProcessPending(): Boolean {
        val pending = pendingNavigation
        return if (pending != null && !pending.isExpired()) {
            Log.d(TAG, "Force processing pending: ${pending.newsId}")
            processNavigation(pending.newsId, pending.title)
            true
        } else {
            false
        }
    }

    /**
     * Reset manager
     */
    fun reset() {
        navController = null
        pendingNavigation = null
        onLoadingStateChanged = null
        Log.d(TAG, "Manager reset")
    }

    /**
     * Helper function to validate notification data
     */
    private fun isValidNotificationData(data: com.metromultindo.tirtapanrannuangku.model.NotificationData): Boolean {
        return !data.newsId.isNullOrBlank() && data.getNewsIdAsInt() != null
    }
}