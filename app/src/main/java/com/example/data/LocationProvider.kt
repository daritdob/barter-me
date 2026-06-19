package com.example.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import java.util.Locale

data class GpsLocation(
    val latitude: Double,
    val longitude: Double,
    val locationName: String
)

sealed class LocationResult {
    data class Success(val location: GpsLocation) : LocationResult()
    data class Error(val message: String) : LocationResult()
}

class LocationProvider(private val context: Context) {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    suspend fun getCurrentLocation(): LocationResult {
        if (!hasLocationPermission()) {
            return LocationResult.Error("Location permission not granted")
        }
        return try {
            val cancellation = CancellationTokenSource()
            val androidLocation = fusedClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cancellation.token
            ).await() ?: fusedClient.lastLocation.await()

            if (androidLocation == null) {
                return LocationResult.Error("Could not determine your location. Try again outdoors.")
            }

            val name = reverseGeocode(androidLocation.latitude, androidLocation.longitude)
            LocationResult.Success(
                GpsLocation(
                    latitude = androidLocation.latitude,
                    longitude = androidLocation.longitude,
                    locationName = name
                )
            )
        } catch (e: SecurityException) {
            LocationResult.Error("Location permission denied")
        } catch (e: Exception) {
            LocationResult.Error(e.message ?: "Location lookup failed")
        }
    }

    private fun reverseGeocode(latitude: Double, longitude: Double): String {
        if (!Geocoder.isPresent()) {
            return formatCoordinates(latitude, longitude)
        }
        return try {
            @Suppress("DEPRECATION")
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val address = geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
            address?.let {
                listOfNotNull(it.locality, it.adminArea).joinToString(", ").ifBlank { null }
            } ?: formatCoordinates(latitude, longitude)
        } catch (_: Exception) {
            formatCoordinates(latitude, longitude)
        }
    }

    private fun formatCoordinates(latitude: Double, longitude: Double): String =
        String.format(Locale.US, "%.4f, %.4f", latitude, longitude)
}
