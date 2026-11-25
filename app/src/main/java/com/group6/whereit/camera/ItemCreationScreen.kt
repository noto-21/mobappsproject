package com.group6.whereit.camera

import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

import android.view.MotionEvent
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.pointerInteropFilter

//imports for video
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.layout.height
import androidx.compose.ui.viewinterop.AndroidView
//imports for location
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.content.ContextCompat
import com.group6.whereit.LocationService
import kotlinx.coroutines.launch
import android.location.Location
import com.group6.whereit.ItemMap
import com.group6.whereit.data.DueDate
import android.text.format.DateFormat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState


@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
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

    var description by rememberSaveable(
        initialDescription
    ) { mutableStateOf(initialDescription) }
    var dueDate by rememberSaveable(
        initialDueDate
    ) { mutableStateOf(initialDueDate) }
    var locationName by rememberSaveable(initialLocation) {
        mutableStateOf(initialLocation ?: "")
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

    var isMapTouched by remember { mutableStateOf(false) }

    // LOCATION: readable address shown in the text field + the coordinates behind it
    var locationText by rememberSaveable(initialLocation) {
        mutableStateOf(initialLocation ?: "")
    }
    var isLocating by remember { mutableStateOf(false) }

    // true once the user types their own location (or the item already has a saved one),
    // so the automatic current-location lookup never overwrites it
    var userEnteredLocation by rememberSaveable(initialLocation) {
        mutableStateOf(!initialLocation.isNullOrBlank())
    }

    // remembers whether the lookup waiting on the permission popup came from the button
    var pendingReplace by remember { mutableStateOf(false) }

    // gets the current location and puts its address in the location field
    // replaceTypedText = true only when the user taps "Use current location"
    fun fetchCurrentLocation(replaceTypedText: Boolean) {
        scope.launch {
            isLocating = true
            val location = locationService.getCurrentLocation()

            if (location != null) {
                // the lookup takes a few seconds; if the user started typing in the meantime,
                // keep what they typed and don't replace it
                if (replaceTypedText || !userEnteredLocation) {
                    latitude = location.latitude
                    longitude = location.longitude

                    // use the address if one is found, otherwise fall back to the coordinates
                    locationText = locationService.getAddress(location)
                        ?: String.format(Locale.US, "%.5f, %.5f", location.latitude, location.longitude)
                    userEnteredLocation = false
                }
            } else {
                Toast.makeText(context, "Couldn't get your location", Toast.LENGTH_SHORT).show()
            }
            isLocating = false
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
            Toast.makeText(context, "Location permission denied, type the location instead", Toast.LENGTH_SHORT).show()
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

    // fill in the current location automatically when a NEW item's screen opens
    // (skipped when editing, or if we already have a location, e.g. after rotating the phone)
    LaunchedEffect(Unit) {
        if (!isEditing && latitude == null && locationText.isBlank()) {
            requestCurrentLocation(replaceTypedText = false)
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
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Describe the item")
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Speech input isn't available on this device", Toast.LENGTH_SHORT).show()
            print(e)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState(), enabled = !isMapTouched)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (isEditing) "Edit Item" else "New Item",
            style = MaterialTheme.typography.headlineSmall
        )

        if (mediaPath.isNotBlank())
            if (isVideo)
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
            else
                AsyncImage(
                    model = File(mediaPath),
                    contentDescription = "Photo of the item",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(140.dp)
                        .align(Alignment.CenterHorizontally)
                )

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

        // LOCATION: filled with the current location's address, user can edit it
        OutlinedTextField(
            value = locationText,
            onValueChange = {
                locationText = it
                userEnteredLocation = true   // the user's text wins over the automatic location
            },
            label = { Text("Location") },
            placeholder = { Text("e.g. Kitchen, top drawer") },
            supportingText = {
                if (isLocating) Text("Getting current location...")
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
                onLocationPicked = { newLat, newLon ->
                    latitude = newLat
                    longitude = newLon
                    userEnteredLocation = true

                    scope.launch {
                        val pickedLocation = Location("").apply {
                            latitude = newLat
                            longitude = newLon
                        }
                        locationText = locationService.getAddress(pickedLocation)
                                ?: String.format(Locale.US, "%.5f, %.5f", newLat, newLon)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .pointerInteropFilter { event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> isMapTouched = true
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> isMapTouched = false
                        }
                        false // don't consume the event; let the map handle it
                    }
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

    if (showDatePicker)
    {
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
