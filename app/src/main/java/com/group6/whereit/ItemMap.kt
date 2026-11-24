package com.group6.whereit

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.fragment.compose.AndroidFragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

@Composable
fun ItemMap(
    latitude: Double,
    longitude: Double,
    markerTitle: String,
    onLocationPicked: (lat: Double, lon: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidFragment<SupportMapFragment>(
        modifier = modifier,
        onUpdate = { mapFragment ->
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

                // Let the user tap the map to move the pin
                googleMap.setOnMapClickListener { clickedLatLng ->
                    onLocationPicked(clickedLatLng.latitude, clickedLatLng.longitude)
                }
            }
        }
    )
}
