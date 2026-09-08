package com.sachlabel.app.data.model

/**
 * Structured extraction from both front and back label OCR regions.
 * Produced by LabelExtractor after OCR runs on both photos.
 *
 * Architecture.md §3.3: heuristic segmentation using keyword anchors.
 * Nothing is invented — null/empty means not found.
 */
data class StructuredLabel(
    /** Candidate marketing claim phrases from the front label. */
    val frontClaimsRaw: List<String> = emptyList(),

    /** Ingredient list items, in printed order. */
    val ingredients: List<String> = emptyList(),

    /** Key-value pairs from the nutrition table (e.g. "sugars_g" → 12.0). */
    val nutritionTable: Map<String, Double> = emptyMap(),

    /** Verbatim raw OCR line/span corresponding to each parsed nutrition key. */
    val nutritionRawLines: Map<String, String> = emptyMap(),

    /** Fine-print / disclaimer sentences (small text, usually at edges/bottom). */
    val finePrint: List<String> = emptyList(),

    /** Raw OCR text from the back label — kept for evidence validation. */
    val rawBackText: String = ""
) {
    companion object {
        /** Nutrition table keys (canonical) */
        const val KEY_SUGARS_G = "sugars_g"
        const val KEY_TOTAL_FAT_G = "total_fat_g"
        const val KEY_PROTEIN_G = "protein_g"
        const val KEY_SODIUM_MG = "sodium_mg"
        const val KEY_TRANS_FAT_G = "trans_fat_g"
        const val KEY_ENERGY_KCAL = "energy_kcal"
        const val KEY_CARBS_G = "carbohydrates_g"
    }
}
