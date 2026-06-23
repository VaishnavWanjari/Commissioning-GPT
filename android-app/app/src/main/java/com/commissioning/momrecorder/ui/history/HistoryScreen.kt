package com.commissioning.momrecorder.ui.history

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.commissioning.momrecorder.model.MomReport
import com.commissioning.momrecorder.ui.theme.*
import com.commissioning.momrecorder.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    vm: MainViewModel,
    onOpenMom: (MomReport) -> Unit,
    modifier: Modifier = Modifier
) {
    val moms by vm.savedMoms.collectAsState()
    var deleteTarget by remember { mutableStateOf<MomReport?>(null) }

    // Delete confirmation
    deleteTarget?.let { mom ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            icon = { Icon(Icons.Filled.DeleteForever, null, tint = RecordingRed) },
            title = { Text("Delete MOM") },
            text = { Text("Delete '${mom.meetingTitle.ifBlank { "this meeting" }}'? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { vm.deleteMom(mom.id); deleteTarget = null },
                    colors = ButtonDefaults.buttonColors(containerColor = RecordingRed)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("History", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Indigo800,
                titleContentColor = Color.White
            )
        )

        if (moms.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.History, null, tint = Gray200, modifier = Modifier.size(72.dp))
                    Text(
                        "No MOM reports yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Gray700
                    )
                    Text(
                        "Record a meeting and generate your first report!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Gray500,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(moms, key = { it.id }) { mom ->
                    MomCard(
                        mom = mom,
                        onClick = { onOpenMom(mom) },
                        onDelete = { deleteTarget = mom }
                    )
                }
            }
        }
    }
}

@Composable
private fun MomCard(mom: MomReport, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mom.meetingTitle.ifBlank { "Meeting" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.CalendarToday, null, tint = Indigo600, modifier = Modifier.size(14.dp))
                        Text(
                            text = mom.date.ifBlank {
                                SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault())
                                    .format(Date(mom.createdAt))
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Indigo600
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, null, tint = Gray500, modifier = Modifier.size(20.dp))
                }
            }

            if (mom.summary.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = mom.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray700,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Gray200)
            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (mom.actionItems.isNotEmpty()) {
                    AssistChip(
                        onClick = {},
                        label = { Text("${mom.actionItems.size} actions", style = MaterialTheme.typography.labelMedium) },
                        leadingIcon = { Icon(Icons.Filled.TaskAlt, null, Modifier.size(14.dp)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Indigo50,
                            labelColor = Indigo800,
                            leadingIconContentColor = Indigo800
                        ),
                        border = null
                    )
                }
                if (mom.duration.isNotBlank()) {
                    AssistChip(
                        onClick = {},
                        label = { Text(mom.duration, style = MaterialTheme.typography.labelMedium) },
                        leadingIcon = { Icon(Icons.Filled.Timer, null, Modifier.size(14.dp)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Gray100,
                            labelColor = Gray700,
                            leadingIconContentColor = Gray500
                        ),
                        border = null
                    )
                }
            }
        }
    }
}
