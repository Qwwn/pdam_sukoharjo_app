// SelfMeterViewModel.kt - Updated with combined load and phone update functionality
package com.metromultindo.pdam_app_v2.ui.selfmeter

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metromultindo.pdam_app_v2.data.datastore.UserPreferences
import com.metromultindo.pdam_app_v2.data.model.SelfMeterCustomerInfo
import com.metromultindo.pdam_app_v2.data.model.SelfMeterRequest
import com.metromultindo.pdam_app_v2.data.repository.SelfMeterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SelfMeterViewModel @Inject constructor(
    private val repository: SelfMeterRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorState = MutableStateFlow<Pair<Int, String>?>(null)
    val errorState: StateFlow<Pair<Int, String>?> = _errorState

    private val _customerInfo = MutableStateFlow<SelfMeterCustomerInfo?>(null)
    val customerInfo: StateFlow<SelfMeterCustomerInfo?> = _customerInfo

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _submissionSuccess = MutableStateFlow(false)
    val submissionSuccess: StateFlow<Boolean> = _submissionSuccess

    // State untuk phone update
    private val _phoneUpdateSuccess = MutableStateFlow(false)
    val phoneUpdateSuccess: StateFlow<Boolean> = _phoneUpdateSuccess

    private val _phoneUpdateLoading = MutableStateFlow(false)
    val phoneUpdateLoading: StateFlow<Boolean> = _phoneUpdateLoading

    init {
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            val customerNumber = userPreferences.customerNumber.first()
            val customerName = userPreferences.customerName.first()

            if (!customerNumber.isNullOrEmpty() && !customerName.isNullOrEmpty()) {
                _isLoggedIn.value = true
                // Auto load customer info if logged in
                loadCustomerInfo(customerNumber)
            } else {
                _isLoggedIn.value = false
            }
        }
    }

    fun loadCustomerInfo(customerNumber: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorState.value = null

            try {
                val result = repository.getCustomerInfo(customerNumber)
                result.fold(
                    onSuccess = { response ->
                        if (!response.error) {
                            // Get customer info from the response
                            val custName = if (response.cust_name == "-" && response.cust_data.isNotEmpty()) {
                                response.cust_data.first().name
                            } else {
                                response.cust_name ?: "Tidak diketahui"
                            }

                            val custCode = if (response.cust_code == "-" && response.cust_data.isNotEmpty()) {
                                response.cust_data.first().customerNumber
                            } else {
                                response.cust_code ?: customerNumber
                            }

                            val custAddress = if (response.cust_address == "-" && response.cust_data.isNotEmpty()) {
                                response.cust_data.first().address
                            } else {
                                response.cust_address ?: "Tidak diketahui"
                            }

                            // Handle phone number
                            val custPhone = response.cust_phone ?: ""

                            // Handle null tarif_class properly
                            val custTariffClass = when {
                                response.cust_data.isNotEmpty() -> {
                                    response.cust_data.first().tariffClass ?: "Tidak diketahui"
                                }
                                !response.tarif_class.isNullOrEmpty() && response.tarif_class != "-" -> {
                                    response.tarif_class
                                }
                                else -> "Tidak diketahui"
                            }

                            // Handle startMeter and convert to String
                            val custStartMeter = if (response.cust_data.isNotEmpty()) {
                                response.cust_data.first().startMeter.toString()
                            } else {
                                "Tidak diketahui"
                            }

                            _customerInfo.value = SelfMeterCustomerInfo(
                                custCode = custCode,
                                name = custName,
                                address = custAddress,
                                tariffClass = custTariffClass,
                                startMeter = custStartMeter,
                                phone = custPhone
                            )
                        } else {
                            _errorState.value = Pair(response.message_code, response.message_text)
                        }
                    },
                    onFailure = { exception ->
                        Log.e("SelfMeterViewModel", "Error loading customer info", exception)
                        _errorState.value = Pair(500, "Gagal memuat data pelanggan")
                    }
                )
            } catch (e: Exception) {
                Log.e("SelfMeterViewModel", "Unexpected error", e)
                _errorState.value = Pair(500, "Terjadi kesalahan: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // NEW METHOD: Combined load customer info and update phone
    fun loadCustomerInfoWithPhoneUpdate(customerNumber: String, phoneNumber: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorState.value = null

            try {
                // Step 1: Load customer info
                Log.d("SelfMeterViewModel", "Loading customer info for: $customerNumber")
                val customerResult = repository.getCustomerInfo(customerNumber)

                customerResult.fold(
                    onSuccess = { response ->
                        if (!response.error) {
                            // Parse customer data
                            val custName = if (response.cust_name == "-" && response.cust_data.isNotEmpty()) {
                                response.cust_data.first().name
                            } else {
                                response.cust_name ?: "Tidak diketahui"
                            }

                            val custCode = if (response.cust_code == "-" && response.cust_data.isNotEmpty()) {
                                response.cust_data.first().customerNumber
                            } else {
                                response.cust_code ?: customerNumber
                            }

                            val custAddress = if (response.cust_address == "-" && response.cust_data.isNotEmpty()) {
                                response.cust_data.first().address
                            } else {
                                response.cust_address ?: "Tidak diketahui"
                            }

                            val custTariffClass = when {
                                response.cust_data.isNotEmpty() -> {
                                    response.cust_data.first().tariffClass ?: "Tidak diketahui"
                                }
                                !response.tarif_class.isNullOrEmpty() && response.tarif_class != "-" -> {
                                    response.tarif_class
                                }
                                else -> "Tidak diketahui"
                            }

                            val custStartMeter = if (response.cust_data.isNotEmpty()) {
                                response.cust_data.first().startMeter.toString()
                            } else {
                                "Tidak diketahui"
                            }

                            Log.d("SelfMeterViewModel", "Customer data loaded successfully, updating phone number")

                            // Step 2: Update phone number
                            val phoneResult = repository.updateCustomerPhone(custCode, phoneNumber)

                            phoneResult.fold(
                                onSuccess = { phoneResponse ->
                                    if (!phoneResponse.error) {
                                        // Success: Set customer info with updated phone
                                        _customerInfo.value = SelfMeterCustomerInfo(
                                            custCode = custCode,
                                            name = custName,
                                            address = custAddress,
                                            tariffClass = custTariffClass,
                                            startMeter = custStartMeter,
                                            phone = phoneNumber // Use the new phone number
                                        )

                                        _phoneUpdateSuccess.value = true
                                        Log.d("SelfMeterViewModel", "Customer loaded and phone updated successfully")
                                    } else {
                                        _errorState.value = Pair(phoneResponse.messageCode, phoneResponse.messageText)
                                    }
                                },
                                onFailure = { exception ->
                                    Log.e("SelfMeterViewModel", "Error updating phone", exception)
                                    _errorState.value = Pair(500, "Data pelanggan berhasil dimuat, tetapi gagal memperbarui nomor WhatsApp")

                                    // Still set customer info even if phone update failed
                                    _customerInfo.value = SelfMeterCustomerInfo(
                                        custCode = custCode,
                                        name = custName,
                                        address = custAddress,
                                        tariffClass = custTariffClass,
                                        startMeter = custStartMeter,
                                        phone = response.cust_phone ?: ""
                                    )
                                }
                            )
                        } else {
                            _errorState.value = Pair(response.message_code, response.message_text)
                        }
                    },
                    onFailure = { exception ->
                        Log.e("SelfMeterViewModel", "Error loading customer info", exception)
                        _errorState.value = Pair(500, "Gagal memuat data pelanggan")
                    }
                )
            } catch (e: Exception) {
                Log.e("SelfMeterViewModel", "Unexpected error", e)
                _errorState.value = Pair(500, "Terjadi kesalahan: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Update customer phone (existing method)
    fun updateCustomerPhone(customerNumber: String, phone: String) {
        viewModelScope.launch {
            _phoneUpdateLoading.value = true
            _errorState.value = null

            try {
                val result = repository.updateCustomerPhone(customerNumber, phone)
                result.fold(
                    onSuccess = { response ->
                        if (!response.error) {
                            _phoneUpdateSuccess.value = true

                            // Update current customer info with new phone
                            _customerInfo.value?.let { currentInfo ->
                                _customerInfo.value = currentInfo.copy(phone = phone)
                            }

                            Log.d("SelfMeterViewModel", "Phone updated successfully to: $phone")
                        } else {
                            _errorState.value = Pair(response.messageCode, response.messageText)
                        }
                    },
                    onFailure = { exception ->
                        Log.e("SelfMeterViewModel", "Error updating phone", exception)
                        _errorState.value = Pair(500, "Gagal memperbarui nomor telepon}")
                    }
                )
            } catch (e: Exception) {
                Log.e("SelfMeterViewModel", "Unexpected error", e)
                _errorState.value = Pair(500, "Terjadi kesalahan: ${e.message}")
            } finally {
                _phoneUpdateLoading.value = false
            }
        }
    }

    fun submitMeterReading(
        customerNumber: String,
        standMeter: Int,
        imageUri: Uri?,
        cameraFilePath: String?,
        latitude: Double?,
        longitude: Double?
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorState.value = null

            try {
                val request = SelfMeterRequest(
                    custCode = customerNumber,
                    standMeter = standMeter,
                    latitude = latitude,
                    longitude = longitude,
                    imageUri = imageUri,
                    cameraFilePath = cameraFilePath
                )

                val result = repository.submitSelfMeterReading(request)
                result.fold(
                    onSuccess = { response ->
                        if (!response.error) {
                            _submissionSuccess.value = true
                            Log.d("SelfMeterViewModel", "Self meter reading submitted successfully")
                        } else {
                            _errorState.value = Pair(response.messageCode, response.messageText)
                        }
                    },
                    onFailure = { exception ->
                        Log.e("SelfMeterViewModel", "Error submitting meter reading", exception)
                        _errorState.value = Pair(500, "Gagal mengirim data")
                    }
                )
            } catch (e: Exception) {
                Log.e("SelfMeterViewModel", "Unexpected error", e)
                _errorState.value = Pair(500, "Terjadi kesalahan: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorState.value = null
    }

    fun resetSubmissionState() {
        _submissionSuccess.value = false
    }

    fun resetPhoneUpdateState() {
        _phoneUpdateSuccess.value = false
    }

    fun clearCustomerInfo() {
        _customerInfo.value = null
    }
}