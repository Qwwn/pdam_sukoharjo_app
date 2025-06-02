package com.example.pdam_app_v2.data.model

import java.text.SimpleDateFormat
import java.util.*

data class News(
    val id: Int,
    val title: String,
    val content: String,
    val date: String,
    val author: String,
    val imageUrl: String? = null,
    val category: String? = null,
    val status: Int,
    val createdAt: String,
    val updatedAt: String?
) {
    // Helper function to format date for display
    fun getFormattedDate(): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val date = inputFormat.parse(date)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            date // Return original if parsing fails
        }
    }

    // Helper function to format time for display
    fun getFormattedTime(): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val date = inputFormat.parse(date)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            "" // Return empty if parsing fails
        }
    }

    // Helper function to get formatted category
    fun getDisplayCategory(): String {
        return when (category) {
            "blokir" -> "Pemblokiran"
            "putus" -> "Pemutusan"
            "0" -> "Umum"
            null -> "Tidak Berkategori"
            else -> category.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        }
    }
}

// Extension function to convert NewsData to News
fun NewsData.toNews(): News {
    return News(
        id = this.newsId,
        title = this.newsTitle,
        content = this.newsContent,
        date = this.newsDate,
        author = this.newsAuthor,
        imageUrl = this.newsImage?.let { imageUrl ->
            // Fix localhost URL for Android emulator or real device
            when {
                imageUrl.startsWith("http://localhost/") ->
                    imageUrl.replace("http://localhost/", "http://skhj.ddns.net:81/")
                imageUrl.startsWith("http://127.0.0.1/") ->
                    imageUrl.replace("http://127.0.0.1/", "http://skhj.ddns.net:81/")
                else -> imageUrl
            }
        },
        category = this.newsCategory,
        status = this.newsStatus,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}