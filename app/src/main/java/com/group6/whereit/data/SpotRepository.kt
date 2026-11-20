package com.group6.whereit.data

import kotlinx.coroutines.flow.Flow

// The "receptionist": screens call these functions and never touch Room directly.

class SpotRepository(private val dao: SpotItemDao) {

    suspend fun save(item: SpotItem): Long = dao.insert(item)

    suspend fun update(item: SpotItem) = dao.update(item)

    suspend fun delete(item: SpotItem) = dao.delete(item)

    fun getUserItems(userId: String): Flow<List<SpotItem>> = dao.getItemsForUser(userId)

    fun getActive(userId: String): Flow<List<SpotItem>> = dao.getActiveItems(userId)

    suspend fun togglePin(item: SpotItem) = dao.update(item.copy(isPinned = !item.isPinned))
}