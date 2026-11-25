package com.group6.whereit

// ANDROID ACTIVITY AND NAVIGATION
import android.Manifest // access Android permissions
import android.content.Intent // opens another Activity
import android.content.res.Configuration
import android.os.Bundle // stores saved Activity state
import android.widget.Toast // displays temporary messages
import androidx.appcompat.app.AppCompatActivity // base class for MainActivity
import androidx.activity.compose.setContent // displays Compose UI
import androidx.activity.result.contract.ActivityResultContracts // handles permission results
import androidx.appcompat.app.AppCompatDelegate
import android.app.UiModeManager // tells Android which mode the launch screen should use
import android.os.Build // checks the Android version

// JETPACK COMPOSE - UI COMPONENTS
import androidx.compose.material3.Text // displays text
import androidx.compose.material3.Button // creates clickable buttons

// JETPACK COMPOSE - LAYOUT
import androidx.compose.foundation.layout.Column // arranges elements vertically
import androidx.compose.foundation.layout.Row // arranges elements horizontally
import androidx.compose.foundation.layout.Arrangement // controls positioning
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth // fills available width
import androidx.compose.ui.Modifier // changes layout properties
import androidx.compose.foundation.layout.fillMaxSize // fills available screen space

// JETPACK COMPOSE - STATE
import androidx.compose.runtime.remember // preserves state across recompositions
import androidx.compose.runtime.mutableStateOf // creates observable state

// FIREBASE AUTHENTICATION
import com.google.firebase.auth.FirebaseAuth // manages Firebase user sessions

// ITEM LIBRARY AND DATABASE
import com.group6.whereit.data.SpotItem // one saved item (database row)
import com.group6.whereit.data.SpotRepository // provides access to stored items
import com.group6.whereit.data.WhereItDatabase // local Room database
import com.group6.whereit.ui.items.ItemsListScreen // displays the item library
import com.group6.whereit.ui.theme.WhereItTheme // app's Compose theme

// LOCATION AND COROUTINES
import androidx.lifecycle.lifecycleScope // launches lifecycle-aware coroutines
import kotlinx.coroutines.launch // runs asynchronous location operations

// CAMERA
import com.group6.whereit.camera.CameraFlow

import androidx.compose.runtime.LaunchedEffect // runs asynchronous code inside Compose

import androidx.compose.foundation.layout.PaddingValues // controls spacing inside buttons
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
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

        // load saved dark mode preference (defaults to device system setting)
        val prefs = getSharedPreferences("whereit_prefs", MODE_PRIVATE)
        val systemDark = (resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val initialDarkMode = prefs.getBoolean("dark_mode", systemDark)

        AppCompatDelegate.setDefaultNightMode(
            if (initialDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        // display the Jetpack Compose interface
        setContent {
            val isDarkMode = remember { mutableStateOf(initialDarkMode) }

            WhereItTheme(darkTheme = isDarkMode.value) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Root Box allows floating the Dark Mode toggle in the top-right corner
                    Box(modifier = Modifier.fillMaxSize()) {

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


                        // display the navigation buttons with the current screen underneath
                        // (one Column, so the camera no longer draws on top of the buttons)
                        Column(modifier = Modifier.fillMaxSize()) {

                            Row(
                                //edited to leave room for dark mode toggle button
                                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(end = 8.dp).padding(start = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {



                                // button to request location and test retrieval
                                Button(
                                    onClick = {
                                        currentScreen.value = "library"
                                    },
                                    contentPadding = PaddingValues(horizontal = 7.dp, vertical = 4.dp)

                                ) {
                                    Text("Item Library")
                                }


                                // Dark Mode toggle pinned to the top-right corner of the screen
                                FilledTonalButton(
                                    onClick = {
                                        val nextMode = !isDarkMode.value
                                        isDarkMode.value = nextMode
                                        prefs.edit().putBoolean("dark_mode", nextMode).apply()
                                        AppCompatDelegate.setDefaultNightMode(
                                            if (nextMode) AppCompatDelegate.MODE_NIGHT_YES
                                            else AppCompatDelegate.MODE_NIGHT_NO
                                        )
                                    },
                                    // same padding and top position as the Open Map / Item Library / Sign Out buttons
                                    contentPadding = PaddingValues(horizontal = 7.dp, vertical = 4.dp),
                                    modifier = Modifier

                                ) {
                                    Text(if (isDarkMode.value) "☀️ Light" else "🌙 Dark", maxLines = 1)
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

                            if (currentScreen.value == "camera") {

                                CameraFlow(
                                    onItemSaved = { mediaPath, isVideo, description, dueDate,
                                                    locationName, latitude, longitude ->

                                        // database calls are asynchronous, so run them in a coroutine
                                        lifecycleScope.launch {

                                            // save the new item to the Room database
                                            repository.save(
                                                SpotItem(
                                                    userId = userId,
                                                    photoPath = mediaPath,
                                                    description = description,
                                                    latitude = latitude,
                                                    longitude = longitude,
                                                    locationName = locationName,
                                                    dueDate = dueDate,
                                                    isVideo = isVideo
                                                )
                                            )

                                            // send the user to the item library to see the new item
                                            currentScreen.value = "library"
                                        }
                                    },
                                    // take up the rest of the screen below the buttons
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (currentScreen.value == "library"){

                            // display items belonging to the signed-in user
                                ItemsListScreen(

                                    repository = repository,
                                    userId = userId,
                                    // + button sends the user to the camera to make a new item
                                    onAddItem = { currentScreen.value = "camera" },
                                    // take up the rest of the screen below the buttons
                                    modifier = Modifier.weight(1f)
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
            }
        }
    }



    // When the app goes to the background, tell Android which mode the user picked, so the
    // launch screen on the next start matches (it is drawn before our code runs, so it
    // can't read the in-app toggle by itself). Only available on Android 12+.
    override fun onStop() {
        super.onStop()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val prefs = getSharedPreferences("whereit_prefs", MODE_PRIVATE)
            if (prefs.contains("dark_mode")) {
                val dark = prefs.getBoolean("dark_mode", false)
                getSystemService(UiModeManager::class.java)?.setApplicationNightMode(
                    if (dark) UiModeManager.MODE_NIGHT_YES else UiModeManager.MODE_NIGHT_NO
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
