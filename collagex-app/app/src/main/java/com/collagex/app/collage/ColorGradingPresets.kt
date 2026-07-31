package com.collagex.app.collage

import android.graphics.ColorMatrix
import com.collagex.app.data.ColorGradingPreset

/**
 * One-tap Instagram-style grades. Each preset is a genuine 4x5 android ColorMatrix
 * (saturation + contrast + per-channel brightness offset), applied to the whole
 * composite bitmap with [android.graphics.ColorMatrixColorFilter] — no network call,
 * runs fully on-device and instantly.
 */
object ColorGradingPresets {

    private fun grade(
        saturation: Float,
        contrast: Float,
        rOffset: Float = 0f,
        gOffset: Float = 0f,
        bOffset: Float = 0f,
    ): FloatArray {
        val sat = ColorMatrix().apply { setSaturation(saturation) }
        val base = (1 - contrast) * 128f
        val tone = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, base + rOffset,
                0f, contrast, 0f, 0f, base + gOffset,
                0f, 0f, contrast, 0f, base + bOffset,
                0f, 0f, 0f, 1f, 0f,
            )
        )
        sat.postConcat(tone)
        return sat.array
    }

    val presets: List<ColorGradingPreset> = listOf(
        ColorGradingPreset("original", "Original", grade(1f, 1f)),
        ColorGradingPreset("moody", "Moody", grade(saturation = 0.75f, contrast = 1.2f, rOffset = -6f, gOffset = -4f, bOffset = 4f)),
        ColorGradingPreset("brown", "Brown", grade(saturation = 0.7f, contrast = 1.05f, rOffset = 14f, gOffset = 4f, bOffset = -12f)),
        ColorGradingPreset("warm", "Warm", grade(saturation = 1.1f, contrast = 1.05f, rOffset = 12f, gOffset = 4f, bOffset = -10f)),
        ColorGradingPreset("cold", "Cold", grade(saturation = 0.95f, contrast = 1.05f, rOffset = -10f, gOffset = 0f, bOffset = 14f)),
        ColorGradingPreset("vintage", "Vintage", grade(saturation = 0.6f, contrast = 0.9f, rOffset = 10f, gOffset = 4f, bOffset = -8f)),
        ColorGradingPreset("film", "Film", grade(saturation = 0.85f, contrast = 1.1f, rOffset = 6f, gOffset = 2f, bOffset = -4f)),
        ColorGradingPreset("kodak", "Kodak", grade(saturation = 1.15f, contrast = 1.1f, rOffset = 8f, gOffset = -2f, bOffset = -6f)),
        ColorGradingPreset("fuji", "Fuji", grade(saturation = 0.95f, contrast = 1.05f, rOffset = -4f, gOffset = 6f, bOffset = 2f)),
        ColorGradingPreset("leica", "Leica", grade(saturation = 0.9f, contrast = 1.2f, rOffset = 0f, gOffset = 0f, bOffset = 0f)),
        ColorGradingPreset("pastel", "Pastel", grade(saturation = 0.6f, contrast = 0.85f, rOffset = 14f, gOffset = 14f, bOffset = 14f)),
        ColorGradingPreset("clean_girl", "Clean Girl", grade(saturation = 0.9f, contrast = 0.95f, rOffset = 8f, gOffset = 8f, bOffset = 8f)),
        ColorGradingPreset("pinterest", "Pinterest", grade(saturation = 1.05f, contrast = 1.0f, rOffset = 4f, gOffset = -2f, bOffset = 6f)),
        ColorGradingPreset("cinematic", "Cinematic", grade(saturation = 0.8f, contrast = 1.25f, rOffset = 6f, gOffset = -2f, bOffset = 10f)),
    )

    fun byId(id: String): ColorGradingPreset = presets.firstOrNull { it.id == id } ?: presets.first()
}
