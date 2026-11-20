package com.group6.whereit.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "spot_items")
data class SpotItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val photoPath: String,         // file location of the photo on the phone
    val description: String,
    val latitude: Double? = null,  // null if GPS wasn't available
    val longitude: Double? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val isPinned: Boolean = false
)
