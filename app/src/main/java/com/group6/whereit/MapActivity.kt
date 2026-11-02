package com.group6.whereit

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapActivity : AppCompatActivity(), OnMapReadyCallback
{
    private lateinit var map: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync(this)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     */
    override fun onMapReady(googleMap: GoogleMap)
    {
        map = googleMap

        // Example: Add a marker at Ontario Tech University and move the camera
        val ontarioTech = LatLng(43.9454, -78.8967)
        map.addMarker(MarkerOptions().position(ontarioTech)
            .title("Marker in Ontario Tech"))

        // Zoom levels: 1 (World) to 20 (Buildings). 15 is street level.
        map.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                ontarioTech,
                15f
            )
        )
    }
}
