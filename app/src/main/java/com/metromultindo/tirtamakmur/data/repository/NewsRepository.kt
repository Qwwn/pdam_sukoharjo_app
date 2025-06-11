// NewsRepository.kt - Updated to use real API
package com.metromultindo.tirtamakmur.data.repository

import com.metromultindo.tirtamakmur.data.api.ApiService
import com.metromultindo.tirtamakmur.data.model.News
import com.metromultindo.tirtamakmur.data.model.toNews
import javax.inject.Inject

class NewsRepository @Inject constructor(
    private val apiService: ApiService
) {

    suspend fun getNews(
        limit: Int = 10,
        offset: Int = 0,
        status: Int? = 1, // Default to active news only
        category: String? = null
    ): ApiResult<List<News>> {
        return try {
            val response = apiService.getAllNews(
                limit = limit,
                offset = offset,
                status = status,
                category = category
            )

            if (!response.error) {
                val newsList = response.newsData.map { it.toNews() }
                ApiResult.Success(newsList)
            } else {
                ApiResult.Error(
                    code = response.messageCode,
                    message = response.messageText
                )
            }
        } catch (e: Exception) {
            ApiResult.Error(
                code = 500,
                message = e.message ?: "Terjadi kesalahan saat mengambil data berita"
            )
        }
    }

    suspend fun getNewsById(newsId: Int): ApiResult<News> {
        return try {
            val response = apiService.getNewsById(newsId)

            if (!response.error) {
                ApiResult.Success(response.news.toNews())
            } else {
                ApiResult.Error(
                    code = response.messageCode,
                    message = response.messageText
                )
            }
        } catch (e: Exception) {
            ApiResult.Error(
                code = 500,
                message = e.message ?: "Terjadi kesalahan saat mengambil detail berita"
            )
        }
    }

    suspend fun getNewsByCategory(category: String): ApiResult<List<News>> {
        return getNews(category = category)
    }
}