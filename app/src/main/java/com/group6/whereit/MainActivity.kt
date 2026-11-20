package com.group6.whereit

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.google.firebase.auth.FirebaseAuth
import com.group6.whereit.data.SpotRepository
import com.group6.whereit.data.WhereItDatabase
import com.group6.whereit.ui.items.ItemsListScreen
import com.group6.whereit.ui.theme.WhereItTheme
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity()
{ 
    private lateinit var locationService: LocationService

    // Register the permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    )
    { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
            fetchAndShowLocation()
        else
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT)
                .show()
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        enableEdgeToEdge()

        val db = WhereItDatabase.getInstance(this)
        val repository = SpotRepository(db.spotItemDao())

        // Real Firebase user once Essa's login is merged; test ID until then.
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "test-user"

        setContent {
            WhereItTheme {
                ItemsListScreen(repository = repository, userId = userId)
            }
        }

        locationService = LocationService(this)

        findViewById<Button>(R.id.btnTestLocation).setOnClickListener {
            // Launch the permission request dialog
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

        findViewById<Button>(R.id.btnOpenMap).setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }
    }

    private fun fetchAndShowLocation()
    {
        lifecycleScope.launch {
            val location = locationService.getCurrentLocation()
            if (location != null)
            {
                Toast.makeText(
                    this@MainActivity,
                    "Lat: ${location.latitude}, Lng: ${location.longitude}",
                    Toast.LENGTH_LONG
                ).show()
            }
            else
                Toast.makeText(
                    this@MainActivity,
                    "Location is null. Check emulator settings.",
                    Toast.LENGTH_LONG
                ).show()
        }
    }
}
