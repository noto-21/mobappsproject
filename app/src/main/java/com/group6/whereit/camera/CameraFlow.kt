package com.group6.whereit.camera

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.io.File

/**
    Camera feature entry point: take photo or video -> fill in item details -> save.
    onItemSaved is where the database (SpotItem repository) plugs in.
 */
@Composable
fun CameraFlow(
    onItemSaved: (
        mediaPath: String,
        isVideo: Boolean,
        description: String,
        dueDate: Long?,
        locationName: String?,
        latitude: Double?,
        longitude: Double?
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mediaPath by rememberSaveable { mutableStateOf<String?>(null) }
    var isVideo by rememberSaveable { mutableStateOf(false) }

    val currentPath = mediaPath
    //mediaPath is a simple state switch to go between CameraScreen and ItemCreationScreen
    if (currentPath == null) {
        CameraScreen(
            onMediaCaptured = { path, video ->
                mediaPath = path
                isVideo = video
            },
            modifier = modifier
        )
    } else {
        ItemCreationScreen(
            mediaPath = currentPath,
            isVideo = isVideo,
            onSave = { description, dueDate, locationName, latitude, longitude ->
                // MainActivity saves the item to the database and switches to the item library
                onItemSaved(currentPath, isVideo, description, dueDate, locationName, latitude, longitude)
                Toast.makeText(context, "Item saved", Toast.LENGTH_SHORT).show()
                mediaPath = null            // reset so the camera starts fresh next time
                isVideo = false
            },
            onRetake = {
                File(currentPath).delete()  // throw away the unused photo
                mediaPath = null
                isVideo = false
            },
            modifier = modifier
        )
    }
}

