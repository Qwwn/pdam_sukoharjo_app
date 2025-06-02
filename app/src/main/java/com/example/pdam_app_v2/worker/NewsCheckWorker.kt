package com.example.pdam_app_v2.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.pdam_app_v2.data.api.RetrofitInstance
import com.example.pdam_app_v2.data.preferences.NewsPreferences
import com.example.pdam_app_v2.data.repository.NewsRepository
import com.example.pdam_app_v2.data.repository.ApiResult
import com.example.pdam_app_v2.service.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class NewsCheckWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParams: WorkerParameters,
    private val newsRepository: NewsRepository,
    private val newsPreferences: NewsPreferences
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "NewsCheckWorker"
        const val WORK_NAME = "news_check_work"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Hilt Worker: Checking for new news...")

            // Get latest news
            when (val result = newsRepository.getNews(limit = 5, offset = 0)) {
                is ApiResult.Success -> {
                    val newsList = result.data

                    if (newsList.isNotEmpty()) {
                        val latestNewsId = newsList.first().id
                        val lastKnownNewsId = newsPreferences.getLastNewsId()

                        Log.d(TAG, "Latest news ID: $latestNewsId, Last known: $lastKnownNewsId")

                        if (latestNewsId > lastKnownNewsId) {
                            // Ada berita baru
                            val newNews = newsList.filter { it.id > lastKnownNewsId }

                            Log.d(TAG, "Found ${newNews.size} new news")

                            if (newNews.size == 1) {
                                // Hanya 1 berita baru
                                val news = newNews.first()
                                NotificationHelper.showNewsNotification(
                                    context,
                                    news.id,
                                    news.title,
                                    news.content,
                                    news.author
                                )
                            } else {
                                // Multiple berita baru
                                NotificationHelper.showMultipleNewsNotification(
                                    context,
                                    newNews.size
                                )
                            }

                            // Update preferences
                            newsPreferences.setLastNewsId(latestNewsId)
                        }
                    }

                    newsPreferences.setLastCheckTime(System.currentTimeMillis())
                    Result.success()
                }
                is ApiResult.Error -> {
                    Log.e(TAG, "Error checking news: ${result.message}")
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in NewsCheckWorker", e)
            Result.failure()
        }
    }
}