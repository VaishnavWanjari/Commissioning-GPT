package com.commissioning.momrecorder.ui.record

import android.content.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.commissioning.momrecorder.model.MomReport
import com.commissioning.momrecorder.service.RecordingService
import com.commissioning.momrecorder.ui.theme.*
import com.commissioning.momrecorder.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    vm: MainViewModel,
    onNavigateToSettings: () -> Unit,
    onMomGenerated: (MomReport) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val recordingState by vm.recordingState.collectAsState()
    val duration by vm.duration.collectAsState()
    val transcriptEntries by vm.transcriptEntries.collectAsState()
    val momGenerating by vm.momGenerating.collectAsState()
    val hasApiKey = vm.prefs.hasApiKey()

    // Register broadcast receiver
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    RecordingService.BROADCAST_TRANSCRIPT -> {
                        val text = intent.getStringExtra(RecordingService.EXTRA_TEXT) ?: return
                        val isFinal = intent.getBooleanExtra(RecordingService.EXTRA_IS_FINAL, false)
                        vm.appendTranscript(text, isFinal)
                    }
                    RecordingService.BROADCAST_STATE -> {
                        val state = intent.getStringExtra(RecordingService.EXTRA_STATE) ?: return
                        val dur = intent.getLongExtra(RecordingService.EXTRA_DURATION, 0L)
                        vm.updateDuration(dur)
                        when (state) {
                            "PAUSED" -> vm.setPaused()
                            "RECORDING" -> vm.setResumed()
                            "STOPPED" -> vm.stopSession()
                        }
                    }
                    RecordingService.BROADCAST_ERROR -> {
                        val msg = intent.getStringExtra(RecordingService.EXTRA_ERROR_MSG) ?: return
                        scope.launch { snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Long) }
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(RecordingService.BROADCAST_TRANSCRIPT)
            addAction(RecordingService.BROADCAST_STATE)
            addAction(RecordingService.BROADCAST_ERROR)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { try { context.unregisterReceiver(receiver) } catch (_: Exception) {} }
    }

    // Collect one-shot events
    LaunchedEffect(Unit) {
        launch { vm.snackbar.collect { snackbarHostState.showSnackbar(it) } }
        launch { vm.momGenerated.collect { mom -> onMomGenerated(mom) } }
    }

    // Generate MOM dialog
    var showGenerateDialog by remember { mutableStateOf(false) }
    if (showGenerateDialog) {
        GenerateMomDialog(
            onConfirm = { title ->
                showGenerateDialog = false
                vm.generateMom(title, vm.prefs.defaultMeetingPlatform)
            },
            onDismiss = { showGenerateDialog = false }
        )
    }

    // Start recording dialog
    var showStartDialog by remember { mutableStateOf(false) }
    if (showStartDialog) {
        StartRecordingDialog(
            onConfirm = { platform ->
                showStartDialog = false
                vm.startSession(title = "Meeting on $platform")
                context.startForegroundService(
                    Intent(context, RecordingService::class.java).apply { action = RecordingService.ACTION_START }
                )
            },
            onDismiss = { showStartDialog = false }
        )
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Minutes", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Indigo800,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // API key warning
            AnimatedVisibility(
                visible = !hasApiKey,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFFBEB),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Warning, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "No API key — AI MOM generation won't work.",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF92400E)
                        )
                        TextButton(onClick = onNavigateToSettings) {
                            Text("Set up", color = Color(0xFFD97706), fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Recording hero card
            RecordingHeroCard(
                recordingState = recordingState,
                duration = duration,
                onRecordClick = {
                    when (recordingState) {
                        MainViewModel.RecordingState.IDLE,
                        MainViewModel.RecordingState.STOPPED -> showStartDialog = true
                        MainViewModel.RecordingState.RECORDING ->
                            context.startService(Intent(context, RecordingService::class.java).apply { action = RecordingService.ACTION_PAUSE })
                        MainViewModel.RecordingState.PAUSED ->
                            context.startService(Intent(context, RecordingService::class.java).apply { action = RecordingService.ACTION_RESUME })
                    }
                },
                onStopClick = {
                    context.startService(Intent(context, RecordingService::class.java).apply { action = RecordingService.ACTION_STOP })
                    vm.stopSession()
                }
            )

            Spacer(Modifier.height(16.dp))

            // Transcript card
            TranscriptCard(
                entries = transcriptEntries,
                onClear = { vm.startSession() }
            )

            Spacer(Modifier.height(16.dp))

            // MOM progress
            if (momGenerating) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Analyzing transcript with AI…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
            }

            // Generate MOM button
            val hasFinalTranscript = transcriptEntries.any { it.isFinal }
            Button(
                onClick = {
                    if (!hasApiKey) onNavigateToSettings()
                    else showGenerateDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(52.dp),
                enabled = hasFinalTranscript && !momGenerating,
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.AutoAwesome, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Generate MOM Report with AI", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RecordingHeroCard(
    recordingState: MainViewModel.RecordingState,
    duration: Long,
    onRecordClick: () -> Unit,
    onStopClick: () -> Unit
) {
    val isRecording = recordingState == MainViewModel.RecordingState.RECORDING

    // Pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // FAB color
    val fabColor by animateColorAsState(
        targetValue = if (isRecording) RecordingRed else Indigo800,
        animationSpec = tween(400),
        label = "fab_color"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mic button with pulse
            Box(contentAlignment = Alignment.Center) {
                if (isRecording) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(RecordingRed.copy(alpha = 0.2f))
                    )
                }
                FloatingActionButton(
                    onClick = onRecordClick,
                    containerColor = fabColor,
                    contentColor = Color.White,
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        imageVector = when (recordingState) {
                            MainViewModel.RecordingState.RECORDING -> Icons.Filled.Pause
                            else -> Icons.Filled.Mic
                        },
                        contentDescription = "Record",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Status text
            Text(
                text = when (recordingState) {
                    MainViewModel.RecordingState.IDLE, MainViewModel.RecordingState.STOPPED -> "Tap to start recording"
                    MainViewModel.RecordingState.RECORDING -> "Recording…"
                    MainViewModel.RecordingState.PAUSED -> "Paused — tap to resume"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = when (recordingState) {
                    MainViewModel.RecordingState.RECORDING -> RecordingRed
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            // Duration
            Text(
                text = formatDuration(duration),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            // Waveform bars (only when recording)
            AnimatedVisibility(
                visible = isRecording,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                WaveformBars()
            }

            // Stop button
            AnimatedVisibility(
                visible = recordingState == MainViewModel.RecordingState.RECORDING ||
                        recordingState == MainViewModel.RecordingState.PAUSED,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                OutlinedButton(
                    onClick = onStopClick,
                    border = BorderStroke(1.5.dp, RecordingRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Stop, null, tint = RecordingRed, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Stop Recording", color = RecordingRed, fontWeight = FontWeight.Medium)
                }
            }

            Text(
                text = "Start recording BEFORE joining your call for best results",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun WaveformBars() {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val heights = (0..4).map { i ->
        infiniteTransition.animateFloat(
            initialValue = 0.2f, targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(300 + i * 80, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar$i"
        )
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(40.dp)
    ) {
        heights.forEach { heightFraction ->
            val barHeight by heightFraction
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight(barHeight)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Indigo600)
            )
        }
    }
}

@Composable
private fun TranscriptCard(
    entries: List<com.commissioning.momrecorder.model.TranscriptEntry>,
    onClear: () -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) listState.animateScrollToItem(entries.size - 1)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Gray100)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.TextFields, null, tint = Indigo600, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Live Transcript",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (entries.isNotEmpty()) {
                    TextButton(onClick = onClear) { Text("Clear", color = RecordingRed, fontSize = 12.sp) }
                }
            }

            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.GraphicEq, null, tint = Gray200, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Recording will appear here…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Gray500,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(entries, key = { "${it.timestamp}_${it.isFinal}" }) { entry ->
                        Text(
                            text = entry.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (entry.isFinal) Gray900 else Gray500,
                            modifier = Modifier.animateItemPlacement()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GenerateMomDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.AutoAwesome, null, tint = Indigo600) },
        title = { Text("Generate MOM Report") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "AI will analyse your transcript (Hindi + English) and produce a professional English MOM.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Meeting title (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(title.trim()) }) { Text("Generate") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun StartRecordingDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val platforms = listOf(
        "WhatsApp Video Call", "Instagram Video Call",
        "Zoom", "Google Meet", "Teams", "Phone Call", "Other"
    )
    var selected by remember { mutableIntStateOf(0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Mic, null, tint = Indigo600) },
        title = { Text("Start Recording") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Start recording BEFORE joining your call on speakerphone for best results.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                platforms.forEachIndexed { i, platform ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selected = i }
                            .background(if (selected == i) Indigo50 else Color.Transparent)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        RadioButton(selected = selected == i, onClick = { selected = i })
                        Text(platform, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(platforms[selected]) }) { Text("Start Recording") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun formatDuration(millis: Long): String {
    val mins = TimeUnit.MILLISECONDS.toMinutes(millis)
    val secs = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return String.format("%02d:%02d", mins, secs)
}
