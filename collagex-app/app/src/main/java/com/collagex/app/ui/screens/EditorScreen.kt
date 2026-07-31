package com.collagex.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.collagex.app.collage.ColorGradingPresets
import com.collagex.app.collage.FontCatalog
import com.collagex.app.collage.StickerCatalog
import com.collagex.app.data.AppViewModel
import com.collagex.app.data.CanvasOverlay
import com.collagex.app.data.OverlayType
import com.collagex.app.ui.components.PrimaryButton

private enum class EditorTab { GRADE, TEXT, STICKERS }

@Composable
fun EditorScreen(viewModel: AppViewModel, onNext: () -> Unit, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    var tab by remember { mutableStateOf(EditorTab.GRADE) }
    var showTextDialog by remember { mutableStateOf(false) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp, start = 8.dp, end = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                Text("Edit", style = MaterialTheme.typography.headlineMedium)
            }
        },
        bottomBar = {
            PrimaryButton(text = "Continue", modifier = Modifier.padding(24.dp), onClick = onNext)
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .onSizeChanged { canvasSize = it },
            ) {
                val bitmap = state.finalComposite ?: state.baseCollage
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Collage preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                state.overlays.forEach { overlay ->
                    DraggableOverlay(
                        overlay = overlay,
                        canvasSize = canvasSize,
                        onMove = { updated -> viewModel.updateOverlay(updated) },
                        onRemove = { viewModel.removeOverlay(overlay.id) },
                    )
                }
            }

            TabRow(selectedTabIndex = tab.ordinal) {
                Tab(selected = tab == EditorTab.GRADE, onClick = { tab = EditorTab.GRADE }, text = { Text("Color") })
                Tab(selected = tab == EditorTab.TEXT, onClick = { tab = EditorTab.TEXT }, text = { Text("Text") })
                Tab(selected = tab == EditorTab.STICKERS, onClick = { tab = EditorTab.STICKERS }, text = { Text("Stickers") })
            }

            when (tab) {
                EditorTab.GRADE -> LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(ColorGradingPresets.presets) { preset ->
                        val selected = preset.id == state.colorPresetId
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { viewModel.setColorPreset(preset.id) },
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(
                                        width = if (selected) 3.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        shape = CircleShape,
                                    ),
                            )
                            Text(preset.displayName, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                EditorTab.TEXT -> Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TextButton(onClick = { showTextDialog = true }) { Text("+ Add text") }
                    Text(
                        "Drag to move · tap ✕ to remove",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                EditorTab.STICKERS -> LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(StickerCatalog.stickers) { sticker ->
                        Text(
                            text = sticker,
                            fontSize = 28.sp,
                            modifier = Modifier.clickable { viewModel.addStickerOverlay(sticker) },
                        )
                    }
                }
            }
        }
    }

    if (showTextDialog) {
        AddTextDialog(
            onDismiss = { showTextDialog = false },
            onConfirm = { text, fontId ->
                viewModel.addTextOverlay(text, fontId)
                showTextDialog = false
            },
        )
    }
}

@Composable
private fun AddTextDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var text by remember { mutableStateOf("") }
    var fontId by remember { mutableStateOf(FontCatalog.fonts.first().id) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add text") },
        text = {
            Column {
                OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Caption") })
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FontCatalog.fonts) { font ->
                        val selected = font.id == fontId
                        Text(
                            text = font.displayName,
                            fontStyle = if (font.italic) FontStyle.Italic else FontStyle.Normal,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .clickable { fontId = font.id }
                                .padding(8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onConfirm(text, fontId) }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun DraggableOverlay(
    overlay: CanvasOverlay,
    canvasSize: IntSize,
    onMove: (CanvasOverlay) -> Unit,
    onRemove: () -> Unit,
) {
    val xPx = overlay.xFrac * canvasSize.width
    val yPx = overlay.yFrac * canvasSize.height
    val font = FontCatalog.byId(overlay.fontId)

    Box(
        modifier = Modifier.centeredAt { IntOffset(xPx.toInt(), yPx.toInt()) },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.pointerInput(overlay.id) {
                detectDragGestures { change, drag ->
                    change.consume()
                    if (canvasSize.width == 0 || canvasSize.height == 0) return@detectDragGestures
                    val newX = ((overlay.xFrac * canvasSize.width) + drag.x) / canvasSize.width
                    val newY = ((overlay.yFrac * canvasSize.height) + drag.y) / canvasSize.height
                    onMove(overlay.copy(xFrac = newX.coerceIn(0f, 1f), yFrac = newY.coerceIn(0f, 1f)))
                }
            },
        ) {
            when (overlay.type) {
                OverlayType.TEXT -> Text(
                    text = overlay.content,
                    color = Color(overlay.colorArgb),
                    fontStyle = if (font.italic) FontStyle.Italic else FontStyle.Normal,
                    fontFamily = font.family,
                    fontSize = 20.sp,
                )
                OverlayType.STICKER -> Text(text = overlay.content, fontSize = 32.sp)
            }
        }
        Text(
            text = "✕",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offsetPixels(18, -18)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .clickable { onRemove() }
                .padding(4.dp),
        )
    }
}

private fun Modifier.offsetPixels(x: Int, y: Int): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) { placeable.place(x, y) }
    }

private fun Modifier.centeredAt(offset: () -> IntOffset): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            val off = offset()
            placeable.place(off.x - placeable.width / 2, off.y - placeable.height / 2)
        }
    }
