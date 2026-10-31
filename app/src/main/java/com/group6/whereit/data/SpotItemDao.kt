package com.group6.whereit.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// DAO = Data Access Object. You write function names + SQL; Room writes the code.
@Dao
interface SpotItemDao {

    @Insert
    suspend fun insert(item: SpotItem): Long

    @Update
    suspend fun update(item: SpotItem)

    @Delete
    suspend fun delete(item: SpotItem)

    // Flow = live stream. When the table changes, the UI updates automatically.
    @Query("SELECT * FROM spot_items WHERE userId = :userId ORDER BY isPinned DESC, timestamp DESC")
    fun getItemsForUser(userId: String): Flow<List<SpotItem>>

    @Query("SELECT * FROM spot_items WHERE userId = :userId AND isActive = 1 ORDER BY timestamp DESC")
    fun getActiveItems(userId: String): Flow<List<SpotItem>>

    @Query("SELECT * FROM spot_items WHERE id = :id")
    suspend fun getById(id: Long): SpotItem?
}