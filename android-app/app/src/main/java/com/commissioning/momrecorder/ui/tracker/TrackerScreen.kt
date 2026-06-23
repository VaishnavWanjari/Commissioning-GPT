package com.commissioning.momrecorder.ui.tracker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.commissioning.momrecorder.model.ActionStatus
import com.commissioning.momrecorder.model.Priority
import com.commissioning.momrecorder.model.TrackerItem
import com.commissioning.momrecorder.ui.theme.*
import com.commissioning.momrecorder.viewmodel.MainViewModel

private val filters = listOf("ALL" to "All", "PENDING" to "Pending", "IN_PROGRESS" to "In Progress", "COMPLETED" to "Done")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerScreen(vm: MainViewModel, modifier: Modifier = Modifier) {
    val items by vm.trackerItems.collectAsState()
    var activeFilter by remember { mutableStateOf("ALL") }

    LaunchedEffect(Unit) { vm.loadTrackerItems("ALL") }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Action Tracker", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Indigo800,
                titleContentColor = Color.White
            )
        )

        // Filter chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filters) { (key, label) ->
                FilterChip(
                    selected = activeFilter == key,
                    onClick = {
                        activeFilter = key
                        vm.loadTrackerItems(key)
                    },
                    label = { Text(label) }
                )
            }
        }

        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.TaskAlt, null, tint = Gray200, modifier = Modifier.size(72.dp))
                    Text(
                        "No action items",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Gray700
                    )
                    Text(
                        "Generate a MOM to track action items here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Gray500,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items, key = { it.actionItem.id }) { item ->
                    ActionTrackerCard(
                        item = item,
                        onStatusCycle = {
                            val next = when (item.actionItem.status) {
                                ActionStatus.PENDING -> ActionStatus.IN_PROGRESS
                                ActionStatus.IN_PROGRESS -> ActionStatus.COMPLETED
                                ActionStatus.COMPLETED -> ActionStatus.PENDING
                                ActionStatus.DEFERRED -> ActionStatus.PENDING
                            }
                            vm.updateActionStatus(item.meetingId, item.actionItem.id, next)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionTrackerCard(item: TrackerItem, onStatusCycle: () -> Unit) {
    val action = item.actionItem
    val isDone = action.status == ActionStatus.COMPLETED

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Priority bar
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(
                        when (action.priority) {
                            Priority.HIGH -> RecordingRed
                            Priority.MEDIUM -> Amber500
                            Priority.LOW -> Green500
                        },
                        RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)
                    )
            )

            Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = action.action,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDone) Gray500 else Gray900,
                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(8.dp))
                    // Status chip
                    val (chipLabel, chipBg, chipText) = when (action.status) {
                        ActionStatus.PENDING -> Triple("Pending", Color(0xFFFFF7ED), Amber500)
                        ActionStatus.IN_PROGRESS -> Triple("In Progress", Indigo50, Indigo800)
                        ActionStatus.COMPLETED -> Triple("Done", Green100, Green500)
                        ActionStatus.DEFERRED -> Triple("Deferred", Gray100, Gray500)
                    }
                    Surface(
                        onClick = onStatusCycle,
                        shape = RoundedCornerShape(20.dp),
                        color = chipBg
                    ) {
                        Text(
                            chipLabel,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = chipText,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Meeting name
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.FolderOpen, null, tint = Gray500, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        item.meetingTitle.ifBlank { "Untitled" },
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (action.assignedTo.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Person, null, tint = Indigo600, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(action.assignedTo, style = MaterialTheme.typography.bodySmall, color = Indigo600)
                        }
                    }
                    if (action.dueDate.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CalendarToday, null, tint = Gray500, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(action.dueDate, style = MaterialTheme.typography.bodySmall, color = Gray500)
                        }
                    }
                }
            }
        }
    }
}
