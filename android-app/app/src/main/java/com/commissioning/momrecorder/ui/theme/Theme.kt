package com.commissioning.momrecorder.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val MinutesColorScheme = lightColorScheme(
    primary = Indigo800,
    onPrimary = White,
    primaryContainer = Indigo100,
    onPrimaryContainer = Indigo900,
    secondary = Indigo500,
    onSecondary = White,
    secondaryContainer = Indigo50,
    onSecondaryContainer = Indigo800,
    background = Surface,
    onBackground = Gray900,
    surface = White,
    onSurface = Gray900,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray500,
    error = RecordingRed,
    onError = White,
    errorContainer = RecordingRedLight,
    outline = Gray200
)

@Composable
fun MinutesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MinutesColorScheme,
        typography = MinutesTypography,
        content = content
    )
}
