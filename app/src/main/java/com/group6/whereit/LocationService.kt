package com.group6.whereit

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

class LocationService(context: Context)
{
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var currentTimestamp: Long = 0L

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location?
    {
        return try
        {
            // Actively fetches a fresh location update
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).await()
        }
        catch (e: Exception)
        {
            e.printStackTrace()

            null
        }
    }

    fun setTimestamp(timestamp: Long = System.currentTimeMillis())
    {
        this.currentTimestamp = timestamp
    }

    fun getTimestamp(): Long
    {
        return currentTimestamp
    }
}
