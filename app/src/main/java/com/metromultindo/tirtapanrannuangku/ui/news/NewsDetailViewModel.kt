// NewsDetailViewModel.kt - ViewModel for news detail
package com.metromultindo.tirtapanrannuangku.ui.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metromultindo.tirtapanrannuangku.data.model.News
import com.metromultindo.tirtapanrannuangku.data.repository.ApiResult
import com.metromultindo.tirtapanrannuangku.data.repository.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewsDetailViewModel @Inject constructor(
    private val newsRepository: NewsRepository
) : ViewModel() {

    private val _newsDetail = MutableStateFlow<News?>(null)
    val newsDetail: StateFlow<News?> = _newsDetail

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorState = MutableStateFlow<Pair<Int, String>?>(null)
    val errorState: StateFlow<Pair<Int, String>?> = _errorState

    fun loadNewsDetail(newsId: Int) {
        _isLoading.value = true
        _errorState.value = null

        println("Loading news detail for ID: $newsId") // Debug log

        viewModelScope.launch {
            val result = newsRepository.getNewsById(newsId)
            _isLoading.value = false

            when (result) {
                is ApiResult.Success -> {
                    _newsDetail.value = result.data
                    println("News detail loaded successfully: ${result.data.title}") // Debug log
                }
                is ApiResult.Error -> {
                    _errorState.value = Pair(result.code, result.message)
                    println("Error loading news detail: ${result.message}") // Debug log
                }
            }
        }
    }

    fun clearError() {
        _errorState.value = null
    }
}