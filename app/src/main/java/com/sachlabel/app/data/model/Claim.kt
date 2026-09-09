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
    val prominenceScore: Float = 0f,
    val matchConfidence: Float = 1.0f
) {
    val category: CanonicalClaimCategory?
        get() = CanonicalClaimCategory.fromId(patternKey)

    /**
     * Composite score combining semantic match confidence and layout prominence.
     * High weight on semantic confidence ensures strong marketing claims
     * are not overridden by larger ordinary product brand text.
     */
    val compositeScore: Float
        get() = (matchConfidence * 150f) + (prominenceScore * 0.4f)
}
