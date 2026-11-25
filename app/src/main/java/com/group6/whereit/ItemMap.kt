package com.group6.whereit

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.compose.AndroidFragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

@SuppressLint("ClickableViewAccessibility")
@Composable
fun ItemMap(
    latitude: Double,
    longitude: Double,
    markerTitle: String,
    onLocationPicked: (lat: Double, lon: Double, placeName: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    var marker by remember { mutableStateOf<Marker?>(null) }
    var hasInitializedCamera by remember { mutableStateOf(false) }

    val latestOnLocationPicked by rememberUpdatedState(onLocationPicked)

    AndroidFragment<SupportMapFragment>(
        modifier = modifier,
        onUpdate = { mapFragment ->
            val container = mapFragment.view as? ViewGroup
            if (container != null && container.findViewWithTag<View>("map_touch_interceptor") == null) {
                val touchInterceptor = View(container.context).apply {
                    tag = "map_touch_interceptor"
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setOnTouchListener { v, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN ->
                                v.parent?.requestDisallowInterceptTouchEvent(true)
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                                v.parent?.requestDisallowInterceptTouchEvent(false)
                        }
                        false // Pass the touch event through to the map
                    }
                }
                container.addView(touchInterceptor)
            }

            mapFragment.getMapAsync { map ->
                if (googleMap !== map) {
                    marker = null
                    hasInitializedCamera = false
                    googleMap = map
                }

                // Tapping a named Google Maps place/building gives its exact POI name
                map.setOnPoiClickListener { poi ->
                    val cleanName = poi.name.replace("\n", ", ").trim()
                    latestOnLocationPicked(poi.latLng.latitude, poi.latLng.longitude, cleanName)
                }

                // Tapping anywhere else on the map resolves via geocoding
                map.setOnMapClickListener { clickedLatLng ->
                    latestOnLocationPicked(clickedLatLng.latitude, clickedLatLng.longitude, null)
                }
            }
        }
    )

    // Move the waypoint marker and camera whenever coordinates change
    LaunchedEffect(googleMap, latitude, longitude) {
        val map = googleMap ?: return@LaunchedEffect
        val position = LatLng(latitude, longitude)

        val currentMarker = marker
        if (currentMarker == null) {
            marker = map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(markerTitle)
            )
        } else {
            currentMarker.position = position
        }

        if (!hasInitializedCamera) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 15f))
            hasInitializedCamera = true
        } else {
            val targetZoom = maxOf(map.cameraPosition.zoom, 15f)
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(position, targetZoom))
        }
    }

    // Update marker title when reverse geocoding completes without moving the camera
    LaunchedEffect(marker, markerTitle) {
        marker?.title = markerTitle
    }
}
