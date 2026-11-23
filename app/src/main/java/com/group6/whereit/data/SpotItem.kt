package com.group6.whereit.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "spot_items")
data class SpotItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val photoPath: String,         // file location of the photo (or video) on the phone
    val description: String,
    val latitude: Double? = null,  // null if GPS wasn't available
    val longitude: Double? = null,
    val locationName: String? = null, // readable location (address) shown in the item creation screen
    val dueDate: Long? = null,        // optional due date picked on the item creation screen
    val isVideo: Boolean = false,     // true if photoPath points to a video instead of a photo
    val timestamp: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val isPinned: Boolean = false
)
