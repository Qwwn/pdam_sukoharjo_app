package com.metromultindo.tirtapanrannuangku.utils

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUpdateManagerUtil @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(context)
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "AppUpdateManagerUtil"
        private const val PREFS_NAME = "app_update_prefs"
        private const val KEY_LAST_UPDATE_CHECK = "last_update_check"
        private const val UPDATE_CHECK_INTERVAL = 24 * 60 * 60 * 1000L // 24 hours
    }

    /**
     * Check if update is available with last update date
     */
    fun checkForUpdate(callback: (Boolean, String?) -> Unit) {
        Log.d(TAG, "Checking for app updates...")

        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                val isUpdateAvailable = appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                        appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)

                // Get last update date - format it nicely
                val lastUpdateDate = getFormattedUpdateDate()

                Log.d(TAG, "Update available: $isUpdateAvailable")
                Log.d(TAG, "Last update date: $lastUpdateDate")
                callback(isUpdateAvailable, lastUpdateDate)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to check for updates", exception)
                callback(false, null)
            }
    }

    /**
     * Start immediate update
     */
    fun startImmediateUpdate(activity: Activity, requestCode: Int, callback: (Boolean) -> Unit) {
        Log.d(TAG, "Starting immediate update...")

        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {

                    try {
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.IMMEDIATE,
                            activity,
                            requestCode
                        )
                        callback(true)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to start immediate update", e)
                        callback(false)
                    }
                } else {
                    Log.w(TAG, "Immediate update not available")
                    callback(false)
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to get app update info", exception)
                callback(false)
            }
    }

    /**
     * Check if should check for updates based on time interval
     */
    fun shouldCheckForUpdates(): Boolean {
        val lastCheck = sharedPreferences.getLong(KEY_LAST_UPDATE_CHECK, 0)
        val currentTime = System.currentTimeMillis()
        val shouldCheck = (currentTime - lastCheck) > UPDATE_CHECK_INTERVAL

        if (shouldCheck) {
            sharedPreferences.edit()
                .putLong(KEY_LAST_UPDATE_CHECK, currentTime)
                .apply()
        }

        Log.d(TAG, "Should check for updates: $shouldCheck")
        return shouldCheck
    }

    /**
     * Force update check (ignore time interval)
     */
    fun forceUpdateCheck() {
        sharedPreferences.edit()
            .putLong(KEY_LAST_UPDATE_CHECK, 0)
            .apply()
        Log.d(TAG, "Forced update check")
    }

    /**
     * Check for interrupted update on resume
     */
    fun checkForInterruptedUpdate(callback: (Boolean) -> Unit) {
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                val isInterrupted = appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                Log.d(TAG, "Interrupted update detected: $isInterrupted")
                callback(isInterrupted)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to check for interrupted update", exception)
                callback(false)
            }
    }

    /**
     * Get formatted update date from package info
     */
    private fun getFormattedUpdateDate(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val lastUpdateTime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.lastUpdateTime
            } else {
                @Suppress("DEPRECATION")
                packageInfo.lastUpdateTime
            }

            val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale("id", "ID"))
            dateFormat.format(Date(lastUpdateTime))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get last update time", e)
            val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale("id", "ID"))
            dateFormat.format(Date())
        }
    }
}