package com.commissioning.momrecorder.ui.settings

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.commissioning.momrecorder.ui.theme.*
import com.commissioning.momrecorder.viewmodel.MainViewModel
import kotlinx.coroutines.launch

private val platforms = listOf("WhatsApp", "Instagram", "Google Meet", "Zoom", "Teams", "Video Call", "Phone Call", "In Person")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit = {}, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var apiKey by remember { mutableStateOf(vm.prefs.claudeApiKey) }
    var showKey by remember { mutableStateOf(false) }
    var validating by remember { mutableStateOf(false) }
    var validationResult by remember { mutableStateOf<Boolean?>(null) }

    var platform by remember { mutableStateOf(vm.prefs.defaultMeetingPlatform) }
    var platformExpanded by remember { mutableStateOf(false) }

    var participants by remember { mutableStateOf(vm.prefs.participantNames) }
    var autoGenerate by remember { mutableStateOf(vm.prefs.autoGenerateMom) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Indigo800,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // --- API Key Section ---
                SettingsSection(title = "Claude AI") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = {
                                apiKey = it
                                validationResult = null
                            },
                            label = { Text("Claude API Key") },
                            placeholder = { Text("sk-ant-api03-...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { showKey = !showKey }) {
                                    Icon(
                                        if (showKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = if (showKey) "Hide key" else "Show key",
                                        tint = Gray500
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    vm.prefs.claudeApiKey = apiKey.trim()
                                    scope.launch { snackbar.showSnackbar("API key saved") }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Indigo800)
                            ) {
                                Icon(Icons.Filled.Save, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Save Key")
                            }

                            OutlinedButton(
                                onClick = {
                                    val key = apiKey.trim()
                                    if (key.isBlank()) {
                                        scope.launch { snackbar.showSnackbar("Enter an API key first") }
                                        return@OutlinedButton
                                    }
                                    validating = true
                                    validationResult = null
                                    scope.launch {
                                        val ok = vm.validateApiKey(key)
                                        validating = false
                                        validationResult = ok
                                    }
                                },
                                enabled = !validating,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (validating) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Filled.VerifiedUser, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Validate")
                                }
                            }
                        }

                        AnimatedVisibility(visible = validationResult != null) {
                            validationResult?.let { ok ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (ok) Green100 else Color(0xFFFFE4E6)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            if (ok) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                                            null,
                                            tint = if (ok) Green500 else RecordingRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            if (ok) "API key is valid and working" else "API key validation failed — check the key and try again",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (ok) Color(0xFF065F46) else RecordingRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // --- Meeting Defaults Section ---
                SettingsSection(title = "Meeting Defaults") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ExposedDropdownMenuBox(
                            expanded = platformExpanded,
                            onExpandedChange = { platformExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = platform,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Default Platform") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = platformExpanded) },
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = platformExpanded,
                                onDismissRequest = { platformExpanded = false }
                            ) {
                                platforms.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text(p) },
                                        onClick = {
                                            platform = p
                                            vm.prefs.defaultMeetingPlatform = p
                                            platformExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = participants,
                            onValueChange = {
                                participants = it
                                vm.prefs.participantNames = it
                            },
                            label = { Text("Default Participant Names") },
                            placeholder = { Text("e.g. Rahul, Priya, Amit") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            supportingText = { Text("Separate names with commas", style = MaterialTheme.typography.bodySmall, color = Gray500) }
                        )
                    }
                }

                // --- Behaviour Section ---
                SettingsSection(title = "Behaviour") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-generate MOM", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = Gray900)
                            Text("Automatically generate MOM when recording stops", style = MaterialTheme.typography.bodySmall, color = Gray500)
                        }
                        Switch(
                            checked = autoGenerate,
                            onCheckedChange = {
                                autoGenerate = it
                                vm.prefs.autoGenerateMom = it
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Indigo800, checkedTrackColor = Indigo100)
                        )
                    }
                }

                // --- About Section ---
                SettingsSection(title = "About") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AboutRow(label = "App Version", value = "2.0.0")
                        AboutRow(label = "AI Model", value = "Claude Haiku (claude-haiku-4-5)")
                        AboutRow(label = "STT Engine", value = "Google Speech Recognition (Free)")
                        HorizontalDivider(color = Gray200)
                        Text(
                            "Minutes uses on-device speech recognition for transcription and Claude AI for intelligent MOM generation. All data is stored locally on your device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray500
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Indigo800)
            HorizontalDivider(color = Gray200)
            content()
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Gray500)
        Text(value, style = MaterialTheme.typography.bodySmall, color = Gray700, fontWeight = FontWeight.Medium)
    }
}
