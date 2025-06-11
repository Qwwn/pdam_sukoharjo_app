package com.metromultindo.tirtamakmur.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metromultindo.tirtamakmur.data.datastore.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage

    private val _hasCompletedOnboarding = MutableStateFlow(false)
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding

    init {
        checkOnboardingStatus()
    }

    fun setCurrentPage(page: Int) {
        _currentPage.value = page
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            userPreferences.saveOnboardingCompleted(true)
            _hasCompletedOnboarding.value = true
        }
    }

    fun checkOnboardingStatus() {
        viewModelScope.launch {
            _hasCompletedOnboarding.value = userPreferences.onboardingCompleted.first()
        }
    }
}