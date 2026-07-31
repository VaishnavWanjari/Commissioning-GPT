package com.collagex.app.collage

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import com.collagex.app.data.CollageStyle
import com.collagex.app.data.FrameStyle
import com.collagex.app.data.LayoutAlgorithm
import com.collagex.app.data.PickedPhoto
import com.collagex.app.data.StyleCatalog
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Deterministic, on-device "AI Smart Collage" engine.
 *
 * There is no trained model here (see README roadmap) — instead, each [CollageStyle]
 * maps to a real geometric placement algorithm (grid / film-strip / masonry / polaroid
 * scatter / magazine split / single hero) that adapts to however many photos and
 * whatever aspect ratios the user picked, so no two photo sets render identically.
 * [suggestStyle] picks a sensible style automatically so the user never has to place
 * anything by hand — that's the "AI" promise this v1 actually keeps.
 */
object CollageLayoutEngine {

    /** Heuristic auto-style pick: no manual placement required. */
    fun suggestStyle(photos: List<PickedPhoto>): CollageStyle {
        if (photos.isEmpty()) return StyleCatalog.byId("minimal")
        if (photos.size == 1) return StyleCatalog.byId("minimal")

        val avgRatio = photos.map { it.aspectRatio }.average().toFloat()
        val varRatio = photos.map { (it.aspectRatio - avgRatio) * (it.aspectRatio - avgRatio) }.average()

        return when {
            photos.size <= 3 -> StyleCatalog.byId("magazine")
            photos.size in 4..6 && varRatio < 0.05 -> StyleCatalog.byId("film_strip")
            varRatio > 0.15 -> StyleCatalog.byId("pinterest")
            photos.size >= 10 -> StyleCatalog.byId("scrapbook")
            else -> StyleCatalog.byId("polaroid")
        }
    }

    fun compose(
        photos: List<Bitmap>,
        style: CollageStyle,
        canvasSize: Int = 1080,
    ): Bitmap {
        require(photos.isNotEmpty()) { "Need at least one photo to compose a collage" }
        val result = Bitmap.createBitmap(canvasSize, canvasSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(style.backgroundColor.toArgbInt())

        when (style.layout) {
            LayoutAlgorithm.GRID -> drawGrid(canvas, photos, style, canvasSize)
            LayoutAlgorithm.FILM_STRIP -> drawFilmStrip(canvas, photos, style, canvasSize)
            LayoutAlgorithm.POLAROID_SCATTER -> drawPolaroidScatter(canvas, photos, style, canvasSize)
            LayoutAlgorithm.MAGAZINE_SPLIT -> drawMagazineSplit(canvas, photos, style, canvasSize)
            LayoutAlgorithm.MASONRY -> drawMasonry(canvas, photos, style, canvasSize)
            LayoutAlgorithm.SINGLE_HERO -> drawSingleHero(canvas, photos, style, canvasSize)
        }
        return result
    }

    // ---- layout algorithms -------------------------------------------------

    private fun drawGrid(canvas: Canvas, photos: List<Bitmap>, style: CollageStyle, size: Int) {
        val n = photos.size
        val cols = ceil(sqrt(n.toDouble())).toInt().coerceAtLeast(1)
        val rows = ceil(n / cols.toDouble()).toInt().coerceAtLeast(1)
        val spacing = style.spacingDp
        val cellW = (size - spacing * (cols + 1)) / cols
        val cellH = (size - spacing * (rows + 1)) / rows
        photos.forEachIndexed { i, bmp ->
            val row = i / cols
            val col = i % cols
            val left = spacing + col * (cellW + spacing)
            val top = spacing + row * (cellH + spacing)
            val rect = RectF(left, top, left + cellW, top + cellH)
            drawCenterCropped(canvas, bmp, rect)
            drawFrame(canvas, rect, style)
        }
    }

    private fun drawFilmStrip(canvas: Canvas, photos: List<Bitmap>, style: CollageStyle, size: Int) {
        val n = photos.size
        val margin = size * 0.08f
        val stripW = size - margin * 2
        val gap = 6f
        val cellH = (size - gap * (n + 1)) / n
        val sprocket = Paint().apply { color = Color.WHITE }
        photos.forEachIndexed { i, bmp ->
            val top = gap + i * (cellH + gap)
            val rect = RectF(margin, top, margin + stripW, top + cellH)
            drawCenterCropped(canvas, bmp, rect)
            // sprocket holes down both edges of the strip
            var y = top + 10f
            while (y < top + cellH - 10f) {
                canvas.drawRoundRect(RectF(margin * 0.35f, y, margin * 0.65f, y + 14f), 3f, 3f, sprocket)
                canvas.drawRoundRect(RectF(size - margin * 0.65f, y, size - margin * 0.35f, y + 14f), 3f, 3f, sprocket)
                y += 30f
            }
        }
    }

    private fun drawPolaroidScatter(canvas: Canvas, photos: List<Bitmap>, style: CollageStyle, size: Int) {
        val n = photos.size
        val cols = ceil(sqrt(n.toDouble())).toInt().coerceAtLeast(1)
        val rows = ceil(n / cols.toDouble()).toInt().coerceAtLeast(1)
        val cellW = size.toFloat() / cols
        val cellH = size.toFloat() / rows
        val cardSize = min(cellW, cellH) * 0.78f
        val bottomStrip = cardSize * 0.18f

        photos.forEachIndexed { i, bmp ->
            val rnd = Random(style.id.hashCode() * 31L + i)
            val row = i / cols
            val col = i % cols
            val cx = cellW * col + cellW / 2f
            val cy = cellH * row + cellH / 2f
            val angle = (rnd.nextFloat() * 2f - 1f) * style.rotationJitterDeg

            canvas.save()
            canvas.rotate(angle, cx, cy)

            val photoRect = RectF(cx - cardSize / 2f, cy - cardSize / 2f, cx + cardSize / 2f, cy + cardSize / 2f - bottomStrip)
            val cardRect = RectF(cx - cardSize / 2f, cy - cardSize / 2f, cx + cardSize / 2f, cy + cardSize / 2f)

            val shadow = Paint().apply { color = Color.argb(60, 0, 0, 0) }
            canvas.drawRect(cardRect.left + 6f, cardRect.top + 8f, cardRect.right + 6f, cardRect.bottom + 8f, shadow)

            val cardPaint = Paint().apply { color = Color.WHITE }
            canvas.drawRect(cardRect, cardPaint)
            drawCenterCropped(canvas, bmp, photoRect)

            if (style.frame == FrameStyle.WASHI_TAPE) {
                val tape = Paint().apply { color = style.accentColor.toArgbInt(); alpha = 190 }
                canvas.drawRect(cx - cardSize * 0.22f, cardRect.top - 14f, cx + cardSize * 0.22f, cardRect.top + 14f, tape)
            }
            canvas.restore()
        }
    }

    private fun drawMagazineSplit(canvas: Canvas, photos: List<Bitmap>, style: CollageStyle, size: Int) {
        val spacing = max(style.spacingDp, 4f)
        if (photos.size == 1) {
            drawCenterCropped(canvas, photos[0], RectF(0f, 0f, size.toFloat(), size.toFloat()))
            return
        }
        val heroW = size * 0.62f
        val heroRect = RectF(0f, 0f, heroW - spacing / 2f, size.toFloat())
        drawCenterCropped(canvas, photos[0], heroRect)
        drawFrame(canvas, heroRect, style)

        val rest = photos.drop(1)
        val colX = heroW + spacing / 2f
        val colW = size - colX
        val cellH = (size - spacing * (rest.size + 1)) / rest.size
        rest.forEachIndexed { i, bmp ->
            val top = spacing + i * (cellH + spacing)
            val rect = RectF(colX, top, size.toFloat(), top + cellH)
            drawCenterCropped(canvas, bmp, rect)
            drawFrame(canvas, rect, style)
        }
    }

    /** Real column-balanced masonry: each photo goes into whichever column is currently shortest. */
    private fun drawMasonry(canvas: Canvas, photos: List<Bitmap>, style: CollageStyle, size: Int) {
        val spacing = style.spacingDp
        val cols = if (photos.size >= 6) 3 else 2
        val colW = (size - spacing * (cols + 1)) / cols
        val colHeights = FloatArray(cols) { spacing }

        photos.forEach { bmp ->
            val targetCol = (0 until cols).minByOrNull { colHeights[it] } ?: 0
            val ratio = bmp.width.toFloat() / bmp.height.toFloat()
            val h = (colW / ratio).coerceIn(colW * 0.6f, colW * 1.6f)
            val left = spacing + targetCol * (colW + spacing)
            val top = colHeights[targetCol]
            val rect = RectF(left, top, left + colW, top + h)
            drawCenterCropped(canvas, bmp, rect)
            drawFrame(canvas, rect, style)
            colHeights[targetCol] = top + h + spacing
        }
    }

    private fun drawSingleHero(canvas: Canvas, photos: List<Bitmap>, style: CollageStyle, size: Int) {
        val margin = size * 0.05f
        val rect = RectF(margin, margin, size - margin, size - margin)
        drawCenterCropped(canvas, photos.first(), rect)
    }

    // ---- shared helpers -----------------------------------------------------

    private fun drawFrame(canvas: Canvas, rect: RectF, style: CollageStyle) {
        if (style.frame != FrameStyle.THIN_BORDER) return
        val paint = Paint().apply {
            color = Color.WHITE
            this.style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRect(rect, paint)
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
