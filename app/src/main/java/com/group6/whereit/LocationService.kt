package com.group6.whereit

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * A service class responsible for fetching the device's current geographical location
 * and managing an associated timestamp.
 *
 * @param context The application or activity context needed to initialize the location provider.
 */
class LocationService(private val context: Context)
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
     * Turns a location's coordinates into a readable address (reverse geocoding),
     * e.g. "2000 Simcoe St N, Oshawa, ON L1G 0C5, Canada".
     *
     * Tries Android's built-in Geocoder first. Emulators often can't use it, so if it gives
     * nothing back, asks OpenStreetMap (Nominatim) for the address over the internet instead.
     *
     * @return The address, or null if neither lookup found one (no internet, etc.).
     */
    suspend fun getAddress(location: Location): String?
    {
        val address = getAddressFromGeocoder(location) ?: getAddressFromOpenStreetMap(location)
        Log.d("WhereIt", "Address for ${location.latitude}, ${location.longitude}: $address")
        return address
    }

    /**
     * Backup address lookup using OpenStreetMap's free Nominatim service.
     * (Their rules: send a User-Agent naming the app, and at most 1 request per second.)
     */
    private suspend fun getAddressFromOpenStreetMap(location: Location): String? = withContext(Dispatchers.IO)
    {
        try
        {
            val url = URL(
                "https://nominatim.openstreetmap.org/reverse?format=jsonv2" +
                        "&lat=${location.latitude}&lon=${location.longitude}"
            )
            val connection = url.openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "WhereIt-StudentApp/1.0 (Ontario Tech SOFE 4640)")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            try
            {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null

                val body = connection.inputStream.bufferedReader().use { it.readText() }
                JSONObject(body).optString("display_name").ifBlank { null }
            }
            finally
            {
                connection.disconnect()
            }
        }
        catch (e: Exception)
        {
            e.printStackTrace()
            null
        }
    }

    /** Address lookup using Android's built-in Geocoder. */
    private suspend fun getAddressFromGeocoder(location: Location): String? = withContext(Dispatchers.IO)
    {
        if (!Geocoder.isPresent()) return@withContext null

        val geocoder = Geocoder(context, Locale.getDefault())

        try
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            {
                // Android 13+: the geocoder answers through a listener
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(
                        location.latitude,
                        location.longitude,
                        1,
                        object : Geocoder.GeocodeListener
                        {
                            override fun onGeocode(addresses: MutableList<Address>)
                            {
                                continuation.resume(addresses.firstOrNull()?.getAddressLine(0))
                            }

                            override fun onError(errorMessage: String?)
                            {
                                continuation.resume(null)
                            }
                        }
                    )
                }
            }
            else
            {
                // Older Android: blocking call, which is why this runs on Dispatchers.IO
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    ?.firstOrNull()
                    ?.getAddressLine(0)
            }
        }
        catch (e: Exception)
        {
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
