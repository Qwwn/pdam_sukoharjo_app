package com.metromultindo.pdam_app_v2.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metromultindo.pdam_app_v2.data.datastore.UserPreferences
import com.metromultindo.pdam_app_v2.data.repository.ApiResult
import com.metromultindo.pdam_app_v2.data.repository.BillRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val billRepository: BillRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _customerNumber = MutableStateFlow("")
    val customerNumber: StateFlow<String> = _customerNumber

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorState = MutableStateFlow<Pair<Int, String>?>(null)
    val errorState: StateFlow<Pair<Int, String>?> = _errorState

    private val _loginSuccess = MutableStateFlow(false)
    val loginSuccess: StateFlow<Boolean> = _loginSuccess

    init {
        // Check if user is already logged in
        viewModelScope.launch {
            userPreferences.customerNumber.collect { savedNumber ->
                if (!savedNumber.isNullOrEmpty()) {
                    _customerNumber.value = savedNumber
                }
            }
        }
    }

    fun updateCustomerNumber(number: String) {
        _customerNumber.value = number
    }

    fun login() {
        if (_customerNumber.value.isEmpty()) {
            _errorState.value = Pair(0, "Nomor pelanggan tidak boleh kosong")
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                // Lakukan API call untuk mendapatkan info tagihan
                val result = billRepository.getBillInfo(_customerNumber.value)
                _isLoading.value = false

                when (result) {
                    is ApiResult.Success -> {
                        // Simpan data pelanggan dari hasil API
                        userPreferences.saveCustomerNumber(_customerNumber.value)
                        userPreferences.saveCustomerName(result.data.cust_name)

                        _loginSuccess.value = true
                    }
                    is ApiResult.Error -> {
                        _errorState.value = Pair(result.code, result.message)
                    }
                }
            } catch (e: Exception) {
                _errorState.value = Pair(-1, e.message ?: "Error saat login")
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorState.value = null
    }

    fun resetLoginSuccess() {
        _loginSuccess.value = false
    }
}