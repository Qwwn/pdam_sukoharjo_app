package com.metromultindo.tirtamakmur.data.api

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject

class ResponseInterceptor : Interceptor {
    private val TAG = "ResponseInterceptor"

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        try {
            val response = chain.proceed(request)

            if (!response.isSuccessful) {
                try {
                    // Create a copy to avoid consuming the original body
                    val responseBodyCopy = response.peekBody(Long.MAX_VALUE)
                    val responseBodyString = responseBodyCopy.string()
                    Log.d(TAG, "Error response body: $responseBodyString")

                    if (!responseBodyString.isNullOrEmpty()) {
                        try {
                            val jsonObject = JSONObject(responseBodyString)
                            val messageCode = jsonObject.optInt("message_code", -1)

                            if (messageCode != -1) {
                                Log.d(TAG, "Found error code in response: $messageCode")

                                // Return a "successful" response with the original error body
                                return response.newBuilder()
                                    .code(200)
                                    .body(responseBodyCopy.source().buffer().clone().readByteString().toResponseBody(response.body?.contentType()))
                                    .build()
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing JSON", e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing response", e)
                }
            }

            return response
        } catch (e: Exception) {
            Log.e(TAG, "Network error", e)
            throw e
        }
    }
}