package com.group6.whereit

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await

class LocationService(context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var currentTimestamp: Long = 0L

    /**
     * Retrieves the current location using the device's GPS sensor.
     * Note: Ensure location permissions are granted by the user before calling this.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        return try {
            // Fetches the most recent location available
            fusedLocationClient.lastLocation.await()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Sets a timestamp for the location tracking event.
     */
    fun setTimestamp(timestamp: Long = System.currentTimeMillis()) {
        this.currentTimestamp = timestamp
    }

    fun getTimestamp(): Long {
        return currentTimestamp
    }
}
