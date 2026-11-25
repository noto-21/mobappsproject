package com.group6.whereit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.group6.whereit.data.DueDate
import com.group6.whereit.data.SpotItem
import com.group6.whereit.data.SpotRepository
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.content.Intent
import androidx.compose.ui.platform.LocalContext

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
            Box(Modifier
                .fillMaxSize()
                .padding(padding)) {
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

    Card(
        onClick = onClick,
        Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp)) {
            // thumbnail: the photo, or a placeholder for videos / items without a photo
            if (item.photoPath.isNotBlank() && !item.isVideo) {
                AsyncImage(
                    model = File(item.photoPath),
                    contentDescription = "Photo of ${item.description}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (item.isVideo) "🎥" else "📦", style = MaterialTheme.typography.headlineSmall)
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = (if (item.isPinned) "📌 " else "") + item.description,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(date, style = MaterialTheme.typography.bodySmall)

                // show the readable location if there is one, otherwise the coordinates
                if (!item.locationName.isNullOrBlank()) {
                    Text("📍 ${item.locationName}", style = MaterialTheme.typography.bodySmall)
                } else if (item.latitude != null && item.longitude != null) {
                    Text(
                        "📍 %.4f, %.4f".format(item.latitude, item.longitude),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // due date, if one was picked
                item.dueDate?.let {
                    Text("⏰ Due ${DueDate.format(it)}", style = MaterialTheme.typography.bodySmall)
                }

                // compact actions: plain clickable labels instead of TextButtons,
                // which reserve a 48dp touch target and add lots of padding
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End)
                ) {
                    CardAction(if (item.isPinned) "Unpin" else "Pin", onTogglePin)
                    CardAction("Delete", onDelete)
                }
            }
        }
    }
}

@Composable
private fun CardAction(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
