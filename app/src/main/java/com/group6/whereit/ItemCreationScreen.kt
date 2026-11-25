package com.group6.whereit

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.speech.RecognizerIntent
import android.text.format.DateFormat
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import com.group6.whereit.data.DueDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemCreationScreen(
    mediaPath: String,
    isVideo: Boolean,
    onSave: (
        description: String,
        dueDate: Long?,
        locationName: String?,
        latitude: Double?,
        longitude: Double?
    ) -> Unit,
    onRetake: () -> Unit,
    modifier: Modifier = Modifier,
    initialDescription: String = "",
    initialDueDate: Long? = null,
    initialLocation: String? = null,
    initialLatitude: Double? = null,
    initialLongitude: Double? = null,
    isEditing: Boolean = false
) {
    val context = LocalContext.current

    val scope = rememberCoroutineScope()
    val locationService = remember { LocationService(context) }

    var description by rememberSaveable(initialDescription) {
        mutableStateOf(initialDescription)
    }
    var dueDate by rememberSaveable(initialDueDate) {
        mutableStateOf(initialDueDate)
    }
    var latitude by rememberSaveable(initialLatitude) {
        mutableStateOf(initialLatitude)
    }
    var longitude by rememberSaveable(initialLongitude) {
        mutableStateOf(initialLongitude)
    }

    var showDatePicker by remember { mutableStateOf(false) }
    // date chosen in the date picker, waiting for the optional time step
    var pendingDate by remember { mutableStateOf<Long?>(null) }

    // LOCATION: readable address/place name shown in the text field + the coordinates behind it
    var locationText by rememberSaveable(initialLocation) {
        mutableStateOf(initialLocation ?: "")
    }
    var isLocating by remember { mutableStateOf(false) }
    var isGeocoding by remember { mutableStateOf(false) }
    var reverseGeocodeJob by remember { mutableStateOf<Job?>(null) }

    // true once the user types their own location (or the item already has a saved one),
    // so the automatic current-location lookup never overwrites it
    var userEnteredLocation by rememberSaveable(initialLocation) {
        mutableStateOf(!initialLocation.isNullOrBlank())
    }

    // remembers whether the lookup waiting on the permission popup came from the button
    var pendingReplace by remember { mutableStateOf(false) }

    // true only when the user manually types in the location field
    var shouldGeocodeText by remember { mutableStateOf(false) }

    suspend fun geocodeLocationText(rawQuery: String) {
        val query = rawQuery.trim()
        if (query.length < 3) return

        isGeocoding = true
        try {
            val coords = locationService.getCoordinates(query)
            if (coords != null) {
                latitude = coords.first
                longitude = coords.second
            }
        } finally {
            isGeocoding = false
        }
    }

    LaunchedEffect(locationText, shouldGeocodeText) {
        if (!shouldGeocodeText) return@LaunchedEffect

        val query = locationText.trim()
        if (query.length < 3) return@LaunchedEffect

        delay(800L.milliseconds) // debounce so we don't query on every keystroke
        geocodeLocationText(query)
        shouldGeocodeText = false
    }

    // gets the current location and resolves it to the nearest named place/address
    // replaceTypedText = true only when the user taps "Use current location"
    fun fetchCurrentLocation(replaceTypedText: Boolean) {
        scope.launch {
            isLocating = true
            try {
                val location = locationService.getCurrentLocation()

                if (location != null) {
                    // the lookup takes a few seconds; if the user started typing in the meantime,
                    // keep what they typed and don't replace it
                    if (replaceTypedText || !userEnteredLocation) {
                        reverseGeocodeJob?.cancel()
                        latitude = location.latitude
                        longitude = location.longitude
                        shouldGeocodeText = false
                        locationText = locationService.resolveLocationName(
                            location.latitude,
                            location.longitude
                        )
                        userEnteredLocation = false
                    }
                } else {
                    Toast.makeText(context, "Couldn't get your location", Toast.LENGTH_SHORT).show()
                }
            } finally {
                isLocating = false
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            fetchCurrentLocation(replaceTypedText = pendingReplace)
        } else {
            Toast.makeText(
                context,
                "Location permission denied, type the location instead",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // checks permission first, then gets the location
    fun requestCurrentLocation(replaceTypedText: Boolean) {
        pendingReplace = replaceTypedText
        val hasPermission =
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            fetchCurrentLocation(replaceTypedText)
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // On new items, automatically fetch the current location.
    // On edited items that have saved coordinates but no saved location name, resolve the name.
    LaunchedEffect(Unit) {
        if (!isEditing && latitude == null && locationText.isBlank()) {
            requestCurrentLocation(replaceTypedText = false)
        } else if (isEditing && locationText.isBlank() && latitude != null && longitude != null) {
            isLocating = true
            try {
                locationText = locationService.resolveLocationName(latitude!!, longitude!!)
            } finally {
                isLocating = false
            }
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (spoken != null) {
            // Add to whatever is already typed instead of replacing it
            description = if (description.isBlank()) spoken else "$description $spoken"
        }
    }

    fun startSpeech() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Describe the item")
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                context,
                "Speech input isn't available on this device",
                Toast.LENGTH_SHORT
            ).show()
            print(e)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (isEditing) "Edit Item" else "New Item",
            style = MaterialTheme.typography.headlineSmall
        )

        if (mediaPath.isNotBlank()) {
            if (isVideo) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            val controller = MediaController(ctx)
                            controller.setAnchorView(this)
                            setMediaController(controller)
                            setVideoURI(Uri.fromFile(File(mediaPath)))
                            setOnPreparedListener { mp ->
                                mp.isLooping = true
                                start() // Automatically plays muted or displays looped preview
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
            } else {
                AsyncImage(
                    model = File(mediaPath),
                    contentDescription = "Photo of the item",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(140.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }
        }

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedButton(onClick = { startSpeech() }) {
            Text("🎤 Speak description")
        }

        OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
            Text(
                dueDate?.let { "Due: ${DueDate.format(it)}" } ?: "Set due date & time (optional)"
            )
        }
        if (dueDate != null) {
            TextButton(onClick = { dueDate = null }) { Text("Clear due date") }
        }

        // LOCATION: filled with the nearest place/address, user can edit or tap map
        OutlinedTextField(
            value = locationText,
            onValueChange = {
                reverseGeocodeJob?.cancel()
                locationText = it
                userEnteredLocation = true   // the user's text wins over the automatic location
                shouldGeocodeText = true
            },
            label = { Text("Location") },
            placeholder = { Text("e.g. 2000 Simcoe St N, Oshawa") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    shouldGeocodeText = false
                    scope.launch { geocodeLocationText(locationText) }
                }
            ),
            supportingText = {
                when {
                    isLocating -> Text("Resolving location...")
                    isGeocoding -> Text("Finding address on map...")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedButton(
            onClick = { requestCurrentLocation(replaceTypedText = true) },
            enabled = !isLocating
        ) {
            Text("📍 Use current location")
        }

        val lat = latitude
        val lon = longitude
        if (lat != null && lon != null) {
            ItemMap(
                latitude = lat,
                longitude = lon,
                markerTitle = locationText.ifBlank { "Item location" },
                onLocationPicked = { newLat, newLon, poiName ->
                    reverseGeocodeJob?.cancel()
                    latitude = newLat
                    longitude = newLon
                    userEnteredLocation = true
                    shouldGeocodeText = false

                    if (!poiName.isNullOrBlank()) {
                        locationText = poiName
                    } else {
                        reverseGeocodeJob = scope.launch {
                            isLocating = true
                            try {
                                locationText = locationService.resolveLocationName(newLat, newLon)
                            } finally {
                                isLocating = false
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onRetake, modifier = Modifier.weight(1f)) {
                Text(if (isEditing) "Back" else "Retake")
            }
            Button(
                onClick = {
                    onSave(
                        description.trim(),
                        dueDate,
                        locationText.trim().ifBlank { null },
                        latitude,
                        longitude
                    )
                },
                enabled = description.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }
        }
    }

    if (showDatePicker) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = dueDate?.let { DueDate.pickerDate(it) }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    // go on to the optional time step (or clear if no date was picked)
                    val picked = dateState.selectedDateMillis
                    if (picked == null) dueDate = null else pendingDate = picked
                    showDatePicker = false
                }) { Text("Next") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = dateState)
        }
    }

    // Step 2: optional time for the chosen date
    val dateForTime = pendingDate
    if (dateForTime != null) {
        val (startHour, startMinute) = dueDate?.let { DueDate.timeOf(it) } ?: (9 to 0)
        val timeState = rememberTimePickerState(
            initialHour = startHour,
            initialMinute = startMinute,
            is24Hour = DateFormat.is24HourFormat(context)
        )

        AlertDialog(
            onDismissRequest = { pendingDate = null }, // tapping outside keeps the old due date
            title = { Text("Add a time?") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    dueDate = DueDate.withTime(dateForTime, timeState.hour, timeState.minute)
                    pendingDate = null
                }) { Text("Set time") }
            },
            dismissButton = {
                TextButton(onClick = {
                    dueDate = dateForTime // date only
                    pendingDate = null
                }) { Text("No time") }
            }
        )
    }
}
