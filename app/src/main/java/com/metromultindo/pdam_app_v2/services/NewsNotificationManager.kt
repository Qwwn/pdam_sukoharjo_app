package com.metromultindo.pdam_app_v2.service

import android.content.Context
import android.util.Log
import androidx.work.*
import com.metromultindo.pdam_app_v2.worker.SimpleNewsCheckWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val workManager = WorkManager.getInstance(context)

    companion object {
        private const val TAG = "NewsNotificationManager"
    }

    fun startNewsChecking() {
        Log.d(TAG, "Starting news checking...")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(false) // Allow when battery is low
            .build()

        // PERBAIKAN: Gunakan interval yang valid (minimum 15 menit untuk periodic work)
        val newsCheckRequest = PeriodicWorkRequestBuilder<SimpleNewsCheckWorker>(
            repeatInterval = 15, // Minimum 15 menit untuk PeriodicWork
            repeatIntervalTimeUnit = TimeUnit.MINUTES,
            flexTimeInterval = 5, // Allow 5 minutes flexibility
            flexTimeIntervalUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag("periodic_news_check")
            .build()

        workManager.enqueueUniquePeriodicWork(
            SimpleNewsCheckWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            newsCheckRequest
        )

        Log.d(TAG, "Periodic WorkManager started (every 15 minutes)")
    }

    fun stopNewsChecking() {
        workManager.cancelUniqueWork(SimpleNewsCheckWorker.WORK_NAME)
        Log.d(TAG, "News checking stopped")
    }

    fun checkNewsNow() {
        Log.d(TAG, "Triggering immediate news check...")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val immediateCheckRequest = OneTimeWorkRequestBuilder<SimpleNewsCheckWorker>()
            .setConstraints(constraints)
            .addTag("immediate_news_check")
            .build()

        workManager.enqueue(immediateCheckRequest)
        Log.d(TAG, "Immediate check enqueued")
    }

    // TAMBAHAN: Method untuk testing dengan interval pendek
    fun startTestingMode() {
        Log.d(TAG, "Starting testing mode with frequent checks...")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Untuk testing, buat OneTimeWork yang berulang
        scheduleNextCheck()
    }

    private fun scheduleNextCheck() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val testCheckRequest = OneTimeWorkRequestBuilder<SimpleNewsCheckWorker>()
            .setConstraints(constraints)
            .setInitialDelay(2, TimeUnit.MINUTES) // Check every 2 minutes for testing
            .addTag("test_news_check")
            .build()

        workManager.enqueue(testCheckRequest)

        // Schedule next check
        val nextSchedule = OneTimeWorkRequestBuilder<SimpleNewsCheckWorker>()
            .setConstraints(constraints)
            .setInitialDelay(4, TimeUnit.MINUTES) // Next check in 4 minutes
            .addTag("test_news_check")
            .build()

        workManager.enqueue(nextSchedule)

        Log.d(TAG, "Scheduled test checks for next 4 minutes")
    }
}