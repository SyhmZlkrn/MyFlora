package com.example.flora.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationProvider(private val context: Context) {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    // Default: Kuala Lumpur
    private val defaultLat = 3.1390
    private val defaultLon = 101.6869

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    suspend fun getLastLocation(): Location {
        if (!hasLocationPermission()) {
            return Location("default").apply {
                latitude = defaultLat
                longitude = defaultLon
            }
        }

        return suspendCancellableCoroutine { cont ->
            val cancellationToken = CancellationTokenSource()
            fusedClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cancellationToken.token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    cont.resume(location)
                } else {
                    // Fall back to last known location
                    fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
                        cont.resume(lastLoc ?: Location("default").apply {
                            latitude = defaultLat
                            longitude = defaultLon
                        })
                    }.addOnFailureListener {
                        cont.resume(Location("default").apply {
                            latitude = defaultLat
                            longitude = defaultLon
                        })
                    }
                }
            }.addOnFailureListener {
                cont.resume(Location("default").apply {
                    latitude = defaultLat
                    longitude = defaultLon
                })
            }

            cont.invokeOnCancellation { cancellationToken.cancel() }
        }
    }
}
