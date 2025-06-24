package com.metromultindo.tirtapanrannuangku.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metromultindo.tirtapanrannuangku.data.datastore.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _hasCompletedOnboarding = MutableStateFlow(false)
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding

    init {
        checkLoginStatus()
        checkOnboardingStatus()
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            val customerNumber = userPreferences.customerNumber.first()
            _isLoggedIn.value = !customerNumber.isNullOrEmpty()
        }
    }

    private fun checkOnboardingStatus() {
        viewModelScope.launch {
            _hasCompletedOnboarding.value = userPreferences.onboardingCompleted.first()
        }
    }
}