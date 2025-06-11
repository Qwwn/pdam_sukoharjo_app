// SelfMeterRepository.kt - Updated with phone update functionality
package com.metromultindo.pdam_app_v2.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.metromultindo.pdam_app_v2.data.api.ApiService
import com.metromultindo.pdam_app_v2.data.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SelfMeterRepository @Inject constructor(
    private val apiService: ApiService,
    @ApplicationContext private val context: Context
) {

    suspend fun getCustomerInfo(customerNumber: String): Result<CustomerResponse> {
        return try {
            val response = apiService.getBillInfo(customerNumber)
            Result.success(response)
        } catch (e: Exception) {
            Log.e("SelfMeterRepository", "Error getting customer info", e)
            Result.failure(e)
        }
    }

    // NEW METHOD: Update customer phone
    suspend fun updateCustomerPhone(customerNumber: String, phone: String): Result<UpdatePhoneResponse> {
        return try {
            Log.d("SelfMeterRepository", "Updating phone for customer: $customerNumber to: $phone")
            val response = apiService.updateCustomerPhone(customerNumber, phone)
            Log.d("SelfMeterRepository", "Phone update response: ${response.messageText}")
            Result.success(response)
        } catch (e: Exception) {
            Log.e("SelfMeterRepository", "Error updating customer phone", e)
            Result.failure(e)
        }
    }

    suspend fun submitSelfMeterReading(request: SelfMeterRequest): Result<SelfMeterResponse> {
        return try {
            Log.d("SelfMeterRepository", "=== SUBMIT SELF METER (Backend handles compression) ===")
            Log.d("SelfMeterRepository", "custCode: ${request.custCode}")
            Log.d("SelfMeterRepository", "standMeter: ${request.standMeter}")

            // Prepare multipart form data
            val custCodePart = request.custCode.toRequestBody("text/plain".toMediaTypeOrNull())
            val standMeterPart = request.standMeter.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val latitudePart = request.latitude?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            val longitudePart = request.longitude?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())

            // Light compression client-side to reduce upload time (optional)
            var imagePart: MultipartBody.Part? = null
            var sourceFile: File? = null

            // Get source file
            if (!request.cameraFilePath.isNullOrEmpty()) {
                val file = File(request.cameraFilePath)
                if (file.exists() && file.length() > 0) {
                    Log.d("SelfMeterRepository", "Using camera file: ${file.absolutePath}, size: ${file.length()}")
                    sourceFile = file
                }
            }

            if (sourceFile == null && request.imageUri != null) {
                try {
                    val tempFile = File(context.cacheDir, "temp_meter.jpg")
                    context.contentResolver.openInputStream(request.imageUri)?.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (tempFile.exists() && tempFile.length() > 0) {
                        sourceFile = tempFile
                        Log.d("SelfMeterRepository", "Created temp file from URI: ${tempFile.length()} bytes")
                    }
                } catch (e: Exception) {
                    Log.e("SelfMeterRepository", "Error creating file from URI", e)
                }
            }

            // Optional: Light pre-compression to speed up upload
            if (sourceFile != null) {
                val processedFile = if (sourceFile.length() > 2 * 1024 * 1024) { // If > 2MB
                    Log.d("SelfMeterRepository", "File > 2MB, applying light compression for faster upload")
                    lightCompressForUpload(sourceFile)
                } else {
                    sourceFile
                }

                if (processedFile != null && processedFile.exists()) {
                    val requestFile = processedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    imagePart = MultipartBody.Part.createFormData(
                        "meter_image",
                        "meter.jpg",
                        requestFile
                    )
                    Log.d("SelfMeterRepository", "Prepared image for upload: ${processedFile.length()} bytes")
                    Log.d("SelfMeterRepository", "Backend will convert to 500x500 and ≤50KB")
                }
            }

            // Call API - backend will handle final compression to 500x500 and 50KB
            val response = apiService.submitSelfMeterReading(
                custCode = custCodePart,
                standMeter = standMeterPart,
                latitude = latitudePart,
                longitude = longitudePart,
                meter_image = imagePart
            )

            Log.d("SelfMeterRepository", "Response received: ${response.selfMeter?.image != null}")
            if (response.selfMeter?.image != null) {
                Log.d("SelfMeterRepository", "✅ Image successfully processed by backend: ${response.selfMeter.image}")
                Log.d("SelfMeterRepository", "Backend converted to: 500x500 pixels, ≤50KB")
            }
            Log.d("SelfMeterRepository", "=== END SUBMIT ===")

            Result.success(response)
        } catch (e: Exception) {
            Log.e("SelfMeterRepository", "Error submitting self meter reading", e)
            Result.failure(e)
        }
    }

    /**
     * Light compression untuk mempercepat upload
     * Backend akan melakukan final compression ke 500x500 dan 50KB
     */
    private fun lightCompressForUpload(sourceFile: File): File? {
        return try {
            val outputFile = File(context.cacheDir, "light_compressed_${System.currentTimeMillis()}.jpg")

            // Load dengan sample size untuk save memory
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(sourceFile.absolutePath, options)

            val originalWidth = options.outWidth
            val originalHeight = options.outHeight

            // Hitung sample size untuk load efficient
            val sampleSize = calculateSampleSize(originalWidth, originalHeight, 1200, 1200)

            val loadOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inJustDecodeBounds = false
            }

            val bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath, loadOptions)
            if (bitmap == null) {
                Log.w("SelfMeterRepository", "Failed to decode bitmap, using original file")
                return sourceFile
            }

            // Light compression - quality 80% untuk balance size vs quality
            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }
            bitmap.recycle()

            val originalSize = sourceFile.length()
            val compressedSize = outputFile.length()

            Log.d("SelfMeterRepository", "Light compression: ${originalSize} → ${compressedSize} bytes")

            // Return compressed file if successful and smaller
            if (outputFile.exists() && compressedSize < originalSize) {
                return outputFile
            } else {
                // Return original if compression didn't help
                outputFile.delete()
                return sourceFile
            }

        } catch (e: Exception) {
            Log.e("SelfMeterRepository", "Error in light compression, using original", e)
            return sourceFile
        }
    }

    private fun calculateSampleSize(origWidth: Int, origHeight: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1

        if (origHeight > reqHeight || origWidth > reqWidth) {
            val halfHeight = origHeight / 2
            val halfWidth = origWidth / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    private fun createFileFromUri(uri: Uri): File? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream?.use { input ->
                val tempFile = File.createTempFile("meter_image", ".jpg", context.cacheDir)
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
                tempFile
            }
        } catch (e: Exception) {
            Log.e("SelfMeterRepository", "Error creating file from URI", e)
            null
        }
    }
}