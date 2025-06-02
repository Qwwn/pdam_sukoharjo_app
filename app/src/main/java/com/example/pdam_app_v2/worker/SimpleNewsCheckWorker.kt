package com.example.pdam_app_v2.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.pdam_app_v2.data.api.RetrofitInstance
import com.example.pdam_app_v2.data.preferences.NewsPreferences
import com.example.pdam_app_v2.data.repository.NewsRepository
import com.example.pdam_app_v2.data.repository.ApiResult
import com.example.pdam_app_v2.service.NotificationHelper

class SimpleNewsCheckWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "SimpleNewsCheckWorker"
        const val WORK_NAME = "simple_news_check_work"
    }

    private val newsRepository = NewsRepository(RetrofitInstance.api)
    private val newsPreferences = NewsPreferences(applicationContext)

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Simple Worker: Checking for new news...")

            when (val result = newsRepository.getNews(limit = 5, offset = 0)) {
                is ApiResult.Success -> {
                    val newsList = result.data

                    if (newsList.isNotEmpty()) {
                        val latestNewsId = newsList.first().id
                        val lastKnownNewsId = newsPreferences.getLastNewsId()

                        Log.d(TAG, "Latest news ID: $latestNewsId, Last known: $lastKnownNewsId")

                        if (latestNewsId > lastKnownNewsId) {
                            val newNews = newsList.filter { it.id > lastKnownNewsId }

                            Log.d(TAG, "Found ${newNews.size} new news")

                            // PERBAIKAN: Tambahkan try-catch untuk notifikasi
                            try {
                                if (newNews.size == 1) {
                                    val news = newNews.first()
                                    Log.d(TAG, "Showing notification for news: ${news.title}")

                                    NotificationHelper.showNewsNotification(
                                        applicationContext,
                                        news.id,
                                        news.title,
                                        news.content,
                                        news.author
                                    )

                                    Log.d(TAG, "Notification sent successfully")
                                } else {
                                    Log.d(TAG, "Showing multiple news notification: ${ newNews.size} news")

                                    NotificationHelper.showMultipleNewsNotification(
                                        applicationContext,
                                        newNews.size
                                    )

                                    Log.d(TAG, "Multiple notification sent successfully")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to show notification", e)
                            }

                            // Update preferences
                            newsPreferences.setLastNewsId(latestNewsId)
                            Log.d(TAG, "Updated last news ID to: $latestNewsId")
                        } else {
                            Log.d(TAG, "No new news found")
                        }
                    } else {
                        Log.d(TAG, "News list is empty")
                    }

                    newsPreferences.setLastCheckTime(System.currentTimeMillis())
                    Log.d(TAG, "Work completed successfully")
                    Result.success()
                }
                is ApiResult.Error -> {
                    Log.e(TAG, "Error checking news: ${result.message}")
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in SimpleNewsCheckWorker", e)
            Result.failure()
        }
    }
}