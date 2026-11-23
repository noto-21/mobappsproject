package com.group6.whereit

// ANDROID ACTIVITY AND NAVIGATION
import android.Manifest // access Android permissions
import android.content.Intent // opens another Activity
import android.os.Bundle // stores saved Activity state
import android.widget.Toast // displays temporary messages
import androidx.appcompat.app.AppCompatActivity // base class for MainActivity
import androidx.activity.compose.setContent // displays Compose UI
import androidx.activity.result.contract.ActivityResultContracts // handles permission results

// JETPACK COMPOSE - UI COMPONENTS
import androidx.compose.material3.Text // displays text
import androidx.compose.material3.Button // creates clickable buttons

// JETPACK COMPOSE - LAYOUT
import androidx.compose.foundation.layout.Column // arranges elements vertically
import androidx.compose.foundation.layout.Row // arranges elements horizontally
import androidx.compose.foundation.layout.Arrangement // controls positioning
import androidx.compose.foundation.layout.fillMaxWidth // fills available width
import androidx.compose.ui.Modifier // changes layout properties
import androidx.compose.foundation.layout.fillMaxSize // fills available screen space

// JETPACK COMPOSE - STATE
import androidx.compose.runtime.remember // preserves state across recompositions
import androidx.compose.runtime.mutableStateOf // creates observable state

// FIREBASE AUTHENTICATION
import com.google.firebase.auth.FirebaseAuth // manages Firebase user sessions

// ITEM LIBRARY AND DATABASE
import com.group6.whereit.data.SpotRepository // provides access to stored items
import com.group6.whereit.data.WhereItDatabase // local Room database
import com.group6.whereit.ui.items.ItemsListScreen // displays the item library
import com.group6.whereit.ui.theme.WhereItTheme // app's Compose theme

// LOCATION AND COROUTINES
import androidx.lifecycle.lifecycleScope // launches lifecycle-aware coroutines
import kotlinx.coroutines.launch // runs asynchronous location operations

// CAMERA
import com.group6.whereit.camera.CameraFlow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold

import androidx.compose.runtime.LaunchedEffect // runs asynchronous code inside Compose

import androidx.compose.foundation.layout.PaddingValues // controls spacing inside buttons
import androidx.compose.ui.unit.dp // measures spacing in density-independent pixels
import androidx.compose.ui.unit.sp // measures text size

class MainActivity : AppCompatActivity() {

    // location service used to retrieve device location
    private lateinit var locationService: LocationService

    // register a launcher to request location permissions
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions: Map<String, Boolean> ->

        // check whether fine or coarse location permission was granted
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {

            // permission granted, retrieve location
            fetchAndShowLocation()
        }
        else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // initialize the location service
        locationService = LocationService(this)

        // initialize the database and item repository
        val db = WhereItDatabase.getInstance(this)
        val repository = SpotRepository(db.spotItemDao())

        // display the Jetpack Compose interface
        setContent {

                // check whether Firebase already has a signed-in user
                val isLoggedIn = remember {
                    mutableStateOf(FirebaseAuth.getInstance().currentUser != null)
                }

                if (isLoggedIn.value) {

                    // get the unique Firebase ID of the signed-in user
                    val userId = FirebaseAuth.getInstance().currentUser?.uid

                    if (userId != null) {

                        // keeps track of which screen the user is on
                        val currentScreen = remember {
                            mutableStateOf("loading") // loading is the starting value while database is being checked
                        }

                        // starts a coroutine that runs when this section first appears and when userID is changed
                        // need this because db search is asynchronous
                        LaunchedEffect(userId) {

                            // check if the user has any items saved
                            val hasItems = repository.hasItems(userId)

                            // if they do, open to their item library
                            if (hasItems){

                                currentScreen.value = "library"
                            }

                            // if they don't, open straight to the camera
                            else{

                                currentScreen.value = "camera"
                            }
                        }


                        if (currentScreen.value == "camera") {

                            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                                CameraFlow(
                                    onItemSaved = { mediaPath, isVideo, description, dueDate ->
                                        android.util.Log.d(
                                            "WhereIt",
                                            "Saved: (video=$isVideo): $mediaPath | $description | $dueDate"
                                        )
                                    },
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                        }


                        // display the item library and navigation buttons
                        Column {

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {

                                // button to request location and test retrieval
                                Button(
                                    onClick = {
                                        requestPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    },
                                    contentPadding = PaddingValues(horizontal = 7.dp, vertical = 4.dp)

                                ) {
                                    Text("Test Location")
                                }

                                // button to open the map
                                Button(
                                    onClick = {

                                        currentScreen.value = "map"

                                        startActivity(
                                            Intent(
                                                this@MainActivity,
                                                MapActivity::class.java
                                            )
                                        )
                                    },
                                    contentPadding = PaddingValues(horizontal = 7.dp, vertical = 4.dp)

                                ) {
                                    Text("Open Map")
                                }

                                // button to request location and test retrieval
                                Button(
                                    onClick = {
                                        currentScreen.value = "library"
                                    },
                                    contentPadding = PaddingValues(horizontal = 7.dp, vertical = 4.dp)

                                ) {
                                    Text("Item Library")
                                }

                                // button to sign out
                                Button(
                                    onClick = {
                                        FirebaseAuth.getInstance().signOut()
                                        isLoggedIn.value = false
                                    },
                                    contentPadding = PaddingValues(horizontal = 7.dp, vertical = 4.dp)

                                ) {
                                    Text("Sign Out")
                                }


                            }

                            if (currentScreen.value == "library"){

                            // display items belonging to the signed-in user
                                ItemsListScreen(

                                    repository = repository,
                                    userId = userId
                                )

                          }
                        }
                    }
                }
                else {

                    // display login/signup screen when nobody is signed in
                    AuthScreen(
                        onAuthSuccess = {
                            // Firebase authentication succeeded
                            // update the state to display the item library
                            isLoggedIn.value = true
                        }
                    )
                }
            }

    }


    // retrieve the device's current location and display its coordinates
    private fun fetchAndShowLocation() {

        // launch coroutine without blocking the UI
        lifecycleScope.launch {

            val location = locationService.getCurrentLocation()

            if (location != null) {

                // display latitude and longitude
                Toast.makeText(
                    this@MainActivity,
                    "Lat: ${location.latitude}, Lng: ${location.longitude}",
                    Toast.LENGTH_LONG
                ).show()
            }
            else {

                // display message if location cannot be retrieved
                Toast.makeText(
                    this@MainActivity,
                    "Location is null. Check emulator settings.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
