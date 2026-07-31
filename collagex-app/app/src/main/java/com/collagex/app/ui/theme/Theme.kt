package com.collagex.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CollageXColorScheme = lightColorScheme(
    primary = Ink,
    onPrimary = Paper,
    secondary = InkSoft,
    onSecondary = Paper,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = PaperSoft,
    onSurfaceVariant = InkSoft,
    outline = Line,
    error = Danger,
    onError = Color.White,
)

@Composable
fun CollageXTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CollageXColorScheme,
        typography = CollageXTypography,
        shapes = CollageXShapes,
        content = content,
    )
}
