package com.group6.whereit.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Lists every table and creates the actual SQLite file (whereit.db) on the phone.
// version 2: added locationName, dueDate and isVideo to SpotItem
@Database(entities = [SpotItem::class], version = 2)
abstract class WhereItDatabase : RoomDatabase() {

    abstract fun spotItemDao(): SpotItemDao

    companion object {
        @Volatile private var INSTANCE: WhereItDatabase? = null

        // Only ever create ONE database instance for the whole app.
        fun getInstance(context: Context): WhereItDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WhereItDatabase::class.java,
                    "whereit.db"
                )
                    // When the table changes (version bump), wipe and recreate the test database
                    // instead of crashing. Fine while developing; old test items are cleared.
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}