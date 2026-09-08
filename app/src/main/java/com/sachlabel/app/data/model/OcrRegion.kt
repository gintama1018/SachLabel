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
    val centerY: Int = 0
) {
    /**
     * Prominence score for claim detection.
     * Combines bounding-box area with vertical position bias (higher on page = more prominent).
     * This is a heuristic — not a guarantee — per architecture.md §3.2.
     */
    val prominenceScore: Float get() {
        val area = (boundingHeight * boundingWidth).toFloat()
        // Bias toward top of image (lower centerY = higher on screen)
        val positionBonus = if (centerY < 500) 1.3f else 1.0f
        return area * positionBonus
    }
}

enum class RegionLocation {
    TOP_CENTER, TOP_LEFT, TOP_RIGHT,
    MIDDLE,
    BOTTOM_LEFT, BOTTOM_RIGHT,
    FINE_PRINT,
    OTHER
}
