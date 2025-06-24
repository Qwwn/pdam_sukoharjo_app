// NewsViewModel.kt - Updated without category filter
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
class NewsViewModel @Inject constructor(
    private val newsRepository: NewsRepository
) : ViewModel() {

    private val _news = MutableStateFlow<List<News>>(emptyList())
    val news: StateFlow<List<News>> = _news

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorState = MutableStateFlow<Pair<Int, String>?>(null)
    val errorState: StateFlow<Pair<Int, String>?> = _errorState

    // Pagination state
    private val _hasMoreData = MutableStateFlow(true)
    val hasMoreData: StateFlow<Boolean> = _hasMoreData

    private var currentOffset = 0
    private val pageSize = 10

    init {
        loadNews()
    }

    fun loadNews(refresh: Boolean = false) {
        if (refresh) {
            currentOffset = 0
            _news.value = emptyList()
            _hasMoreData.value = true
        }

        if (!_hasMoreData.value && !refresh) return

        _isLoading.value = true
        _errorState.value = null

        viewModelScope.launch {
            val result = newsRepository.getNews(
                limit = pageSize,
                offset = currentOffset,
                status = 1 // Only active news
            )

            _isLoading.value = false

            when (result) {
                is ApiResult.Success -> {
                    val newData = result.data

                    if (refresh || currentOffset == 0) {
                        _news.value = newData
                    } else {
                        _news.value = _news.value + newData
                    }

                    // Update pagination
                    currentOffset += newData.size
                    _hasMoreData.value = newData.size == pageSize
                }
                is ApiResult.Error -> {
                    _errorState.value = Pair(result.code, result.message)
                }
            }
        }
    }

    fun loadMoreNews() {
        if (!_isLoading.value && _hasMoreData.value) {
            loadNews(refresh = false)
        }
    }

    fun refreshNews() {
        loadNews(refresh = true)
    }

    fun clearError() {
        _errorState.value = null
    }
}