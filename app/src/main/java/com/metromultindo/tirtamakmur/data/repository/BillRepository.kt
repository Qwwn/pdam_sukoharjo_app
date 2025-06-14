package com.metromultindo.tirtamakmur.data.repository

import com.metromultindo.tirtamakmur.data.api.RetrofitInstance
import com.metromultindo.tirtamakmur.data.model.CustomerResponse
import javax.inject.Inject
import android.util.Log

class BillRepository @Inject constructor() {
    suspend fun getBillInfo(customerNumber: String): ApiResult<CustomerResponse> {
        return try {
            val response = RetrofitInstance.api.getBillInfo(customerNumber)

            Log.d("BillRepository", "Raw response: $response")

            if (response.error) {
                // Handle error response
                val errorMessage = response.message_text ?: "Pelanggan tidak ditemukan"
                Log.d("BillRepository", "API Error: ${response.message_code} - $errorMessage")
                ApiResult.Error(response.message_code, errorMessage)
            } else {
                // Handle success response
                if (response.message_code == 100) {
                    ApiResult.Success(response)
                } else {
                    ApiResult.Error(response.message_code,
                        response.message_text ?: "Terjadi kesalahan")
                }
            }
        } catch (e: Exception) {
            Log.e("BillRepository", "Network error: ${e.message}", e)
            ApiResult.Error(-1, "Periksa nomor ID Pelanggan, Pastikan nomor ID Pelanggan sudah benar")
        }
    }
}

