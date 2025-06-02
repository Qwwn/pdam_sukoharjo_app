package com.metromultindo.pdam_app_v2.data.preferences

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("news_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val LAST_CHECK_TIME = "last_check_time"
        private const val LAST_NEWS_ID = "last_news_id"
        private const val LAST_NEWS_COUNT = "last_news_count"
    }

    fun getLastCheckTime(): Long {
        return prefs.getLong(LAST_CHECK_TIME, 0)
    }

    fun setLastCheckTime(time: Long) {
        prefs.edit().putLong(LAST_CHECK_TIME, time).apply()
    }

    fun getLastNewsId(): Int {
        return prefs.getInt(LAST_NEWS_ID, 0)
    }

    fun setLastNewsId(newsId: Int) {
        prefs.edit().putInt(LAST_NEWS_ID, newsId).apply()
    }

    fun getLastNewsCount(): Int {
        return prefs.getInt(LAST_NEWS_COUNT, 0)
    }

    fun setLastNewsCount(count: Int) {
        prefs.edit().putInt(LAST_NEWS_COUNT, count).apply()
    }
}
