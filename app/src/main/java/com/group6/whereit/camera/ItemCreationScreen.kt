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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.viewinterop.AndroidView


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemCreationScreen(
    mediaPath: String,
    isVideo: Boolean,
    onSave: (description: String, dueDateMillis: Long?) -> Unit,
    onRetake: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var description by rememberSaveable { mutableStateOf("") }
    var dueDateMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }


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
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("New Item", style = MaterialTheme.typography.headlineSmall)

        if (isVideo){
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        val controller = MediaController(ctx)
                        controller.setAnchorView(this)
                        setMediaController(controller)
                        setVideoURI(Uri.fromFile(File(mediaPath)))
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            start() // Auto-plays muted or displays looped preview
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

        // TODO: location field (handled by location teammate)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onRetake, modifier = Modifier.weight(1f)) {
                Text("Retake")
            }
            Button(
                onClick = { onSave(description.trim(), dueDateMillis) },
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
