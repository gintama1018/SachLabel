package com.sachlabel.app.data.model

/**
 * One region of text extracted from a product photo.
 * ML Kit returns bounding boxes; we record approximate location + text.
 *
 * @param location  approximate position on the package (top-center, middle, fine_print, etc.)
 * @param text      exact transcribed text, as-printed (no correction)
 * @param boundingHeight  pixel height of the bounding box — used for prominence scoring
 * @param boundingWidth   pixel width of the bounding box
 * @param centerY   vertical center of the bounding box in image coordinates
 */
data class OcrRegion(
    val location: RegionLocation,
    val text: String,
    val boundingHeight: Int = 0,
    val boundingWidth: Int = 0,
    val centerY: Int = 0,
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val isLineLevel: Boolean = false
) {
    /** Normalized height relative to total image height (0.0 to 1.0). */
    val relativeHeight: Float
        get() = if (imageHeight > 0) boundingHeight.toFloat() / imageHeight else 0f

    /** Normalized vertical center coordinate (0.0 = top, 1.0 = bottom). */
    val relativeCenterY: Float
        get() = if (imageHeight > 0) centerY.toFloat() / imageHeight
                else if (centerY < 500) 0.35f else 0.75f

    /**
     * Prominence score for claim detection.
     * Combines normalized font/box height with vertical placement bias.
     * Claims on packaging typically appear prominently in the upper 15%–65% of the front panel.
     */
    val prominenceScore: Float get() {
        val relH = relativeHeight
        val relY = relativeCenterY

        val positionBonus = when {
            relY in 0.12f..0.65f -> 1.4f  // Core front hero area
            relY < 0.12f -> 1.1f          // Top brand area
            relY > 0.82f -> 0.6f          // Footnote / fine print area
            else -> 0.9f
        }

        val baseSize = if (relH > 0f) relH * 1000f else (boundingHeight.toFloat())
        return baseSize * positionBonus
    }
}

enum class RegionLocation {
    TOP_CENTER, TOP_LEFT, TOP_RIGHT,
    MIDDLE,
    BOTTOM_LEFT, BOTTOM_RIGHT,
    FINE_PRINT,
    OTHER
}
