package com.metromultindo.tirtamakmur.ui.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metromultindo.tirtamakmur.data.datastore.UserPreferences
import com.metromultindo.tirtamakmur.data.model.CustomerResponse
import com.metromultindo.tirtamakmur.data.repository.ApiResult
import com.metromultindo.tirtamakmur.data.repository.BillRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BillHistoryViewModel @Inject constructor(
    private val billRepository: BillRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _customerData = MutableStateFlow<CustomerResponse?>(null)
    val customerData: StateFlow<CustomerResponse?> = _customerData

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorState = MutableStateFlow<Pair<Int, String>?>(null)
    val errorState: StateFlow<Pair<Int, String>?> = _errorState

    // Customer number retrieved from preferences
    private var customerNumber: String? = null

    init {
        viewModelScope.launch {
            // Get saved customer number from preferences
            customerNumber = userPreferences.customerNumber.first()

            // Only load data if customer number is available
            customerNumber?.let {
                loadBillHistory()
            } ?: run {
                _isLoading.value = false
                _errorState.value = Pair(0, "ID Pel / No Samb tidak ditemukan. Silakan login kembali.")
            }
        }
    }

    fun loadBillHistory() {
        if (customerNumber.isNullOrEmpty()) return

        _isLoading.value = true
        _errorState.value = null

        viewModelScope.launch {
            val result = billRepository.getBillInfo(customerNumber!!)
            _isLoading.value = false

            when (result) {
                is ApiResult.Success -> {
                    _customerData.value = result.data
                }
                is ApiResult.Error -> {
                    _errorState.value = Pair(result.code, result.message)
                }
            }
        }
    }

    fun clearError() {
        _errorState.value = null
    }
}