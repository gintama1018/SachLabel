package com.sachlabel.app.data.model

/**
 * A matched marketing claim detected on the front label.
 *
 * @param rawText       exact text as extracted from OCR
 * @param patternKey    which rule pattern this matched (e.g. "no_added_sugar")
 * @param prominenceScore  how prominent this claim appears on the front (from OcrRegion)
 */
data class Claim(
    val rawText: String,
    val patternKey: String,
    val prominenceScore: Float = 0f
)
