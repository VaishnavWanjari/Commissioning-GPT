package com.commissioning.momrecorder.ui.detail

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.commissioning.momrecorder.model.ActionItem
import com.commissioning.momrecorder.model.MomReport
import com.commissioning.momrecorder.model.Priority
import com.commissioning.momrecorder.ui.theme.*
import com.commissioning.momrecorder.util.PdfExporter
import com.commissioning.momrecorder.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomDetailScreen(momId: String, vm: MainViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val mom = vm.getMomById(momId)
    if (mom == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    var pdfExporting by remember { mutableStateOf(false) }

    fun exportPdf() {
        pdfExporting = true
        scope.launch {
            val file = withContext(Dispatchers.IO) { PdfExporter.exportToPdf(context, mom) }
            pdfExporting = false
            if (file != null) {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                context.startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/pdf")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        },
                        "Open PDF"
                    )
                )
            } else {
                snackbar.showSnackbar("PDF export failed")
            }
        }
    }

    fun shareMom() {
        val text = buildShareText(mom)
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                    putExtra(Intent.EXTRA_SUBJECT, mom.meetingTitle.ifBlank { "Meeting Minutes" })
                },
                "Share MOM"
            )
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        mom.meetingTitle.ifBlank { "Meeting Minutes" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (pdfExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 8.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = ::exportPdf) {
                            Icon(Icons.Filled.PictureAsPdf, "Export PDF", tint = Color.White)
                        }
                    }
                    IconButton(onClick = ::shareMom) {
                        Icon(Icons.Filled.Share, "Share", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Indigo800,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hero header
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Indigo800)
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("MINUTES OF MEETING", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.7f))
                        Text(
                            mom.meetingTitle.ifBlank { "Meeting" },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        HorizontalDivider(color = Color.White.copy(0.3f))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            if (mom.date.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.CalendarToday, null, tint = Color.White.copy(0.8f), modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(mom.date, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.8f))
                                }
                            }
                            if (mom.duration.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Timer, null, tint = Color.White.copy(0.8f), modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(mom.duration, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.8f))
                                }
                            }
                        }
                        if (mom.attendees.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Filled.Group, null, tint = Color.White.copy(0.8f), modifier = Modifier.size(14.dp).padding(top = 2.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(mom.attendees.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.8f))
                            }
                        }
                    }
                }
            }

            if (mom.summary.isNotBlank()) {
                item { MomSection("Executive Summary") { Text(mom.summary, style = MaterialTheme.typography.bodyMedium, color = Gray700) } }
            }

            if (mom.keyDecisions.isNotEmpty()) {
                item {
                    MomSection("Key Decisions") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            mom.keyDecisions.forEachIndexed { i, d ->
                                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(shape = RoundedCornerShape(50), color = Indigo100) {
                                        Text("${i + 1}", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelMedium, color = Indigo800, fontWeight = FontWeight.Bold)
                                    }
                                    Column {
                                        Text(d.decision, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = Gray900)
                                        if (d.context.isNotBlank()) {
                                            Text(d.context, style = MaterialTheme.typography.bodySmall, color = Gray500)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (mom.actionItems.isNotEmpty()) {
                item {
                    MomSection("Action Items (${mom.actionItems.size})") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            mom.actionItems.forEach { action ->
                                ActionItemRow(action)
                            }
                        }
                    }
                }
            }

            if (mom.discussionPoints.isNotEmpty()) {
                item {
                    MomSection("Discussion Points") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            mom.discussionPoints.forEach { pt ->
                                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("•", style = MaterialTheme.typography.bodyMedium, color = Indigo600, fontWeight = FontWeight.Bold)
                                    Text(pt, style = MaterialTheme.typography.bodyMedium, color = Gray700)
                                }
                            }
                        }
                    }
                }
            }

            if (mom.nextSteps.isNotEmpty()) {
                item {
                    MomSection("Next Steps") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            mom.nextSteps.forEachIndexed { i, step ->
                                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("${i + 1}.", style = MaterialTheme.typography.bodyMedium, color = Indigo600, fontWeight = FontWeight.Bold)
                                    Text(step, style = MaterialTheme.typography.bodyMedium, color = Gray700)
                                }
                            }
                        }
                    }
                }
            }

            if (mom.nextMeetingDate.isNotBlank()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Green100
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Event, null, tint = Green500, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Next Meeting", style = MaterialTheme.typography.labelMedium, color = Green500, fontWeight = FontWeight.Bold)
                                Text(mom.nextMeetingDate, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF065F46))
                            }
                        }
                    }
                }
            }

            if (mom.remarks.isNotBlank()) {
                item { MomSection("Remarks") { Text(mom.remarks, style = MaterialTheme.typography.bodyMedium, color = Gray700) } }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun MomSection(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(4.dp).height(18.dp).clip(RoundedCornerShape(2.dp)).background(Indigo600))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Indigo800)
            }
            content()
        }
    }
}

@Composable
private fun ActionItemRow(action: ActionItem) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Gray50,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(IntrinsicSize.Min)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        when (action.priority) {
                            Priority.HIGH -> RecordingRed
                            Priority.MEDIUM -> Amber500
                            Priority.LOW -> Green500
                        }
                    )
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(action.action, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = Gray900)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (action.assignedTo.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Person, null, tint = Indigo600, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(action.assignedTo, style = MaterialTheme.typography.bodySmall, color = Indigo600)
                        }
                    }
                    if (action.dueDate.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CalendarToday, null, tint = Gray500, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(action.dueDate, style = MaterialTheme.typography.bodySmall, color = Gray500)
                        }
                    }
                }
            }
        }
    }
}

private fun buildShareText(mom: MomReport) = buildString {
    appendLine("MINUTES OF MEETING")
    appendLine("==================")
    appendLine("Title: ${mom.meetingTitle.ifBlank { "Meeting" }}")
    if (mom.date.isNotBlank()) appendLine("Date: ${mom.date}")
    if (mom.duration.isNotBlank()) appendLine("Duration: ${mom.duration}")
    appendLine()
    if (mom.summary.isNotBlank()) { appendLine("SUMMARY"); appendLine(mom.summary); appendLine() }
    if (mom.actionItems.isNotEmpty()) {
        appendLine("ACTION ITEMS")
        mom.actionItems.forEachIndexed { i, a ->
            appendLine("${i + 1}. ${a.action} — ${a.assignedTo} by ${a.dueDate} [${a.priority}]")
        }
        appendLine()
    }
    appendLine("Generated by Minutes")
}
