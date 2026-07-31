package com.collagex.app.collage

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

/** Draws a decorative border around the whole finished collage — the outermost visual layer. */
object FrameOverlay {

    fun apply(bitmap: Bitmap, frameId: String): Bitmap {
        if (frameId == "none") return bitmap
        val out = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)
        val size = out.width.toFloat()

        when (frameId) {
            "thin_white" -> strokeBorder(canvas, size, Color.WHITE, size * 0.015f)
            "thick_white" -> strokeBorder(canvas, size, Color.WHITE, size * 0.06f)
            "rounded_black" -> {
                val stroke = size * 0.03f
                val paint = Paint().apply {
                    color = Color.BLACK
                    style = Paint.Style.STROKE
                    strokeWidth = stroke
                }
                val inset = stroke / 2f
                canvas.drawRoundRect(
                    RectF(inset, inset, size - inset, size - inset),
                    size * 0.06f, size * 0.06f, paint,
                )
            }
        }
        return out
    }

    private fun strokeBorder(canvas: Canvas, size: Float, color: Int, stroke: Float) {
        val paint = Paint().apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = stroke
        }
        val inset = stroke / 2f
        canvas.drawRect(inset, inset, size - inset, size - inset, paint)
    }
}
