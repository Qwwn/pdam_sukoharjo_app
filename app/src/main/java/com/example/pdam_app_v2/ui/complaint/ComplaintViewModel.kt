package com.example.pdam_app_v2.ui.complaint

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdam_app_v2.data.repository.ApiResult
import com.example.pdam_app_v2.data.repository.ComplaintRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ComplaintViewModel @Inject constructor(
    private val complaintRepository: ComplaintRepository
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
                    _errorState.value = Pair(400, "Nama harus diisi")
                    _isLoading.value = false
                    return@launch
                }

                if (isCustomer && (customerNumber.isNullOrEmpty())) {
                    _errorState.value = Pair(400, "Nomor sambungan harus diisi untuk pelanggan")
                    _isLoading.value = false
                    return@launch
                }

                if (address.isEmpty()) {
                    _errorState.value = Pair(400, "Alamat harus diisi")
                    _isLoading.value = false
                    return@launch
                }

                if (phoneNumber.isEmpty()) {
                    _errorState.value = Pair(400, "Nomor telepon harus diisi")
                    _isLoading.value = false
                    return@launch
                }

                if (complaintText.isEmpty()) {
                    _errorState.value = Pair(400, "Isi pengaduan harus diisi")
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

                Log.d(TAG, "Submitting complaint with location: lat=$latitude, lng=$longitude")

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
    }
}