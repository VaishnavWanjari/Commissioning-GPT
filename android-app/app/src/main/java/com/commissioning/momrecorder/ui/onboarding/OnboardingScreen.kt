package com.commissioning.momrecorder.ui.onboarding

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.*
import com.commissioning.momrecorder.ui.theme.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(0) }

    val micPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val notifPermission = if (android.os.Build.VERSION.SDK_INT >= 33)
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS) else null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                slideInHorizontally { it } + fadeIn() togetherWith
                        slideOutHorizontally { -it } + fadeOut()
            },
            label = "onboarding_step"
        ) { currentStep ->
            when (currentStep) {
                0 -> WelcomeStep { step = 1 }
                1 -> PermissionStep(
                    icon = { Icon(Icons.Filled.Mic, null, Modifier.size(48.dp), tint = Indigo800) },
                    title = "Microphone Access",
                    description = "Minutes needs your microphone to capture meeting speech and generate transcripts in Hindi and English.",
                    buttonLabel = if (micPermission.status.isGranted) "Continue →" else "Grant Microphone",
                    isPermanentlyDenied = micPermission.status.isPermanentlyDenied(),
                    onAction = {
                        if (micPermission.status.isGranted) step = 2
                        else if (micPermission.status.isPermanentlyDenied()) {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    .apply { data = Uri.fromParts("package", context.packageName, null) }
                            )
                        } else micPermission.launchPermissionRequest()
                    }
                )
                2 -> {
                    if (notifPermission == null || notifPermission.status.isGranted) {
                        onFinished()
                    }
                    PermissionStep(
                        icon = { Icon(Icons.Filled.NotificationsActive, null, Modifier.size(48.dp), tint = Indigo800) },
                        title = "Notifications",
                        description = "Minutes shows a notification while recording so you can control it without switching apps.",
                        buttonLabel = if (notifPermission?.status?.isGranted == true) "Get Started →" else "Allow Notifications",
                        isPermanentlyDenied = notifPermission?.status?.isPermanentlyDenied() == true,
                        onAction = {
                            if (notifPermission?.status?.isGranted == true) {
                                onFinished()
                            } else if (notifPermission?.status?.isPermanentlyDenied() == true) {
                                context.startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                        .apply { data = Uri.fromParts("package", context.packageName, null) }
                                )
                            } else {
                                notifPermission?.launchPermissionRequest()
                                onFinished()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Indigo100),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Task, null, Modifier.size(64.dp), tint = Indigo800)
        }

        Text(
            text = "Minutes",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = Indigo800
        )

        Text(
            text = "AI-Powered Meeting Minutes",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        listOf(
            "🎙 Records Hindi & English speech",
            "🤖 Generates professional MOMs with AI",
            "✅ Tracks action items across all meetings"
        ).forEach { feature ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Indigo50,
                tonalElevation = 0.dp
            ) {
                Text(
                    text = feature,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Indigo900
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Get Started", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PermissionStep(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    buttonLabel: String,
    isPermanentlyDenied: Boolean,
    onAction: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Indigo100),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (isPermanentlyDenied) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    text = "Permission permanently denied. Tap below to open Settings and grant it manually.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onAction,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(buttonLabel, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
