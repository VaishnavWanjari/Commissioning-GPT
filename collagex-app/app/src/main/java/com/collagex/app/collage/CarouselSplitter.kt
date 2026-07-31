package com.collagex.app.collage

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF

/**
 * Splits one wide composite into N equal square tiles that, posted left-to-right in
 * order, reassemble into the original image across an Instagram carousel/feed row.
 */
object CarouselSplitter {

    val supportedSplits = listOf(3, 4, 5, 6, 10)

    fun split(source: Bitmap, pieces: Int): List<Bitmap> {
        require(pieces > 1) { "Need at least 2 pieces to build a carousel" }
        val tile = source.height
        val targetW = tile * pieces
        val cropped = centerCropToSize(source, targetW, tile)
        return (0 until pieces).map { i ->
            Bitmap.createBitmap(cropped, i * tile, 0, tile, tile)
        }
    }

    private fun centerCropToSize(source: Bitmap, targetW: Int, targetH: Int): Bitmap {
        val out = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val srcRatio = source.width.toFloat() / source.height.toFloat()
        val destRatio = targetW.toFloat() / targetH.toFloat()
        val matrix = Matrix()
        val scale: Float
        var dx = 0f
        var dy = 0f
        if (srcRatio > destRatio) {
            scale = targetH.toFloat() / source.height.toFloat()
            dx = (targetW - source.width * scale) / 2f
        } else {
            scale = targetW.toFloat() / source.width.toFloat()
            dy = (targetH - source.height * scale) / 2f
        }
        matrix.setScale(scale, scale)
        matrix.postTranslate(dx, dy)
        canvas.save()
        canvas.clipRect(RectF(0f, 0f, targetW.toFloat(), targetH.toFloat()))
        canvas.drawBitmap(source, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
        canvas.restore()
        return out
    }
}
