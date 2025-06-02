package com.example.pdam_app_v2.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val dataStore = context.dataStore
    // Keys
    companion object {
        private val CUSTOMER_NUMBER = stringPreferencesKey("customer_number")
        private val CUSTOMER_NAME = stringPreferencesKey("customer_name")
        private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        const val DEFAULT_NEWS_CHECK_INTERVAL_MINUTES = 30L

    }
    // Getters
    val customerNumber: Flow<String?> = dataStore.data.map { preferences ->
        preferences[CUSTOMER_NUMBER]
    }

    val customerName: Flow<String?> = dataStore.data.map { preferences ->
        preferences[CUSTOMER_NAME]
    }

    // Match method name used in OnboardingViewModel
    val onboardingCompleted: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[ONBOARDING_COMPLETED] ?: false
    }

    // For backward compatibility
    val hasCompletedOnboarding: Flow<Boolean> = onboardingCompleted

    // Setters
    suspend fun saveCustomerNumber(number: String) {
        dataStore.edit { preferences ->
            preferences[CUSTOMER_NUMBER] = number
        }
    }

    suspend fun saveCustomerName(name: String) {
        dataStore.edit { preferences ->
            preferences[CUSTOMER_NAME] = name
        }
    }

    // Match method signature in OnboardingViewModel
    suspend fun saveOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = completed
        }
    }

    // For backward compatibility
    suspend fun completeOnboarding() {
        saveOnboardingCompleted(true)
    }

    // Clear user data
    suspend fun clearUserData() {
        dataStore.edit { preferences ->
            preferences.remove(CUSTOMER_NUMBER)
            preferences.remove(CUSTOMER_NAME)
        }
    }
}