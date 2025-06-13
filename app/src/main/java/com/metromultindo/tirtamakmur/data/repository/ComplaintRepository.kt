package com.metromultindo.tirtamakmur.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.metromultindo.tirtamakmur.data.api.ApiService
import com.metromultindo.tirtamakmur.data.api.RetrofitInstance
import com.metromultindo.tirtamakmur.data.model.ComplaintResponse
import com.metromultindo.tirtamakmur.data.model.CustomerResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class ComplaintRepository @Inject constructor(
    private val apiService: ApiService,
    @ApplicationContext private val context: Context
) {
    private val TAG = "ComplaintRepository"
    suspend fun getCustomerInfo(customerNumber: String): Result<CustomerResponse> {
        return try {
            val response = apiService.getBillInfo(customerNumber)
            Result.success(response)
        } catch (e: Exception) {
            Log.e("SelfMeterRepository", "Error getting customer info", e)
            Result.failure(e)
        }
    }
    suspend fun submitComplaint(
        name: String,
        address: String,
        phone: String,
        message: String,
        imageUri: Uri? = null,
        isCustomer: Boolean = false,
        customerNo: String? = null,
        cameraFilePath: String? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ): ApiResult<ComplaintResponse> {
        return try {
            Log.d(TAG, "Preparing to submit complaint")
            Log.d(TAG, "Image URI: $imageUri, Camera path: $cameraFilePath")
            Log.d(TAG, "Location: lat=$latitude, lng=$longitude")

            // Create request parts
            val namePart = name.toRequestBody("text/plain".toMediaTypeOrNull())
            val addressPart = address.toRequestBody("text/plain".toMediaTypeOrNull())
            val phonePart = phone.toRequestBody("text/plain".toMediaTypeOrNull())
            val messagePart = message.toRequestBody("text/plain".toMediaTypeOrNull())

            // Create is_customer part (1 = customer, 2 = non-customer)
            val isCustomerPart = (if (isCustomer) "1" else "2").toRequestBody("text/plain".toMediaTypeOrNull())

            // Create customer_no part (only when is_customer = 1)
            val customerNoPart = if (isCustomer && !customerNo.isNullOrEmpty()) {
                customerNo.toRequestBody("text/plain".toMediaTypeOrNull())
            } else {
                null
            }

            // Create location parts
            val latitudePart = latitude?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            val longitudePart = longitude?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())

            // Log customer info and location for debugging
            Log.d(TAG, "Customer Status: ${if (isCustomer) "Customer" else "Non-Customer"}")
            Log.d(TAG, "Customer Number: ${customerNo ?: "None"}")
            Log.d(TAG, "Location coordinates: ${latitude ?: "N/A"}, ${longitude ?: "N/A"}")

            // Process image with compression
            var imagePart: MultipartBody.Part? = null

            // Process image based on source
            var sourceFile: File? = null

            // First try camera path
            if (cameraFilePath != null && cameraFilePath.isNotEmpty()) {
                val file = File(cameraFilePath)
                if (file.exists() && file.length() > 0) {
                    Log.d(TAG, "Using camera file: ${file.absolutePath}, size: ${file.length()}")
                    sourceFile = file
                } else {
                    Log.e(TAG, "Camera file doesn't exist or is empty: $cameraFilePath")
                }
            }

            // If no source file yet, try from URI
            if (sourceFile == null && imageUri != null) {
                try {
                    val tempFile = File(context.cacheDir, "temp_image.jpg")
                    context.contentResolver.openInputStream(imageUri)?.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }

                    if (tempFile.exists() && tempFile.length() > 0) {
                        Log.d(TAG, "Created file from URI: ${tempFile.absolutePath}, size=${tempFile.length()}")
                        sourceFile = tempFile
                    } else {
                        Log.e(TAG, "Failed to create valid file from URI")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating file from URI", e)
                }
            }

            // Compress the image if we have a source file
            if (sourceFile != null) {
                try {
                    // Create output file for compressed image
                    val compressedFile = File(context.cacheDir, "compressed_image.jpg")
                    if (compressedFile.exists()) {
                        compressedFile.delete()
                    }

                    // Load bitmap and compress it
                    val options = BitmapFactory.Options().apply {
                        // If image is too large, sample it down first
                        if (sourceFile.length() > 4_000_000) { // 4MB
                            inSampleSize = 2 // Sample down by factor of 2
                        }
                    }

                    val bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath, options)
                    if (bitmap != null) {
                        val out = FileOutputStream(compressedFile)
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, out) // 75% quality
                        out.flush()
                        out.close()

                        Log.d(TAG, "Original size: ${sourceFile.length()}, Compressed size: ${compressedFile.length()}")

                        // Create multipart with specific name expected by server
                        val requestFile = compressedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                        imagePart = MultipartBody.Part.createFormData(
                            "image",  // Must match server's expected field name exactly
                            "image.jpg",  // Simple filename
                            requestFile
                        )
                        Log.d(TAG, "Created compressed image part")
                    } else {
                        Log.e(TAG, "Failed to decode bitmap from file")

                        // Fallback to direct file upload without compression
                        val requestFile = sourceFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                        imagePart = MultipartBody.Part.createFormData(
                            "image",
                            "image.jpg",
                            requestFile
                        )
                        Log.d(TAG, "Created uncompressed image part as fallback")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error compressing image", e)

                    // Fallback to direct file upload without compression
                    try {
                        val requestFile = sourceFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                        imagePart = MultipartBody.Part.createFormData(
                            "image",
                            "image.jpg",
                            requestFile
                        )
                        Log.d(TAG, "Created uncompressed image part after compression error")
                    } catch (e2: Exception) {
                        Log.e(TAG, "Failed to create image part: ${e2.message}")
                    }
                }
            }

            // Log image part status
            Log.d(TAG, "Image part created: ${imagePart != null}")

            // Make API call with location data
            Log.d(TAG, "Making API call to submit complaint with location data")
            val response = RetrofitInstance.api.submitComplaint(
                name = namePart,
                address = addressPart,
                phone = phonePart,
                message = messagePart,
                isCustomer = isCustomerPart,
                customerNo = customerNoPart,
                latitude = latitudePart,
                longitude = longitudePart,
                image = imagePart
            )

            Log.d(TAG, "Response received: ${response.message_code}, ${response.message_text}")
            Log.d(TAG, "Image URL in response: ${response.complaint?.image ?: "null"}")

            // Check for errors
            if (response.error || response.message_code != 910) {
                Log.e(TAG, "API returned error: ${response.message_text}")
                ApiResult.Error(response.message_code, response.message_text)
            } else {
                Log.d(TAG, "Complaint submitted successfully: ${response.complaint?.number}")
                ApiResult.Success(response)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception when submitting complaint", e)
            ApiResult.Error(-1, e.message ?: "Unknown error occurred")
        }
    }

    // Helper method to create directory if it doesn't exist
    private fun ensureDirectoryExists(directory: File): Boolean {
        return if (!directory.exists()) {
            directory.mkdirs()
        } else {
            true
        }
    }
}