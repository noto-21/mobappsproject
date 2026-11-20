
package com.group6.whereit

// ANDROID ACTIVITY AND NAVIGATION
import android.Manifest // access Android permissions
import android.content.Intent // used to open another Activity
import android.os.Bundle // stores saved Activity state
import android.widget.Toast // displays temporary messages
import androidx.appcompat.app.AppCompatActivity // base class for MainActivity
import androidx.activity.compose.setContent // displays Compose UI inside Activity
import androidx.activity.result.contract.ActivityResultContracts // handles permission request results

// JETPACK COMPOSE - UI COMPONENTS
import androidx.compose.material3.Text // displays text on screen
import androidx.compose.material3.Button // creates clickable buttons

// JETPACK COMPOSE - LAYOUT AND POSITIONING
import androidx.compose.foundation.layout.Column // arranges UI elements vertically
import androidx.compose.foundation.layout.Arrangement // controls vertical positioning
import androidx.compose.foundation.layout.fillMaxSize // fills available screen space
import androidx.compose.ui.Alignment // controls horizontal alignment
import androidx.compose.ui.Modifier // changes UI element size, spacing, etc.

// JETPACK COMPOSE - STATE MANAGEMENT
import androidx.compose.runtime.remember // preserves values across recompositions
import androidx.compose.runtime.mutableStateOf // creates observable state that triggers UI updates

// FIREBASE AUTHENTICATION
import com.google.firebase.auth.FirebaseAuth // checks signed-in users and handles sign out

// LOCATION - ASYNCHRONOUS OPERATIONS
import androidx.lifecycle.lifecycleScope // launches coroutines tied to the Activity lifecycle
import kotlinx.coroutines.launch // starts coroutine for location retrieval


class MainActivity : AppCompatActivity() {

    // location service used to retrieve device location
    private lateinit var locationService: LocationService

    // register a launcher to request location permissions and receive the result
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
            // permission denied
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // initialize the location service
        locationService = LocationService(this)

        // display the Jetpack Compose interface
        setContent {

            // check whether Firebase already has a signed-in user
            val isLoggedIn = remember {

                // currentUser returns the signed-in user, or null if nobody is signed in
                // != null returns true when a signed-in user exists
                // mutableStateOf creates observable state
                // remember preserves this state across recompositions
                // users with an existing session can skip the login screen
                mutableStateOf(FirebaseAuth.getInstance().currentUser != null)
            }

            if (isLoggedIn.value) {

                // temporary home screen until final navigation is implemented
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text("Welcome to WhereIt!")

                    // button to request location permissions and test location retrieval
                    Button(
                        onClick = {
                            requestPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    ) {
                        Text("Test Location")
                    }

                    // button to open the map screen
                    Button(
                        onClick = {
                            startActivity(
                                Intent(this@MainActivity, MapActivity::class.java)
                            )
                        }
                    ) {
                        Text("Open Map")
                    }

                    // button to sign out of Firebase
                    Button(
                        onClick = {
                            // sign out the currently authenticated Firebase user
                            FirebaseAuth.getInstance().signOut()

                            // update UI state to display the login screen
                            isLoggedIn.value = false
                        }
                    ) {
                        Text("Sign Out")
                    }
                }
            }
            else {

                // display the authentication screen when no user is signed in
                AuthScreen(
                    onAuthSuccess = {
                        // called after successful Firebase login or signup
                        // update state to display the temporary home screen
                        isLoggedIn.value = true
                    }
                )
            }
        }
    }


    // retrieve the device's current location and display its coordinates
    private fun fetchAndShowLocation() {

        // launch coroutine to retrieve location without blocking the UI
        lifecycleScope.launch {

            val location = locationService.getCurrentLocation()

            if (location != null) {

                // display the retrieved latitude and longitude
                Toast.makeText(
                    this@MainActivity,
                    "Lat: ${location.latitude}, Lng: ${location.longitude}",
                    Toast.LENGTH_LONG
                ).show()
            }
            else {

                // display message if location could not be retrieved
                Toast.makeText(
                    this@MainActivity,
                    "Location is null. Check emulator settings.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
