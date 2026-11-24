package com.group6.whereit

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.group6.whereit.camera.ItemCreationScreen
import com.group6.whereit.data.SpotItem
import com.group6.whereit.data.SpotRepository
import com.group6.whereit.data.WhereItDatabase
import kotlinx.coroutines.launch

class EditItemActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = WhereItDatabase.getInstance(this)
        val repository = SpotRepository(db.spotItemDao())

        // Same pattern as lab2: read the ID from the Intent
        val itemId = intent.getLongExtra("item_id", -1L)

        setContent {
            var item by remember { mutableStateOf<SpotItem?>(null) }

            // Fetch the item from Room when the Activity opens
            LaunchedEffect(itemId) {
                if (itemId != -1L) {
                    item = repository.getById(itemId)
                }
            }

            val loadedItem = item
            if (loadedItem != null) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ItemCreationScreen(
                        mediaPath = loadedItem.photoPath,
                        isVideo = loadedItem.isVideo,
                        initialDescription = loadedItem.description,
                        initialDueDateMillis = loadedItem.dueDate,
                        isEditing = true,
                        // start with the item's saved location instead of the current one
                        initialLocationName = loadedItem.locationName,
                        initialLatitude = loadedItem.latitude,
                        initialLongitude = loadedItem.longitude,
                        onSave = { updatedDescription, updatedDueDate,
                                   updatedLocationName, updatedLatitude, updatedLongitude ->
                            lifecycleScope.launch {
                                // save every field the user can change, not just the description
                                repository.update(
                                    loadedItem.copy(
                                        description = updatedDescription,
                                        dueDate = updatedDueDate,
                                        locationName = updatedLocationName,
                                        latitude = updatedLatitude,
                                        longitude = updatedLongitude
                                    )
                                )
                                Toast.makeText(
                                    this@EditItemActivity,
                                    "Saved: $updatedDescription",
                                    Toast.LENGTH_SHORT
                                ).show()
                                finish()
                            }
                        },
                        onRetake = {
                            // Acts as the Back/Cancel button in edit mode
                            finish()
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
