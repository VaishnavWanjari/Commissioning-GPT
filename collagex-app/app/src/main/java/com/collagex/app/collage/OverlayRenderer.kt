package com.collagex.app.collage

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import com.collagex.app.data.CanvasOverlay
import com.collagex.app.data.OverlayType

/** Bakes text + sticker overlays onto the graded composite for a final, shareable bitmap. */
object OverlayRenderer {

    fun flatten(base: Bitmap, overlays: List<CanvasOverlay>): Bitmap {
        val out = base.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)
        val size = base.width.coerceAtLeast(base.height).toFloat()

        overlays.forEach { overlay ->
            val cx = overlay.xFrac * base.width
            val cy = overlay.yFrac * base.height
            canvas.save()
            canvas.rotate(overlay.rotationDeg, cx, cy)
            when (overlay.type) {
                OverlayType.TEXT -> drawText(canvas, overlay, cx, cy, size)
                OverlayType.STICKER -> drawSticker(canvas, overlay, cx, cy, size)
            }
            canvas.restore()
        }
        return out
    }

    private fun drawText(canvas: Canvas, overlay: CanvasOverlay, cx: Float, cy: Float, refSize: Float) {
        val font = FontCatalog.byId(overlay.fontId)
        val typeface = androidTypefaceFor(font)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = overlay.colorArgb.toInt()
            textSize = refSize * 0.06f * overlay.scale
            this.typeface = typeface
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(overlay.content, cx, cy, paint)
    }

    private fun drawSticker(canvas: Canvas, overlay: CanvasOverlay, cx: Float, cy: Float, refSize: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = refSize * 0.1f * overlay.scale
            textAlign = Paint.Align.CENTER
        }
        val metrics = paint.fontMetrics
        canvas.drawText(overlay.content, cx, cy - (metrics.ascent + metrics.descent) / 2f, paint)
    }

    private fun androidTypefaceFor(font: CollageFont): Typeface {
        val family = when (font.family) {
            androidx.compose.ui.text.font.FontFamily.Serif -> Typeface.SERIF
            androidx.compose.ui.text.font.FontFamily.Monospace -> Typeface.MONOSPACE
            androidx.compose.ui.text.font.FontFamily.Cursive -> Typeface.create("cursive", Typeface.NORMAL)
            else -> Typeface.SANS_SERIF
        }
        var style = Typeface.NORMAL
        val bold = font.weight.weight >= 600
        if (bold && font.italic) style = Typeface.BOLD_ITALIC
        else if (bold) style = Typeface.BOLD
        else if (font.italic) style = Typeface.ITALIC
        return Typeface.create(family, style)
    }
}
