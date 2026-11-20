package com.group6.whereit.ui.items

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.group6.whereit.data.SpotItem
import com.group6.whereit.data.SpotRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemsListScreen(repository: SpotRepository, userId: String) {

    // Live list from the database. Updates automatically when data changes.
    val items by repository.getUserItems(userId).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { TopAppBar(title = { Text("My Items") }) },
        floatingActionButton = {
            // TEMPORARY: adds a fake item so we can test before the camera is ready
            FloatingActionButton(onClick = {
                val samples = listOf(
                    Triple("Car - Lot 5, Row C", 43.9452, -78.8968),
                    Triple("Bike locked at SIRC building", 43.9447, -78.8957),
                    Triple("Spare keys - kitchen drawer", 43.8971, -79.0186),
                    Triple("Laptop charger - library 2nd floor", 43.9443, -78.8962),
                    Triple("Umbrella - ERC lecture hall", 43.9456, -78.8971)
                )
                val (desc, lat, lng) = samples[items.size % samples.size]
                scope.launch {
                    repository.save(
                        SpotItem(userId = userId, photoPath = "", description = desc,
                            latitude = lat, longitude = lng)
                    )
                }
            }) { Text("+") }
        }
    ) { padding ->
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding)) {
                Text("No items yet. Tap + to add one.", Modifier.padding(16.dp))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    SpotItemCard(
                        item = item,
                        onTogglePin = { scope.launch { repository.togglePin(item) } },
                        onDelete = { scope.launch { repository.delete(item) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun SpotItemCard(item: SpotItem, onTogglePin: () -> Unit, onDelete: () -> Unit) {
    val date = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(item.timestamp))

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = (if (item.isPinned) "📌 " else "") + item.description,
                style = MaterialTheme.typography.titleMedium
            )
            Text(date, style = MaterialTheme.typography.bodySmall)
            if (item.latitude != null && item.longitude != null) {
                Text(
                    "📍 %.4f, %.4f".format(item.latitude, item.longitude),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onTogglePin) { Text(if (item.isPinned) "Unpin" else "Pin") }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}