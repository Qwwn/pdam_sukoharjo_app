package com.metromultindo.tirtamakmur.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val provider: String
)

sealed class LocationResult {
    data class Success(val location: LocationData) : LocationResult()
    data class Error(val message: String) : LocationResult()
    object PermissionDenied : LocationResult()
    object LocationDisabled : LocationResult()
    object Timeout : LocationResult()
}

@Singleton
class LocationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "LocationHelper"
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    /**
     * Check if location permissions are granted
     */
    fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if location services are enabled
     */
    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    /**
     * Get current location with timeout
     */
    suspend fun getCurrentLocation(timeoutMs: Long = 10000): LocationResult {
        return withTimeoutOrNull(timeoutMs) {
            getCurrentLocationInternal()
        } ?: LocationResult.Timeout
    }

    private suspend fun getCurrentLocationInternal(): LocationResult {
        // Check permissions
        if (!hasLocationPermission()) {
            Log.w(TAG, "Location permission not granted")
            return LocationResult.PermissionDenied
        }

        // Check if location is enabled
        if (!isLocationEnabled()) {
            Log.w(TAG, "Location services disabled")
            return LocationResult.LocationDisabled
        }

        return try {
            // First try to get last known location (faster)
            val lastLocation = getLastKnownLocation()
            if (lastLocation != null) {
                Log.d(TAG, "Using last known location: ${lastLocation.latitude}, ${lastLocation.longitude}")
                return LocationResult.Success(lastLocation)
            }

            // If no last known location, get current location
            Log.d(TAG, "Getting current location...")
            getCurrentLocationFromGPS()
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception getting location", e)
            LocationResult.PermissionDenied
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location", e)
            LocationResult.Error(e.message ?: "Unknown error getting location")
        }
    }

    @SuppressWarnings("MissingPermission")
    private suspend fun getLastKnownLocation(): LocationData? {
        return suspendCancellableCoroutine { continuation ->
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        // Check if location is recent (within 5 minutes)
                        val currentTime = System.currentTimeMillis()
                        val locationTime = location.time
                        val fiveMinutesInMs = 5 * 60 * 1000

                        if (currentTime - locationTime <= fiveMinutesInMs) {
                            val locationData = LocationData(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                accuracy = location.accuracy,
                                provider = location.provider ?: "unknown"
                            )
                            continuation.resume(locationData)
                        } else {
                            Log.d(TAG, "Last known location is too old, getting fresh location")
                            continuation.resume(null)
                        }
                    } else {
                        continuation.resume(null)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to get last known location", exception)
                    continuation.resume(null)
                }
        }
    }

    @SuppressWarnings("MissingPermission")
    private suspend fun getCurrentLocationFromGPS(): LocationResult {
        return suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()

            continuation.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }

            val locationRequest = CurrentLocationRequest.Builder()
                .setDurationMillis(8000) // 8 seconds timeout
                .setMaxUpdateAgeMillis(2000) // Accept locations up to 2 seconds old
                .build()

            fusedLocationClient.getCurrentLocation(
                locationRequest,
                cancellationTokenSource.token
            )
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        val locationData = LocationData(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracy = location.accuracy,
                            provider = location.provider ?: "fused"
                        )
                        Log.d(TAG, "Got current location: ${locationData.latitude}, ${locationData.longitude}, accuracy: ${locationData.accuracy}m")
                        continuation.resume(LocationResult.Success(locationData))
                    } else {
                        Log.w(TAG, "Current location is null")
                        continuation.resume(LocationResult.Error("Unable to get current location"))
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to get current location", exception)
                    continuation.resume(LocationResult.Error(exception.message ?: "Failed to get location"))
                }
        }
    }

    /**
     * Format location for display
     */
    fun formatLocation(locationData: LocationData): String {
        return String.format(
            "%.6f, %.6f (±%.0fm)",
            locationData.latitude,
            locationData.longitude,
            locationData.accuracy
        )
    }

    /**
     * Check if coordinates are within Indonesia boundaries (rough check)
     */
    fun isLocationInIndonesia(latitude: Double, longitude: Double): Boolean {
        // Indonesia boundaries (approximate):
        // Latitude: -11.0 to 6.0
        // Longitude: 95.0 to 141.0
        return latitude >= -11.0 && latitude <= 6.0 &&
                longitude >= 95.0 && longitude <= 141.0
    }
}