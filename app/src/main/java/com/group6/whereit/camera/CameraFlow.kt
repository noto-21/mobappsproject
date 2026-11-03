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
    Camera feature entry point: take photo -> fill in item details -> save.
    onItemSaved is where the database (SpotItem repository) plugs in.
 */
@Composable
fun CameraFlow(
    onItemSaved: (photoPath: String, description: String, dueDateMillis: Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var photoPath by rememberSaveable { mutableStateOf<String?>(null) }

    val currentPath = photoPath
    if (currentPath == null) {
        CameraScreen(
            onPhotoTaken = { path -> photoPath = path },
            modifier = modifier
        )
    } else {
        ItemCreationScreen(
            photoPath = currentPath,
            onSave = { description, dueDate ->
                onItemSaved(currentPath, description, dueDate)
                Toast.makeText(context, "Item saved", Toast.LENGTH_SHORT).show()
                photoPath = null            // back to the camera for the next item
            },
            onRetake = {
                File(currentPath).delete()  // throw away the unused photo
                photoPath = null
            },
            modifier = modifier
        )
    }
}

