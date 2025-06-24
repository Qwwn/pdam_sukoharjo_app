/**
 * File: app/src/main/java/com/metromultindo/pdam_app_v2/models/NotificationModels.kt
 *
 * FIXED: Pindahkan NotificationData ke file terpisah agar bisa diakses semua file
 */

package com.metromultindo.tirtapanrannuangku.model

/**
 * Data class untuk notification data yang konsisten
 */
data class NotificationData(
    val title: String,
    val body: String,
    val newsId: String?,
    val timestamp: Long,
    val dataOnlyMode: Boolean = false,
    val forceCustomStyling: Boolean = false
) {
    fun hasValidNewsId(): Boolean {
        return newsId != null && newsId.isNotBlank() && getNewsIdAsInt() != null
    }

    fun getNewsIdAsInt(): Int? {
        return try {
            newsId?.toIntOrNull()
        } catch (e: Exception) {
            null
        }
    }

    override fun toString(): String {
        return "NotificationData(title='$title', body='$body', newsId='$newsId', dataOnly=$dataOnlyMode, forceCustom=$forceCustomStyling)"
    }
}

/**
 * Enum untuk app state tracking
 */
enum class AppState {
    FOREGROUND, BACKGROUND, KILLED
}

/**
 * Data class untuk pending navigation
 */
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
 * Data class untuk notification metrics
 */
data class NotificationMetrics(
    val notificationId: String,
    val newsId: String,
    val title: String,
    val deliveredAt: Long,
    val clickedAt: Long? = null,
    val appState: AppState,
    val navigatedSuccessfully: Boolean = false,
    val errorCode: String? = null
)

/**
 * Data class untuk notification performance summary
 */
data class NotificationPerformance(
    val totalDelivered: Int,
    val totalClicked: Int,
    val totalNavigated: Int,
    val clickThroughRate: Float,
    val navigationSuccessRate: Float,
    val averageClickDelay: Long,
    val performanceByState: Map<AppState, StatePerformance>
)

/**
 * Data class untuk state performance
 */
data class StatePerformance(
    val delivered: Int,
    val clicked: Int,
    val navigated: Int
) {
    val clickRate: Float get() = if (delivered > 0) (clicked.toFloat() / delivered) * 100 else 0f
    val navRate: Float get() = if (clicked > 0) (navigated.toFloat() / clicked) * 100 else 0f
}