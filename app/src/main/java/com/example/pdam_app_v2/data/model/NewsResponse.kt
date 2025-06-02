// NewsResponse.kt
package com.example.pdam_app_v2.data.model

import com.google.gson.annotations.SerializedName

data class NewsResponse(
    @SerializedName("error")
    val error: Boolean,
    @SerializedName("message_code")
    val messageCode: Int,
    @SerializedName("message_text")
    val messageText: String,
    @SerializedName("pagination")
    val pagination: Pagination,
    @SerializedName("news_data")
    val newsData: List<NewsData>
)

data class Pagination(
    @SerializedName("total_records")
    val totalRecords: Int,
    @SerializedName("limit")
    val limit: Int,
    @SerializedName("offset")
    val offset: Int,
    @SerializedName("fetched")
    val fetched: Int
)

data class NewsData(
    @SerializedName("news_id")
    val newsId: Int,
    @SerializedName("news_title")
    val newsTitle: String,
    @SerializedName("news_content")
    val newsContent: String,
    @SerializedName("news_date")
    val newsDate: String,
    @SerializedName("news_author")
    val newsAuthor: String,
    @SerializedName("news_image")
    val newsImage: String?,
    @SerializedName("news_category")
    val newsCategory: String?,
    @SerializedName("news_status")
    val newsStatus: Int,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String?
)

// Single News Response for detail
data class SingleNewsResponse(
    @SerializedName("error")
    val error: Boolean,
    @SerializedName("message_code")
    val messageCode: Int,
    @SerializedName("message_text")
    val messageText: String,
    @SerializedName("news")
    val news: NewsData
)