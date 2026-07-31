package com.collagex.app.collage

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

data class CollageFont(
    val id: String,
    val displayName: String,
    val family: FontFamily,
    val weight: FontWeight,
    val italic: Boolean = false,
    val letterSpacingEm: Float = 0f,
)

/**
 * v1 ships style *pairings* of the four platform type families (no bundled TTFs yet —
 * see README roadmap for real display/script font packs, incl. Japanese/Korean/Arabic).
 */
object FontCatalog {
    val fonts: List<CollageFont> = listOf(
        CollageFont("system_sans", "Minimal", FontFamily.SansSerif, FontWeight.Normal),
        CollageFont("luxury_serif", "Luxury", FontFamily.Serif, FontWeight.Light, letterSpacingEm = 0.04f),
        CollageFont("vintage_serif", "Vintage", FontFamily.Serif, FontWeight.Medium, italic = true),
        CollageFont("typewriter", "Typewriter", FontFamily.Monospace, FontWeight.Normal),
        CollageFont("handwriting", "Handwriting", FontFamily.Cursive, FontWeight.Normal),
        CollageFont("neon", "Neon", FontFamily.SansSerif, FontWeight.Black, letterSpacingEm = 0.02f),
        CollageFont("brush", "Brush", FontFamily.Cursive, FontWeight.Bold, italic = true),
        CollageFont("editorial", "Editorial", FontFamily.Serif, FontWeight.Normal, italic = true),
        CollageFont("fashion", "Fashion", FontFamily.SansSerif, FontWeight.Bold, letterSpacingEm = 0.12f),
    )

    fun byId(id: String): CollageFont = fonts.firstOrNull { it.id == id } ?: fonts.first()
}
