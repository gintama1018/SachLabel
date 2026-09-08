package com.sachlabel.app.ocr

import com.sachlabel.app.data.model.OcrRegion
import com.sachlabel.app.data.model.RegionLocation
import com.sachlabel.app.data.model.StructuredLabel

/**
 * Converts raw OCR regions from both photos into a typed [StructuredLabel].
 *
 * Architecture.md §3.3: heuristic segmentation using keyword anchors.
 * "Ingredients:", "Nutritional Information", fine-print size heuristics.
 * Full generalized table parsing is a v2+ problem.
 */
object LabelExtractor {

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
        val nutritionTable = extractNutritionTable(backRegions, rawBackText)
        val finePrint = extractFinePrint(backRegions)

        return StructuredLabel(
            frontClaimsRaw = frontClaimsRaw,
            ingredients = ingredients,
            nutritionTable = nutritionTable,
            finePrint = finePrint,
            rawBackText = rawBackText
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Front label: extract candidate claim phrases
    // ─────────────────────────────────────────────────────────────────────────

    private fun extractFrontClaims(regions: List<OcrRegion>): List<String> {
        // Take all non-trivial text blocks from the front as candidates
        return regions
            .filter { it.text.length in 3..120 }
            .sortedByDescending { it.prominenceScore }
            .map { it.text.trim() }
            .filter { it.isNotBlank() }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Back label: ingredients list
    // ─────────────────────────────────────────────────────────────────────────

    private val INGREDIENT_ANCHORS = listOf(
        "ingredients:", "ingredient:", "ingredients list:", "contains:",
        "made with:", "composition:"
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
            "storage instructions|directions for use|\\*rda)"
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
        "nutrition per", "per 100", "per serving"
    )

    private fun extractNutritionTable(
        regions: List<OcrRegion>,
        rawText: String
    ): Map<String, Double> {
        val result = mutableMapOf<String, Double>()
        val lowerText = rawText.lowercase()

        val hasNutritionSection = NUTRITION_ANCHORS.any { anchor -> lowerText.contains(anchor) }
        if (!hasNutritionSection) return result

        // Regex patterns for common nutrition fields
        val patterns = mapOf(
            StructuredLabel.KEY_ENERGY_KCAL to Regex(
                """(?i)energy[\s:]*(\d+\.?\d*)\s*(?:kcal|kj|cal)"""),
            StructuredLabel.KEY_PROTEIN_G to Regex(
                """(?i)protein[\s:]*(\d+\.?\d*)\s*g"""),
            StructuredLabel.KEY_TOTAL_FAT_G to Regex(
                """(?i)(?:total\s+)?fat[\s:]*(\d+\.?\d*)\s*g"""),
            StructuredLabel.KEY_SUGARS_G to Regex(
                """(?i)(?:total\s+)?sugars?[\s:]*(\d+\.?\d*)\s*g"""),
            StructuredLabel.KEY_CARBS_G to Regex(
                """(?i)(?:total\s+)?carbohydrates?[\s:]*(\d+\.?\d*)\s*g"""),
            StructuredLabel.KEY_SODIUM_MG to Regex(
                """(?i)sodium[\s:]*(\d+\.?\d*)\s*mg"""),
            StructuredLabel.KEY_TRANS_FAT_G to Regex(
                """(?i)trans\s+fat[\s:]*(\d+\.?\d*)\s*g""")
        )

        for ((key, pattern) in patterns) {
            val match = pattern.find(rawText)
            if (match != null) {
                result[key] = match.groupValues[1].toDoubleOrNull() ?: continue
            }
        }

        return result
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Back label: fine print
    // ─────────────────────────────────────────────────────────────────────────

    private fun extractFinePrint(regions: List<OcrRegion>): List<String> {
        return regions
            .filter { region ->
                // Fine print: small bounding boxes or explicitly tagged FINE_PRINT
                region.location == RegionLocation.FINE_PRINT ||
                    (region.boundingHeight in 1..30 && region.text.length > 20)
            }
            .map { it.text.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }
}
