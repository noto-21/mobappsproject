package com.group6.whereit

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.group6.whereit.data.DueDate
import com.group6.whereit.data.SpotItem
import com.group6.whereit.data.SpotRepository
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemsListScreen(
    repository: SpotRepository,
    userId: String,
    onAddItem: () -> Unit,  // called by the + button, MainActivity switches to the camera
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Live list from the database. Updates automatically when data changes.
    val items by repository.getUserItems(userId).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("My Items") }) },
        floatingActionButton = {
            // + button opens the camera to create a new item
            FloatingActionButton(onClick = onAddItem) { Text("+") }
        }
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Text("No items yet. Tap + to add one.", Modifier.padding(16.dp))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    SpotItemCard(
                        item = item,
                        onClick = {
                            val intent = Intent(context, EditItemActivity::class.java)
                            intent.putExtra("item_id", item.id)
                            context.startActivity(intent)
                        },
                        onTogglePin = { scope.launch { repository.togglePin(item) } },
                        onDelete = { scope.launch { repository.delete(item) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun SpotItemCard(
    item: SpotItem,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    val date = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(item.timestamp))

    val displayLocation = item.locationName?.takeIf { it.isNotBlank() }
            ?: if (item.latitude != null && item.longitude != null) {
                String.format(Locale.US, "%.4f, %.4f", item.latitude, item.longitude)
            } else {
                null
            }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.photoPath.isNotBlank()) {
                    AsyncImage(
                        model = File(item.photoPath),
                        contentDescription = "Photo of ${item.description}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        text = (if (item.isPinned) "📌 " else "") + item.description,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Saved: $date",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (item.dueDate != null) {
                        Text(
                            text = "⏰ Due: ${DueDate.format(item.dueDate)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (displayLocation != null) {
                        Text(
                            text = "📍 $displayLocation",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onTogglePin) {
                    Text(if (item.isPinned) "Unpin" else "Pin")
                }
                TextButton(onClick = onDelete) {
                    Text("Delete")
                }
            }
        }
    }
}
