package com.group6.whereit.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Lists every table and creates the actual SQLite file (whereit.db) on the phone.
@Database(entities = [SpotItem::class], version = 1)
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
                ).build().also { INSTANCE = it }
            }
    }
}