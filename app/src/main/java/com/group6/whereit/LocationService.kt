package com.group6.whereit

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

/**
 * A service class responsible for fetching the device's current geographical location
 * and managing an associated timestamp.
 *
 * @param context The application or activity context needed to initialize the location provider.
 */
class LocationService(context: Context)
{
    // The main entry point for interacting with the fused location provider.
    // FusedLocationProviderClient intelligently manages underlying location technologies (GPS, Wi-Fi, cell).
    private val fusedLocationClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)

    // Stores a timestamp associated with the location fetch (defaults to 0L).
    private var currentTimestamp: Long = 0L

    /**
     * Asynchronously fetches the device's current location with high accuracy.
     *
     * Note: This function suppresses the "MissingPermission" warning. It assumes that
     * the caller has already requested and been granted the necessary location permissions
     * (ACCESS_COARSE_LOCATION / ACCESS_FINE_LOCATION) before invoking this method.
     *
     * @return The current [Location] object, or null if the location could not be determined or an error occurred.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location?
    {
        return try
        {
            // Actively fetches a fresh location update rather than relying on a cached location.
            // PRIORITY_HIGH_ACCURACY requests the most precise location available (usually GPS).
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token // Allows the location request to be cancelled if needed
            ).await() // Suspends the coroutine until the Google Play Services task completes
        }
        catch (e: Exception)
        {
            // Log the exception for debugging purposes if the location fetch fails
            e.printStackTrace()
            null
        }
    }

    /**
     * Updates the stored timestamp.
     *
     * @param timestamp The time in milliseconds. Defaults to the current system time if not provided.
     */
    fun setTimestamp(timestamp: Long = System.currentTimeMillis())
    {
        this.currentTimestamp = timestamp
    }

    /**
     * Retrieves the stored timestamp.
     *
     * @return The current timestamp in milliseconds.
     */
    fun getTimestamp(): Long
    {
        return currentTimestamp
    }
}
