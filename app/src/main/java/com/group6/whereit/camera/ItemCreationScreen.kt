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
//imports for video
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.layout.height
import androidx.compose.ui.viewinterop.AndroidView
//imports for location
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.content.ContextCompat
import com.group6.whereit.LocationService
import kotlinx.coroutines.launch


@Composable
fun ItemCreationScreen(
    mediaPath: String,
    isVideo: Boolean,
    onSave: (
        description: String,
        dueDateMillis: Long?,
        locationName: String?,
        latitude: Double?,
        longitude: Double?
    ) -> Unit,
    onRetake: () -> Unit,
    modifier: Modifier = Modifier,
    initialDescription: String = "",
    initialDueDateMillis: Long? = null,
    isEditing: Boolean = false
) {
    val context = LocalContext.current

    val scope = rememberCoroutineScope()
    val locationService = remember { LocationService(context) }

    var description by rememberSaveable(
        initialDescription
    ) { mutableStateOf(initialDescription) }
    var dueDateMillis by rememberSaveable(
        initialDueDateMillis
    ) { mutableStateOf(initialDueDateMillis) }
    var showDatePicker by remember { mutableStateOf(false) }

    // LOCATION: readable address shown in the text field + the coordinates behind it
    var locationText by rememberSaveable { mutableStateOf("") }
    var latitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var longitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var isLocating by remember { mutableStateOf(false) }

    // gets the current location and puts its address in the location field
    fun fetchCurrentLocation() {
        scope.launch {
            isLocating = true
            val location = locationService.getCurrentLocation()

            if (location != null) {
                latitude = location.latitude
                longitude = location.longitude

                // use the address if one is found, otherwise fall back to the coordinates
                locationText = locationService.getAddress(location)
                    ?: String.format(Locale.US, "%.5f, %.5f", location.latitude, location.longitude)
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
            fetchCurrentLocation()
        } else {
            Toast.makeText(context, "Location permission denied, type the location instead", Toast.LENGTH_SHORT).show()
        }
    }

    // checks permission first, then gets the location
    fun requestCurrentLocation() {
        val hasPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            fetchCurrentLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // fill in the current location automatically when the screen opens
    // (skipped if we already have one, e.g. after rotating the phone)
    LaunchedEffect(Unit) {
        if (latitude == null && locationText.isBlank()) {
            requestCurrentLocation()
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
            .verticalScroll(rememberScrollState())
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
                dueDateMillis?.let { "Due: ${formatDate(it)}" } ?: "Set due date (optional)"
            )
        }
        if (dueDateMillis != null) {
            TextButton(onClick = { dueDateMillis = null }) { Text("Clear due date") }
        }

        // LOCATION: filled with the current location's address, user can edit it
        OutlinedTextField(
            value = locationText,
            onValueChange = { locationText = it },
            label = { Text("Location") },
            placeholder = { Text("e.g. Kitchen, top drawer") },
            supportingText = {
                if (isLocating) Text("Getting current location...")
            },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedButton(
            onClick = { requestCurrentLocation() },
            enabled = !isLocating
        ) {
            Text("📍 Use current location")
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
                        dueDateMillis,
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
        val dateState = rememberDatePickerState(initialSelectedDateMillis = dueDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dueDateMillis = dateState.selectedDateMillis
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = dateState)
        }
    }
}

/** DatePicker gives midnight UTC, so format in UTC or the date shows up a day early in Toronto. */
private fun formatDate(millis: Long): String {
    val format = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    format.timeZone = TimeZone.getTimeZone("UTC")
    return format.format(Date(millis))
}
