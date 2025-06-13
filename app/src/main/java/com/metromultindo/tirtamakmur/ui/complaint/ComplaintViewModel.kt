package com.metromultindo.tirtamakmur.ui.complaint

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metromultindo.tirtamakmur.data.model.CustomerResponse
import com.metromultindo.tirtamakmur.data.repository.ApiResult
import com.metromultindo.tirtamakmur.data.repository.ComplaintRepository
import com.metromultindo.tirtamakmur.data.repository.SelfMeterRepository
import com.metromultindo.tirtamakmur.ui.navigation.Screen.CustomerInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ComplaintViewModel @Inject constructor(
    private val complaintRepository: ComplaintRepository,
    private val selfMeterRepository: SelfMeterRepository // Tambahkan ini untuk update phone
) : ViewModel() {
    private val TAG = "ComplaintViewModel"

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorState = MutableStateFlow<Pair<Int, String>?>(null)
    val errorState: StateFlow<Pair<Int, String>?> = _errorState

    private val _isCustomer = MutableStateFlow(true)
    val isCustomer: StateFlow<Boolean> = _isCustomer

    private val _complaintSubmitted = MutableStateFlow(false)
    val complaintSubmitted: StateFlow<Boolean> = _complaintSubmitted

    // Customer info state (menggunakan CustomerResponse sesuai repository)
    private val _customerInfo = MutableStateFlow<CustomerResponse?>(null)
    val customerInfo: StateFlow<CustomerResponse?> = _customerInfo

    // Support for bitmap image from camera
    private val _bitmapImage = mutableStateOf<ImageBitmap?>(null)
    val bitmapImage: State<ImageBitmap?> = _bitmapImage

    // Location states
    private val _currentLatitude = MutableStateFlow<Double?>(null)
    val currentLatitude: StateFlow<Double?> = _currentLatitude

    private val _currentLongitude = MutableStateFlow<Double?>(null)
    val currentLongitude: StateFlow<Double?> = _currentLongitude

    private val _locationStatus = MutableStateFlow("Belum diambil")
    val locationStatus: StateFlow<String> = _locationStatus

    // Phone update states - TAMBAHAN BARU
    private val _phoneUpdateSuccess = MutableStateFlow(false)
    val phoneUpdateSuccess: StateFlow<Boolean> = _phoneUpdateSuccess

    private val _phoneUpdateLoading = MutableStateFlow(false)
    val phoneUpdateLoading: StateFlow<Boolean> = _phoneUpdateLoading

    // TAMBAHAN - Fungsi validasi nomor WhatsApp
    private fun isValidWhatsAppNumber(phoneNumber: String): Boolean {
        val cleanNumber = phoneNumber.replace(" ", "").replace("-", "")
        return when {
            cleanNumber.startsWith("0") && cleanNumber.length >= 9 -> true
            cleanNumber.startsWith("62") && cleanNumber.length >= 10 -> true
            cleanNumber.startsWith("+62") && cleanNumber.length >= 11 -> true
            else -> false
        }
    }

    fun setIsCustomer(isCustomer: Boolean) {
        _isCustomer.value = isCustomer
    }

    fun setBitmapImage(bitmap: Bitmap) {
        _bitmapImage.value = bitmap.asImageBitmap()
    }

    fun clearBitmapImage() {
        _bitmapImage.value = null
    }

    fun setLocation(latitude: Double?, longitude: Double?) {
        _currentLatitude.value = latitude
        _currentLongitude.value = longitude
    }

    fun setLocationStatus(status: String) {
        _locationStatus.value = status
    }

    fun clearLocation() {
        _currentLatitude.value = null
        _currentLongitude.value = null
        _locationStatus.value = "Belum diambil"
    }

    // Load customer info menggunakan complaintRepository
    fun loadCustomerInfo(customerNumber: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorState.value = null

            try {
                val result = complaintRepository.getCustomerInfo(customerNumber)
                result.fold(
                    onSuccess = { customerResponse ->
                        _customerInfo.value = customerResponse
                        Log.d(TAG, "Customer info loaded successfully: ${customerResponse.cust_name}")
                    },
                    onFailure = { exception ->
                        Log.e("SelfMeterViewModel", "Error loading customer info", exception)
                        _errorState.value = Pair(500, "Periksa nomor Id Pelanggan, Pastikan nomor Id Pelanggan sudah benar")
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

    // FUNGSI BARU - Update customer phone dengan validasi
    fun updateCustomerPhone(customerNumber: String, phone: String) {
        viewModelScope.launch {
            _phoneUpdateLoading.value = true
            _errorState.value = null

            // Validasi format nomor WhatsApp
            if (!isValidWhatsAppNumber(phone)) {
                _errorState.value = Pair(400, "Format nomor WhatsApp tidak valid. Harus diawali 0 atau +62 dan minimal 8 digit")
                _phoneUpdateLoading.value = false
                return@launch
            }

            try {
                val result = selfMeterRepository.updateCustomerPhone(customerNumber, phone)
                result.fold(
                    onSuccess = { response ->
                        if (!response.error) {
                            _phoneUpdateSuccess.value = true
                            // Tidak perlu update CustomerResponse object
                            // UI akan menggunakan editPhoneNumber state yang sudah diupdate di ComplaintScreen
                            Log.d(TAG, "Phone updated successfully to: $phone")
                        } else {
                            _errorState.value = Pair(response.messageCode, response.messageText)
                        }
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Error updating phone", exception)
                        _errorState.value = Pair(500, "Gagal memperbarui nomor telepon")
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error", e)
                _errorState.value = Pair(500, "Terjadi kesalahan: ${e.message}")
            } finally {
                _phoneUpdateLoading.value = false
            }
        }
    }

    // Clear customer info
    fun clearCustomerInfo() {
        _customerInfo.value = null
    }

    fun submitComplaint(
        name: String,
        customerName: String,
        isCustomer: Boolean,
        customerNumber: String?,
        phoneNumber: String,
        complaintText: String,
        address: String,
        imageUri: Uri? = null,
        cameraFilePath: String? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                // Determine the name to use (use customer name if customer)
                val nameToUse = if (isCustomer && customerName.isNotEmpty()) customerName else name

                // Validate inputs
                if (nameToUse.isEmpty()) {
                    Log.e(TAG, "Validation failed: Name is empty")
                    _errorState.value = Pair(400, "Nama harus diisi")
                    _isLoading.value = false
                    return@launch
                }

                if (isCustomer && (customerNumber.isNullOrEmpty())) {
                    Log.e(TAG, "Validation failed: Customer number is empty for customer")
                    _errorState.value = Pair(400, "Id Pel / No Samb harus diisi untuk pelanggan")
                    _isLoading.value = false
                    return@launch
                }

                if (address.isEmpty()) {
                    Log.e(TAG, "Validation failed: Address is empty")
                    _errorState.value = Pair(400, "Alamat harus diisi")
                    _isLoading.value = false
                    return@launch
                }

                if (phoneNumber.isEmpty()) {
                    Log.e(TAG, "Validation failed: Phone number is empty")
                    _errorState.value = Pair(400, "Nomor telepon harus diisi")
                    _isLoading.value = false
                    return@launch
                }

                // TAMBAHAN - Validasi format nomor WhatsApp
                if (!isValidWhatsAppNumber(phoneNumber)) {
                    Log.e(TAG, "Validation failed: Invalid WhatsApp number format")
                    _errorState.value = Pair(400, "Format nomor WhatsApp tidak valid. Harus diawali 0 atau +62 dan minimal 8 digit")
                    _isLoading.value = false
                    return@launch
                }

                if (complaintText.isEmpty()) {
                    Log.e(TAG, "Validation failed: Complaint text is empty")
                    _errorState.value = Pair(400, "Isi pengaduan harus diisi")
                    _isLoading.value = false
                    return@launch
                }

                // TAMBAHAN - Validasi wajib foto dan lokasi
                if (imageUri == null) {
                    Log.e(TAG, "Validation failed: Image is required")
                    _errorState.value = Pair(400, "Foto bukti wajib disertakan")
                    _isLoading.value = false
                    return@launch
                }

                if (latitude == null || longitude == null) {
                    Log.e(TAG, "Validation failed: Location is required")
                    _errorState.value = Pair(400, "Lokasi wajib disertakan")
                    _isLoading.value = false
                    return@launch
                }

                // Validate location coordinates if provided (basic validation)
                if (latitude != null && longitude != null) {
                    // Basic Indonesia boundary check
                    if (latitude < -11.0 || latitude > 6.0 || longitude < 95.0 || longitude > 141.0) {
                        Log.w(TAG, "Location coordinates seem to be outside Indonesia: $latitude, $longitude")
                        // Don't fail, just log warning
                    }
                }

                val result = complaintRepository.submitComplaint(
                    name = nameToUse,
                    address = address,
                    phone = phoneNumber,
                    message = complaintText,
                    imageUri = imageUri,
                    isCustomer = isCustomer,
                    customerNo = if (isCustomer) customerNumber else null,
                    cameraFilePath = cameraFilePath,
                    latitude = latitude,
                    longitude = longitude
                )

                when (result) {
                    is ApiResult.Success -> {
                        Log.d(TAG, "Complaint submitted successfully with location data")
                        _complaintSubmitted.value = true
                        // Clear location after successful submission
                        clearLocation()
                        // Clear customer info after successful submission
                        clearCustomerInfo()
                    }
                    is ApiResult.Error -> {
                        Log.e(TAG, "Error submitting complaint: ${result.code} - ${result.message}")
                        _errorState.value = Pair(result.code, result.message)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception submitting complaint", e)
                _errorState.value = Pair(-1, e.message ?: "Error submitting complaint")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setError(code: Int, message: String) {
        _errorState.value = Pair(code, message)
    }

    fun clearError() {
        _errorState.value = null
    }

    fun resetSubmissionState() {
        _complaintSubmitted.value = false
        clearLocation()
        clearCustomerInfo()
    }

    // FUNGSI BARU - Reset phone update state
    fun resetPhoneUpdateState() {
        _phoneUpdateSuccess.value = false
    }
}