package com.group6.whereit

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.fragment.compose.AndroidFragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
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
                        if (event.action == MotionEvent.ACTION_DOWN) {
                            v.parent?.requestDisallowInterceptTouchEvent(true)
                        }
                        false // Pass the touch event through to the map
                    }
                }
                container.addView(touchInterceptor)
            }

            mapFragment.getMapAsync { googleMap ->
                val position = LatLng(latitude, longitude)
                googleMap.clear()
                googleMap.addMarker(
                    MarkerOptions()
                        .position(position)
                        .title(markerTitle)
                )
                googleMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(position, 15f)
                )

                // Tapping a named Google Maps place/building gives its exact POI name
                googleMap.setOnPoiClickListener { poi ->
                    val cleanName = poi.name.replace("\n", ", ").trim()
                    onLocationPicked(poi.latLng.latitude, poi.latLng.longitude, cleanName)
                }

                // Tapping anywhere else on the map resolves via geocoding
                googleMap.setOnMapClickListener { clickedLatLng ->
                    onLocationPicked(clickedLatLng.latitude, clickedLatLng.longitude, null)
                }
            }
        }
    )
}
