package com.collagex.app.collage

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import com.collagex.app.data.BackgroundSpec
import com.collagex.app.data.PhotoTransform

/**
 * SCRL's headline feature: a freeform canvas where every photo is independently
 * dragged, pinch-scaled and rotated (as opposed to [CollageLayoutEngine]'s fixed
 * algorithmic templates). The interactive editing happens live in Compose via
 * cheap `graphicsLayer` transforms; this object only bakes the final [PhotoTransform]
 * list into a real bitmap once, for export/carousel/preview.
 */
object FreeformComposer {

    fun compose(
        photos: List<Bitmap>,
        transforms: List<PhotoTransform>,
        background: BackgroundSpec,
        canvasSize: Int = 1080,
    ): Bitmap {
        val out = Bitmap.createBitmap(canvasSize, canvasSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        drawBackground(canvas, background, canvasSize)

        photos.forEachIndexed { i, bmp ->
            val t = transforms.getOrElse(i) { PhotoTransform() }
            val cx = t.xFrac * canvasSize
            val cy = t.yFrac * canvasSize
            val baseSize = canvasSize * 0.42f * t.scale
            val photoRect = RectF(cx - baseSize / 2f, cy - baseSize / 2f, cx + baseSize / 2f, cy + baseSize / 2f)
            val pad = baseSize * 0.05f
            val cardRect = RectF(photoRect.left - pad, photoRect.top - pad, photoRect.right + pad, photoRect.bottom + pad)

            canvas.save()
            canvas.rotate(t.rotationDeg, cx, cy)

            val shadow = Paint().apply { color = Color.argb(50, 0, 0, 0) }
            canvas.drawRect(cardRect.left + 4f, cardRect.top + 6f, cardRect.right + 4f, cardRect.bottom + 6f, shadow)

            val cardPaint = Paint().apply { color = Color.WHITE }
            canvas.drawRect(cardRect, cardPaint)
            drawCenterCropped(canvas, bmp, photoRect)

            canvas.restore()
        }
        return out
    }

    private fun drawBackground(canvas: Canvas, background: BackgroundSpec, size: Int) {
        when (background) {
            is BackgroundSpec.Solid -> canvas.drawColor(background.color.toArgbInt())
            is BackgroundSpec.Gradient -> {
                val paint = Paint().apply {
                    shader = LinearGradient(
                        0f, 0f, size.toFloat(), size.toFloat(),
                        background.start.toArgbInt(), background.end.toArgbInt(),
                        Shader.TileMode.CLAMP,
                    )
                }
                canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
            }
        }
    }

    private fun drawCenterCropped(canvas: Canvas, bmp: Bitmap, dest: RectF) {
        val srcRatio = bmp.width.toFloat() / bmp.height.toFloat()
        val destRatio = dest.width() / dest.height()
        val matrix = Matrix()
        val scale: Float
        var dx = 0f
        var dy = 0f
        if (srcRatio > destRatio) {
            scale = dest.height() / bmp.height.toFloat()
            dx = (dest.width() - bmp.width * scale) / 2f
        } else {
            scale = dest.width() / bmp.width.toFloat()
            dy = (dest.height() - bmp.height * scale) / 2f
        }
        matrix.setScale(scale, scale)
        matrix.postTranslate(dest.left + dx, dest.top + dy)
        canvas.save()
        canvas.clipRect(dest)
        canvas.drawBitmap(bmp, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
        canvas.restore()
    }
}

private fun androidx.compose.ui.graphics.Color.toArgbInt(): Int =
    Color.argb((alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())
