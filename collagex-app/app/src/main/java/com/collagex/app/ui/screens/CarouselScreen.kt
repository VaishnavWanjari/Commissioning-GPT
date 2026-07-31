package com.collagex.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.collagex.app.collage.CarouselSplitter
import com.collagex.app.data.AppViewModel
import com.collagex.app.ui.components.PrimaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarouselScreen(viewModel: AppViewModel, onNext: () -> Unit, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    var gridSize by remember { mutableStateOf(3) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp, start = 8.dp, end = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                Text("Carousel & feed preview", style = MaterialTheme.typography.headlineMedium)
            }
        },
        bottomBar = {
            Column {
                TextButton(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("Skip carousel") }
                PrimaryButton(text = "Continue", modifier = Modifier.padding(24.dp), onClick = onNext)
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp)) {
            Text("Split into a carousel", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CarouselSplitter.supportedSplits.forEach { n ->
                    FilterChip(
                        selected = state.carouselTiles.size == n,
                        onClick = { viewModel.buildCarousel(n) },
                        label = { Text("1/$n") },
                    )
                }
            }

            if (state.carouselTiles.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    items(state.carouselTiles) { tile ->
                        Image(
                            bitmap = tile.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(96.dp).clip(RoundedCornerShape(10.dp)),
                        )
                    }
                }
            }

            Text("Feed preview", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(vertical = 12.dp)) {
                listOf(3, 6).forEachIndexed { index, n ->
                    SegmentedButton(
                        selected = gridSize == n,
                        onClick = { gridSize = n },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                    ) {
                        Text("${n}x$n")
                    }
                }
            }

            val previewTiles = state.carouselTiles.ifEmpty { listOfNotNull(state.finalComposite) }
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridSize),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(gridSize * gridSize) { index ->
                    val bmp = previewTiles.getOrNull(index)
                    Box(modifier = Modifier.aspectRatio(1f).background(MaterialTheme.colorScheme.surfaceVariant)) {
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }
}
