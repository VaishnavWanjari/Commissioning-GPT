package com.collagex.app.data

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.collagex.app.collage.CarouselSplitter
import com.collagex.app.collage.CollageLayoutEngine
import com.collagex.app.collage.ColorGradingPresets
import com.collagex.app.collage.OverlayRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EditorState(
    val pickedPhotos: List<PickedPhoto> = emptyList(),
    val decodedPhotos: List<Bitmap> = emptyList(),
    val styleId: String = "minimal",
    val colorPresetId: String = "original",
    val overlays: List<CanvasOverlay> = emptyList(),
    val baseCollage: Bitmap? = null,
    val finalComposite: Bitmap? = null,
    val carouselTiles: List<Bitmap> = emptyList(),
    val mood: CaptionMood? = null,
    val isComposing: Boolean = false,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state

    private var nextOverlayId = 1L

    fun setPickedPhotos(photos: List<PickedPhoto>) {
        _state.update { it.copy(pickedPhotos = photos) }
        decodePhotos(photos)
    }

    private fun decodePhotos(photos: List<PickedPhoto>) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val bitmaps = withContext(Dispatchers.IO) {
                photos.mapNotNull { photo ->
                    runCatching { downsample(context, photo.uri, 1200) }.getOrNull()
                }
            }
            _state.update { it.copy(decodedPhotos = bitmaps) }
            val suggested = CollageLayoutEngine.suggestStyle(photos)
            setStyle(suggested.id)
        }
    }

    private fun downsample(context: android.content.Context, uri: Uri, maxDim: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while ((bounds.outWidth / sample) > maxDim || (bounds.outHeight / sample) > maxDim) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }

    fun setStyle(styleId: String) {
        _state.update { it.copy(styleId = styleId) }
        recompose()
    }

    fun setColorPreset(presetId: String) {
        _state.update { it.copy(colorPresetId = presetId) }
        applyGrade()
    }

    fun setMood(mood: CaptionMood) {
        _state.update { it.copy(mood = mood) }
    }

    fun addTextOverlay(text: String, fontId: String) {
        val overlay = CanvasOverlay(id = nextOverlayId++, type = OverlayType.TEXT, content = text, fontId = fontId)
        _state.update { it.copy(overlays = it.overlays + overlay) }
    }

    fun addStickerOverlay(emoji: String) {
        val overlay = CanvasOverlay(id = nextOverlayId++, type = OverlayType.STICKER, content = emoji)
        _state.update { it.copy(overlays = it.overlays + overlay) }
    }

    fun updateOverlay(updated: CanvasOverlay) {
        _state.update { s -> s.copy(overlays = s.overlays.map { if (it.id == updated.id) updated else it }) }
    }

    fun removeOverlay(id: Long) {
        _state.update { s -> s.copy(overlays = s.overlays.filterNot { it.id == id }) }
    }

    private fun recompose() {
        val current = _state.value
        if (current.decodedPhotos.isEmpty()) return
        _state.update { it.copy(isComposing = true) }
        viewModelScope.launch {
            val style = StyleCatalog.byId(current.styleId)
            val bitmap = withContext(Dispatchers.Default) {
                CollageLayoutEngine.compose(current.decodedPhotos, style)
            }
            _state.update { it.copy(baseCollage = bitmap, isComposing = false) }
            applyGrade()
        }
    }

    private fun applyGrade() {
        val base = _state.value.baseCollage ?: return
        viewModelScope.launch {
            val graded = withContext(Dispatchers.Default) {
                val preset = ColorGradingPresets.byId(_state.value.colorPresetId)
                val out = Bitmap.createBitmap(base.width, base.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(out)
                val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(ColorMatrix(preset.matrix)) }
                canvas.drawBitmap(base, 0f, 0f, paint)
                out
            }
            _state.update { it.copy(finalComposite = graded) }
        }
    }

    /** Graded composite + baked-in text/sticker overlays — the actual exportable artwork. */
    fun flattenForExport(): Bitmap? {
        val base = _state.value.finalComposite ?: return null
        return OverlayRenderer.flatten(base, _state.value.overlays)
    }

    fun buildCarousel(pieces: Int) {
        val flattened = flattenForExport() ?: return
        viewModelScope.launch {
            val tiles = withContext(Dispatchers.Default) { CarouselSplitter.split(flattened, pieces) }
            _state.update { it.copy(carouselTiles = tiles) }
        }
    }
}
