package com.metromultindo.pdam_app_v2.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metromultindo.pdam_app_v2.data.datastore.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _customerName = MutableStateFlow<String?>(null)
    val customerName: StateFlow<String?> = _customerName

    private val _customerNumber = MutableStateFlow<String?>(null)
    val customerNumber: StateFlow<String?> = _customerNumber

    private val _isLoggedOut = MutableStateFlow(false)
    val isLoggedOut: StateFlow<Boolean> = _isLoggedOut

    init {
        loadUserInfo()
    }

    // Make public so it can be called when returning to Home screen
    fun loadUserInfo() {
        viewModelScope.launch {
            _customerName.value = userPreferences.customerName.first()
            _customerNumber.value = userPreferences.customerNumber.first()
        }
    }

    fun logout() {
        viewModelScope.launch {
            // Clear user data in preferences
            userPreferences.clearUserData()

            // Clear local state values
            _customerName.value = null
            _customerNumber.value = null

            // Set logged out flag
            _isLoggedOut.value = true
        }
    }

    fun resetLogoutState() {
        _isLoggedOut.value = false
    }
}