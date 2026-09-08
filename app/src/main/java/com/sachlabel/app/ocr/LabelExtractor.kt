package com.sachlabel.app.ocr

import com.sachlabel.app.data.model.OcrRegion
import com.sachlabel.app.data.model.RegionLocation
import com.sachlabel.app.data.model.StructuredLabel

/**
 * Converts raw OCR regions from both photos into a typed [StructuredLabel].
 *
 * Architecture.md §3.3: heuristic segmentation using keyword anchors.
 * "Ingredients:", "Nutritional Information", fine-print size heuristics.
 *
 * Zero Synthetic Evidence Rule: Retains the exact verbatim raw OCR line/span
 * for every parsed field so displayed evidence citations are never fabricated.
 */
object LabelExtractor {

    data class NutritionExtraction(
        val values: Map<String, Double>,
        val rawLines: Map<String, String>
    )

    /**
     * Extract a [StructuredLabel] from front + back OCR regions.
     *
     * @param frontRegions  OCR output from the front photo
     * @param backRegions   OCR output from the back photo
     */
    fun extract(
        frontRegions: List<OcrRegion>,
        backRegions: List<OcrRegion>
    ): StructuredLabel {
        val frontClaimsRaw = extractFrontClaims(frontRegions)
        val rawBackText = backRegions.joinToString("\n") { it.text }

        val ingredients = extractIngredients(backRegions, rawBackText)
        val nutrition = extractNutritionTable(backRegions, rawBackText)
        val finePrint = extractFinePrint(backRegions)

        return StructuredLabel(
            frontClaimsRaw = frontClaimsRaw,
            ingredients = ingredients,
            nutritionTable = nutrition.values,
            nutritionRawLines = nutrition.rawLines,
            finePrint = finePrint,
            rawBackText = rawBackText
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Front label: extract candidate claim phrases
    // ─────────────────────────────────────────────────────────────────────────

    private fun extractFrontClaims(regions: List<OcrRegion>): List<String> {
        return regions
            .filter { it.text.length in 3..140 }
            .sortedByDescending { it.prominenceScore }
            .map { it.text.trim() }
            .filter { it.isNotBlank() }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Back label: ingredients list
    // ─────────────────────────────────────────────────────────────────────────

    private val INGREDIENT_ANCHORS = listOf(
        "ingredients:", "ingredient:", "ingredients list:", "ingredients",
        "contains:", "made with:", "composition:",
        "सामग्री:", "सामग्री", "संघटक:", "संघटक"
    )

    private fun extractIngredients(regions: List<OcrRegion>, rawText: String): List<String> {
        val lowerText = rawText.lowercase()
        val anchorIdx = INGREDIENT_ANCHORS.mapNotNull { anchor ->
            val idx = lowerText.indexOf(anchor)
            if (idx >= 0) idx + anchor.length else null
        }.minOrNull() ?: return emptyList()

        // Grab text from anchor to next section break
        val afterAnchor = rawText.substring(anchorIdx)
        val sectionBreakPattern = Regex(
            "(?i)(nutritional information|nutrition facts|nutritive value|" +
            "net weight|best before|manufactured by|marketed by|" +
            "storage instructions|directions for use|\\*rda|पोषण संबंधी)"
        )
        val endIdx = sectionBreakPattern.find(afterAnchor)?.range?.first ?: afterAnchor.length
        val ingredientBlock = afterAnchor.substring(0, endIdx)

        // Split by comma, semicolon, or newline
        return ingredientBlock
            .split(Regex("[,;\\n]"))
            .map { it.trim().trimEnd('.') }
            .filter { it.length > 1 }
            .take(50)  // Reasonable cap
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Back label: nutrition table
    // ─────────────────────────────────────────────────────────────────────────

    private val NUTRITION_ANCHORS = listOf(
        "nutritional information", "nutrition facts", "nutritive value",
        "nutrition per", "per 100", "per serving", "nutrition values",
        "पोषण संबंधी जानकारी", "पोषण"
    )

    fun extractNutritionTable(
        regions: List<OcrRegion>,
        rawText: String
    ): NutritionExtraction {
        val values = mutableMapOf<String, Double>()
        val rawLines = mutableMapOf<String, String>()
        val lowerText = rawText.lowercase()

        // Check if there is a nutrition table section, or check directly if keywords are present
        val hasNutritionSection = NUTRITION_ANCHORS.any { anchor -> lowerText.contains(anchor) }

        val patterns = listOf(
            Triple(
                StructuredLabel.KEY_ENERGY_KCAL,
                Regex("""(?i)(?:energy|energy value|calories)[\s:]*(\d+\.?\d*)\s*(?:kcal|kj|cal)?"""),
                listOf("energy", "kcal", "calories")
            ),
            Triple(
                StructuredLabel.KEY_PROTEIN_G,
                Regex("""(?i)(?:protein|proteins)[\s:]*(\d+\.?\d*)\s*(?:g|gm)?"""),
                listOf("protein")
            ),
            Triple(
                StructuredLabel.KEY_TOTAL_FAT_G,
                Regex("""(?i)(?:total\s+)?(?:fat|fats|lipid)[\s:]*(\d+\.?\d*)\s*(?:g|gm)?"""),
                listOf("fat")
            ),
            Triple(
                StructuredLabel.KEY_SUGARS_G,
                Regex("""(?i)(?:total\s+|added\s+|of which\s+)?sugars?[\s:]*(\d+\.?\d*)\s*(?:g|gm)?"""),
                listOf("sugar")
            ),
            Triple(
                StructuredLabel.KEY_CARBS_G,
                Regex("""(?i)(?:total\s+)?carbohydrates?[\s:]*(\d+\.?\d*)\s*(?:g|gm)?"""),
                listOf("carbohydrate", "carbs")
            ),
            Triple(
                StructuredLabel.KEY_SODIUM_MG,
                Regex("""(?i)sodium[\s:]*(\d+\.?\d*)\s*mg"""),
                listOf("sodium")
            ),
            Triple(
                StructuredLabel.KEY_TRANS_FAT_G,
                Regex("""(?i)trans\s+fat[\s:]*(\d+\.?\d*)\s*(?:g|gm)?"""),
                listOf("trans fat")
            )
        )

        val rawLinesList = rawText.lines()

        for ((key, pattern, keywords) in patterns) {
            // First check lines containing keywords for highest precision line matching
            var foundLineMatch = false
            for (line in rawLinesList) {
                val lowerLine = line.lowercase()
                if (keywords.any { lowerLine.contains(it) }) {
                    val match = pattern.find(line)
                    if (match != null) {
                        val parsed = match.groupValues[1].toDoubleOrNull()
                        if (parsed != null) {
                            values[key] = parsed
                            // Retain the verbatim raw line from OCR
                            rawLines[key] = line.trim()
                            foundLineMatch = true
                            break
                        }
                    }
                }
            }

            if (!foundLineMatch) {
                // Fallback to full text scan
                val match = pattern.find(rawText)
                if (match != null) {
                    val parsed = match.groupValues[1].toDoubleOrNull()
                    if (parsed != null) {
                        values[key] = parsed
                        rawLines[key] = match.value.trim()
                    }
                }
            }
        }

        return NutritionExtraction(values = values, rawLines = rawLines)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Back label: fine print
    // ─────────────────────────────────────────────────────────────────────────

    private fun extractFinePrint(regions: List<OcrRegion>): List<String> {
        // Fine print is typically small text or explicitly located at bottom
        return regions
            .filter { it.location == RegionLocation.FINE_PRINT || it.location == RegionLocation.BOTTOM_LEFT || it.location == RegionLocation.BOTTOM_RIGHT || it.text.length > 50 }
            .map { it.text.trim() }
            .filter { it.isNotBlank() }
    }
}
