package com.collagex.app.data

import android.net.Uri
import androidx.compose.ui.graphics.Color

/** One photo the user picked from the gallery, with its natural aspect ratio (width / height). */
data class PickedPhoto(
    val uri: Uri,
    val aspectRatio: Float = 1f,
)

/** The underlying placement algorithm a [CollageStyle] is rendered with. */
enum class LayoutAlgorithm {
    GRID,
    FILM_STRIP,
    POLAROID_SCATTER,
    MAGAZINE_SPLIT,
    MASONRY,
    SINGLE_HERO,
}

/** How each photo tile is framed/decorated. */
enum class FrameStyle {
    NONE,
    THIN_BORDER,
    POLAROID,
    FILM_SPROCKET,
    WASHI_TAPE,
}

enum class StyleCategory { AESTHETIC, OCCASION }

data class CollageStyle(
    val id: String,
    val displayName: String,
    val category: StyleCategory,
    val layout: LayoutAlgorithm,
    val frame: FrameStyle,
    val backgroundColor: Color,
    val spacingDp: Float,
    val rotationJitterDeg: Float,
    val accentColor: Color,
)

object StyleCatalog {
    val styles: List<CollageStyle> = listOf(
        CollageStyle("film_strip", "Film Strip", StyleCategory.AESTHETIC, LayoutAlgorithm.FILM_STRIP, FrameStyle.FILM_SPROCKET, Color(0xFF0D0D0D), 4f, 0f, Color(0xFFFFFFFF)),
        CollageStyle("polaroid", "Polaroid", StyleCategory.AESTHETIC, LayoutAlgorithm.POLAROID_SCATTER, FrameStyle.POLAROID, Color(0xFFF6F6F7), 10f, 6f, Color(0xFF111111)),
        CollageStyle("magazine", "Magazine", StyleCategory.AESTHETIC, LayoutAlgorithm.MAGAZINE_SPLIT, FrameStyle.THIN_BORDER, Color(0xFFFFFFFF), 6f, 0f, Color(0xFF111111)),
        CollageStyle("scrapbook", "Scrapbook", StyleCategory.AESTHETIC, LayoutAlgorithm.POLAROID_SCATTER, FrameStyle.WASHI_TAPE, Color(0xFFF3ECE0), 8f, 9f, Color(0xFF8A5A33)),
        CollageStyle("pinterest", "Pinterest", StyleCategory.AESTHETIC, LayoutAlgorithm.MASONRY, FrameStyle.THIN_BORDER, Color(0xFFFFFFFF), 8f, 0f, Color(0xFFE60023)),
        CollageStyle("minimal", "Minimal", StyleCategory.AESTHETIC, LayoutAlgorithm.SINGLE_HERO, FrameStyle.NONE, Color(0xFFFFFFFF), 0f, 0f, Color(0xFF111111)),
        CollageStyle("luxury", "Luxury", StyleCategory.AESTHETIC, LayoutAlgorithm.GRID, FrameStyle.THIN_BORDER, Color(0xFF0E0E10), 2f, 0f, Color(0xFFC9A24B)),
        CollageStyle("vintage", "Vintage", StyleCategory.AESTHETIC, LayoutAlgorithm.POLAROID_SCATTER, FrameStyle.POLAROID, Color(0xFFEFE6D8), 10f, 7f, Color(0xFF6B4A2F)),
        CollageStyle("summer", "Summer", StyleCategory.OCCASION, LayoutAlgorithm.MASONRY, FrameStyle.THIN_BORDER, Color(0xFFFFF7E8), 8f, 0f, Color(0xFFFF9F1C)),
        CollageStyle("travel_diary", "Travel Diary", StyleCategory.OCCASION, LayoutAlgorithm.POLAROID_SCATTER, FrameStyle.WASHI_TAPE, Color(0xFFF6F1E7), 8f, 8f, Color(0xFF2E5339)),
        CollageStyle("wedding", "Wedding", StyleCategory.OCCASION, LayoutAlgorithm.MAGAZINE_SPLIT, FrameStyle.THIN_BORDER, Color(0xFFFFFFFF), 6f, 0f, Color(0xFFB98CA6)),
        CollageStyle("birthday", "Birthday", StyleCategory.OCCASION, LayoutAlgorithm.MASONRY, FrameStyle.THIN_BORDER, Color(0xFFFFF0F5), 8f, 0f, Color(0xFFE94F8A)),
        CollageStyle("couple", "Couple", StyleCategory.OCCASION, LayoutAlgorithm.POLAROID_SCATTER, FrameStyle.POLAROID, Color(0xFFFFF5F5), 10f, 5f, Color(0xFFD1495B)),
        CollageStyle("cafe", "Cafe", StyleCategory.OCCASION, LayoutAlgorithm.GRID, FrameStyle.THIN_BORDER, Color(0xFFF1E7DA), 4f, 0f, Color(0xFF6F4E37)),
        CollageStyle("beach", "Beach", StyleCategory.OCCASION, LayoutAlgorithm.MASONRY, FrameStyle.THIN_BORDER, Color(0xFFEAF6FB), 8f, 0f, Color(0xFF1B9AAA)),
    )

    fun byId(id: String): CollageStyle = styles.firstOrNull { it.id == id } ?: styles.first()
}

/** One-tap Instagram-style color grading preset, implemented as a real 4x5 ColorMatrix. */
data class ColorGradingPreset(
    val id: String,
    val displayName: String,
    val matrix: FloatArray,
)

enum class OverlayType { TEXT, STICKER }

data class CanvasOverlay(
    val id: Long,
    val type: OverlayType,
    val content: String,
    val fontId: String = "system_sans",
    val xFrac: Float = 0.5f,
    val yFrac: Float = 0.5f,
    val scale: Float = 1f,
    val rotationDeg: Float = 0f,
    val colorArgb: Long = 0xFF111111,
)

enum class CaptionMood { TRAVEL, BIRTHDAY, GYM, CAFE, LOVE, FRIENDS, GRADUATION }
