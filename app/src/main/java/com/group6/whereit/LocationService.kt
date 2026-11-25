package com.group6.whereit

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.coroutines.resume

class LocationService(private val context: Context)
{
    private val fusedLocationClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)

    private var currentTimestamp: Long = 0L

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location?
    {
        return try
        {
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

    /**
     * Resolves (latitude, longitude) to the nearest human-readable place or street address.
     * Only falls back to formatted "lat, lon" coordinates if all geocoding services fail.
     */
    suspend fun resolveLocationName(latitude: Double, longitude: Double): String
    {
        val location = Location("").apply {
            this.latitude = latitude
            this.longitude = longitude
        }
        return getAddress(location)
                ?: String.format(Locale.US, "%.5f, %.5f", latitude, longitude)
    }

    suspend fun getAddress(location: Location): String?
    {
        val address = getAddressFromGeocoder(location) ?: getAddressFromOpenStreetMap(location)
        Log.d("WhereIt", "Address for ${location.latitude}, ${location.longitude}: $address")
        return address
    }

    /** Formats an Android Geocoder Address, preferring named landmarks/buildings + street address. */
    private fun formatGeocoderAddress(address: Address): String?
    {
        val fullLine = address.getAddressLine(0)?.trim()?.takeIf { it.isNotBlank() }
        val feature = address.featureName?.trim()?.takeIf { it.isNotBlank() }

        // Ignore results where Geocoder just echoed raw coordinates back
        if (fullLine != null && parseLatLng(fullLine) != null) return null

        // If featureName is a distinct place name (not just a street number or already at the start of fullLine),
        // prepend it so the user sees e.g. "Science Building, 2000 Simcoe St N, Oshawa, ON"
        val isStreetNumber = feature != null && feature.all { it.isDigit() || it == '-' }
        if (feature != null && !isStreetNumber && fullLine != null &&
            !fullLine.startsWith(feature, ignoreCase = true)
        )
        {
            return "$feature, $fullLine"
        }

        return fullLine ?: feature
    }

    suspend fun getCoordinates(query: String): Pair<Double, Double>?
    {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return null

        parseLatLng(trimmed)?.let { return it }

        val coords = getCoordinatesFromGeocoder(trimmed) ?: getCoordinatesFromOpenStreetMap(trimmed)
        Log.d("WhereIt", "Coordinates for '$trimmed': $coords")
        return coords
    }

    private fun parseLatLng(input: String): Pair<Double, Double>?
    {
        val match = Regex("""^(-?\d+(?:\.\d+)?)\s*,\s*(-?\d+(?:\.\d+)?)$""").matchEntire(input)
                ?: Regex("""^(-?\d+\.\d+)\s+(-?\d+\.\d+)$""").matchEntire(input)
                ?: return null

        val lat = match.groupValues[1].toDoubleOrNull() ?: return null
        val lon = match.groupValues[2].toDoubleOrNull() ?: return null

        return if (lat in -90.0..90.0 && lon in -180.0..180.0) lat to lon else null
    }

    private suspend fun getCoordinatesFromOpenStreetMap(query: String): Pair<Double, Double>? =
            withContext(Dispatchers.IO)
            {
                try
                {
                    val encoded = URLEncoder.encode(query, "UTF-8")
                    val url = URL("https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q=$encoded")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.setRequestProperty("User-Agent", "WhereIt-StudentApp/1.0 (Ontario Tech SOFE 4640)")
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000

                    try
                    {
                        if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null

                        val body = connection.inputStream.bufferedReader().use { it.readText() }
                        val results = JSONArray(body)
                        if (results.length() == 0) return@withContext null

                        val first = results.getJSONObject(0)
                        val lat = first.optString("lat").toDoubleOrNull()
                        val lon = first.optString("lon").toDoubleOrNull()
                        if (lat != null && lon != null) lat to lon else null
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

    private suspend fun getCoordinatesFromGeocoder(query: String): Pair<Double, Double>? =
            withContext(Dispatchers.IO)
            {
                if (!Geocoder.isPresent()) return@withContext null

                val geocoder = Geocoder(this@LocationService.context, Locale.getDefault())

                try
                {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    {
                        suspendCancellableCoroutine { continuation ->
                            geocoder.getFromLocationName(
                                query,
                                1,
                                object : Geocoder.GeocodeListener
                                {
                                    override fun onGeocode(addresses: MutableList<Address>) {
                                        if (continuation.isActive) {
                                            val first = addresses.firstOrNull()
                                            continuation.resume(
                                                if (first != null) first.latitude to first.longitude else null
                                            )
                                        }
                                    }

                                    override fun onError(errorMessage: String?) {
                                        if (continuation.isActive) continuation.resume(null)
                                    }
                                }
                            )
                        }
                    }
                    else
                    {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocationName(query, 1)
                            ?.firstOrNull()
                            ?.let { it.latitude to it.longitude }
                    }
                }
                catch (e: Exception)
                {
                    e.printStackTrace()
                    null
                }
            }

    private suspend fun getAddressFromOpenStreetMap(location: Location): String? =
            withContext(Dispatchers.IO)
            {
                try
                {
                    val url = URL(
                        "https://nominatim.openstreetmap.org/reverse?format=jsonv2&zoom=18" +
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
                        val json = JSONObject(body)
                        val placeName = json.optString("name").trim().ifBlank { null }
                        val displayName = json.optString("display_name").trim().ifBlank { null }

                        when {
                            placeName != null && displayName != null &&
                                    !displayName.startsWith(placeName, ignoreCase = true) -> "$placeName, $displayName"
                            displayName != null -> displayName
                            else -> placeName
                        }
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

    private suspend fun getAddressFromGeocoder(location: Location): String? =
            withContext(Dispatchers.IO)
            {
                if (!Geocoder.isPresent()) return@withContext null

                val geocoder = Geocoder(this@LocationService.context, Locale.getDefault())

                try
                {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    {
                        suspendCancellableCoroutine { continuation ->
                            geocoder.getFromLocation(
                                location.latitude,
                                location.longitude,
                                1,
                                object : Geocoder.GeocodeListener
                                {
                                    override fun onGeocode(addresses: MutableList<Address>) {
                                        if (continuation.isActive) {
                                            val best = addresses.firstOrNull()?.let { formatGeocoderAddress(it) }
                                            continuation.resume(best)
                                        }
                                    }

                                    override fun onError(errorMessage: String?) {
                                        if (continuation.isActive) continuation.resume(null)
                                    }
                                }
                            )
                        }
                    }
                    else
                    {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocation(location.latitude, location.longitude, 1)
                            ?.firstOrNull()
                            ?.let { formatGeocoderAddress(it) }
                    }
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
