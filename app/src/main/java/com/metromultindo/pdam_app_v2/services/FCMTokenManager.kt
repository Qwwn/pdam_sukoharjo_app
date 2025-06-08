package com.metromultindo.pdam_app_v2.services

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.metromultindo.pdam_app_v2.data.api.ApiService
import com.metromultindo.pdam_app_v2.utils.dataStore
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FCMTokenManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: ApiService
) {
    companion object {
        private const val TAG = "FCMTokenManager"
        private val FCM_TOKEN_KEY = stringPreferencesKey("fcm_token")
        private val DEVICE_ID_KEY = stringPreferencesKey("device_id")
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    // Get current FCM token
    suspend fun getCurrentToken(): String? {
        return try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting FCM token", e)
            null
        }
    }

    // Save token to local storage
    fun saveToken(token: String) {
        scope.launch {
            try {
                context.dataStore.edit { preferences ->
                    preferences[FCM_TOKEN_KEY] = token
                }
                Log.d(TAG, "Token saved to local storage")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving token to local storage", e)
            }
        }
    }

    // Get saved token from local storage
    fun getSavedToken(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[FCM_TOKEN_KEY]
        }
    }

    // Send token to server
    fun sendTokenToServer(token: String) {
        scope.launch {
            try {
                val deviceId = getOrCreateDeviceId()

                // Call API to register/update token
                val response = apiService.registerFCMToken(
                    token = token.toRequestBody("text/plain".toMediaTypeOrNull()),
                    deviceId = deviceId.toRequestBody("text/plain".toMediaTypeOrNull()),
                    deviceType = "android".toRequestBody("text/plain".toMediaTypeOrNull())
                )

                if (response.error == false) {
                    Log.d(TAG, "Token successfully sent to server")
                } else {
                    Log.e(TAG, "Failed to send token to server: ${response.messageText}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending token to server", e)
            }
        }
    }

    // Initialize FCM and register token
    suspend fun initializeFCM() {
        try {
            val token = getCurrentToken()
            if (token != null) {
                saveToken(token)
                sendTokenToServer(token)
                Log.d(TAG, "FCM initialized successfully with token: $token")
            } else {
                Log.e(TAG, "Failed to get FCM token")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing FCM", e)
        }
    }

    // Subscribe to news topic
    suspend fun subscribeToNewsTopic() {
        try {
            FirebaseMessaging.getInstance().subscribeToTopic("news").await()
            Log.d(TAG, "Subscribed to news topic")
        } catch (e: Exception) {
            Log.e(TAG, "Error subscribing to news topic", e)
        }
    }

    // Unsubscribe from news topic
    suspend fun unsubscribeFromNewsTopic() {
        try {
            FirebaseMessaging.getInstance().unsubscribeFromTopic("news").await()
            Log.d(TAG, "Unsubscribed from news topic")
        } catch (e: Exception) {
            Log.e(TAG, "Error unsubscribing from news topic", e)
        }
    }

    // Get or create device ID
    private suspend fun getOrCreateDeviceId(): String {
        return try {
            val savedDeviceId = context.dataStore.data.first()[DEVICE_ID_KEY]
            if (savedDeviceId != null) {
                savedDeviceId
            } else {
                val newDeviceId = generateDeviceId()
                context.dataStore.edit { preferences ->
                    preferences[DEVICE_ID_KEY] = newDeviceId
                }
                newDeviceId
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting device ID", e)
            generateDeviceId()
        }
    }

    // Generate unique device ID
    private fun generateDeviceId(): String {
        return "android_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }

    // Delete token from server (for logout/uninstall)
    fun deleteTokenFromServer() {
        scope.launch {
            try {
                val savedToken = getSavedToken().first()
                if (savedToken != null) {
                    val deviceId = getOrCreateDeviceId()

                    val response = apiService.deleteFCMToken(
                        token = savedToken.toRequestBody("text/plain".toMediaTypeOrNull()),
                        deviceId = deviceId.toRequestBody("text/plain".toMediaTypeOrNull())
                    )

                    if (response.error == false) {
                        Log.d(TAG, "Token successfully deleted from server")
                        // Clear local token
                        context.dataStore.edit { preferences ->
                            preferences.remove(FCM_TOKEN_KEY)
                        }
                    } else {
                        Log.e(TAG, "Failed to delete token from server: ${response.messageText}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting token from server", e)
            }
        }
    }
}