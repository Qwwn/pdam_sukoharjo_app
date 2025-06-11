package com.metromultindo.tirtamakmur.data.repository

import com.metromultindo.tirtamakmur.data.api.RetrofitInstance
import com.metromultindo.tirtamakmur.data.model.CustomerResponse
import javax.inject.Inject
import android.util.Log

class BillRepository @Inject constructor() {
    // BillRepository.kt
    suspend fun getBillInfo(customerNumber: String): ApiResult<CustomerResponse> {
        return try {
            val response = RetrofitInstance.api.getBillInfo(customerNumber)

            Log.d(
                "BillRepository",
                "Response received: message_code=${response.message_code}, message_text=${response.message_text}"
            )

            // API returns error:true and message_code != 100 for errors
            if (response.error || response.message_code != 100) {
                Log.d("BillRepository", "Returning Error with code: ${response.message_code}")
                ApiResult.Error(response.message_code, response.message_text)
            } else {
                ApiResult.Success(response)
            }
        } catch (e: Exception) {
            Log.e("BillRepository", "Exception occurred: ${e.message}")
            ApiResult.Error(-1, e.message ?: "Unknown error occurred")
        }
    }
}