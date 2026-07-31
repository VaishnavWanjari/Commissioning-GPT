package com.collagex.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.collagex.app.collage.CaptionGenerator
import com.collagex.app.collage.ImageExporter
import com.collagex.app.data.AppViewModel
import com.collagex.app.data.CaptionMood
import com.collagex.app.ui.components.PrimaryButton
import com.collagex.app.ui.components.SecondaryButton
import kotlinx.coroutines.launch

@Composable
fun ExportScreen(viewModel: AppViewModel, onDone: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (state.mood == null) viewModel.setMood(CaptionMood.TRAVEL)
    }

    val exportable = state.finalComposite ?: state.baseCollage

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp, start = 8.dp, end = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDone) { Icon(Icons.Filled.ArrowBack, contentDescription = "Home") }
                Text("Export & share", style = MaterialTheme.typography.headlineMedium)
            }
        },
        bottomBar = {
            Column(modifier = Modifier.padding(24.dp)) {
                PrimaryButton(
                    text = "Share",
                    modifier = Modifier.padding(bottom = 12.dp),
                    onClick = {
                        val bmp = viewModel.flattenForExport() ?: exportable ?: return@PrimaryButton
                        scope.launch {
                            val uri = ImageExporter.cacheForSharing(context, bmp, "collagex_${System.currentTimeMillis()}.png")
                            context.startActivity(ImageExporter.shareIntent(context, listOf(uri)))
                        }
                    },
                )
                SecondaryButton(
                    text = "Save to gallery",
                    onClick = {
                        val bmp = viewModel.flattenForExport() ?: exportable ?: return@SecondaryButton
                        scope.launch {
                            ImageExporter.saveToGallery(context, bmp, "collagex_${System.currentTimeMillis()}.png")
                        }
                    },
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            if (exportable != null) {
                Image(
                    bitmap = exportable.asImageBitmap(),
                    contentDescription = "Final artwork",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(20.dp)),
                )
            }

            Text("Caption mood", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 20.dp))
            LazyRow(
                contentPadding = PaddingValues(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(CaptionMood.entries) { mood ->
                    FilterChip(
                        selected = state.mood == mood,
                        onClick = { viewModel.setMood(mood) },
                        label = { Text(mood.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    )
                }
            }

            val mood = state.mood ?: CaptionMood.TRAVEL
            Text("Captions", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            CaptionGenerator.captionsFor(mood).forEach { caption ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { clipboard.setText(AnnotatedString(caption)) },
                ) {
                    Text(caption, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }

            Text("Hashtags", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
            val hashtagLine = CaptionGenerator.hashtagsFor(mood).joinToString(" ")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 0.dp)
                    .clickable { clipboard.setText(AnnotatedString(hashtagLine)) },
            ) {
                Text(hashtagLine, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
