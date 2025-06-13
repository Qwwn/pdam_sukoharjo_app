package com.metromultindo.tirtamakmur.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metromultindo.tirtamakmur.data.datastore.UserPreferences
import com.metromultindo.tirtamakmur.data.repository.ApiResult
import com.metromultindo.tirtamakmur.data.repository.BillRepository
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
            _errorState.value = Pair(1, "Mohon masukkan nomor pelanggan")
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            when (val result = billRepository.getBillInfo(_customerNumber.value)) {
                is ApiResult.Success -> {
                    if (result.data.cust_code.isNullOrEmpty()) {
                        _errorState.value = Pair(102, "Pelanggan tidak ditemukan")
                    } else {
                        userPreferences.saveCustomerNumber(_customerNumber.value)
                        userPreferences.saveCustomerName(result.data.cust_name ?: "")
                        _loginSuccess.value = true
                    }
                }
                is ApiResult.Error -> {
                    val userFriendlyMessage = when (result.code) {
                        102 -> "Pelanggan tidak ditemukan. Periksa kembali nomor Anda."
                        in 400..499 -> "Data tidak valid (${result.code})"
                        in 500..599 -> "Server sedang mengalami gangguan"
                        -1 -> "Periksa nomor Id Pelanggan, Pastikan nomor Id Pelanggan sudah benar"
                        else -> result.message ?: "Terjadi kesalahan (${result.code})"
                    }
                    _errorState.value = Pair(result.code, userFriendlyMessage)
                }
            }
            _isLoading.value = false
        }
    }

    private fun isValidCustomerNumber(number: String): Boolean {
        // Contoh validasi: minimal 6 digit angka
        return number.length >= 6 && number.all { it.isDigit() }
    }

    fun clearError() {
        _errorState.value = null
    }

    fun resetLoginSuccess() {
        _loginSuccess.value = false
    }
}