package com.sachlabel.app.ocr

import com.sachlabel.app.data.model.OcrRegion
import com.sachlabel.app.data.model.RegionLocation
import com.sachlabel.app.data.model.StructuredLabel

/**
 * Converts raw OCR regions from both photos into a typed [StructuredLabel].
 *
 * Uses [LayoutAnalyzer] for:
 * - Front layout: prominence-weighted candidate claims & multi-line phrase synthesis
 * - Back layout: zone segmentation (ingredients, nutrition facts, statutory/certifications, disclaimers)
 *
 * Zero Synthetic Evidence Rule:
 * Retains the exact verbatim raw OCR line/span for every parsed field so displayed
 * evidence citations are never fabricated.
 */
object LabelExtractor {

    data class NutritionExtraction(
        val values: Map<String, Double>,
        val rawLines: Map<String, String>
    )

    /**
     * Extract a [StructuredLabel] from front + back OCR regions.
     */
    fun extract(
        frontRegions: List<OcrRegion>,
        backRegions: List<OcrRegion>
    ): StructuredLabel {
        val frontAnalysis = LayoutAnalyzer.analyzeFront(frontRegions)
        val frontClaimsRaw = frontAnalysis.candidateClaimsWithScores.map { it.first }

        val rawBackText = backRegions.joinToString("\n") { it.text }
        val backZones = LayoutAnalyzer.analyzeBack(backRegions, rawBackText)

        val ingredients = extractIngredients(backZones.ingredientsBlock.ifBlank { rawBackText })
        val nutrition = extractNutritionTable(backZones.nutritionBlock.ifBlank { rawBackText })
        val finePrint = extractFinePrint(backRegions, backZones.disclaimersBlock)

        return StructuredLabel(
            frontClaimsRaw = frontClaimsRaw,
            ingredients = ingredients,
            nutritionTable = nutrition.values,
            nutritionRawLines = nutrition.rawLines,
            finePrint = finePrint,
            detectedCertifications = backZones.detectedCertificationMarks,
            rawBackText = rawBackText
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Back label: ingredients list (with OCR-tolerant anchors & bracket safety)
    // ─────────────────────────────────────────────────────────────────────────

    private val INGREDIENT_ANCHOR_PATTERNS = listOf(
        Regex("""(?i)(?:i|l|1|\|)ngred[i|l]ents?\s*(?:list)?\s*[:\-]?"""),
        Regex("""(?i)contains\s*[:\-]?"""),
        Regex("""(?i)composition\s*[:\-]?"""),
        Regex("""(?i)made\s+with\s*[:\-]?"""),
        Regex("""सामग्री\s*[:\-]?"""),
        Regex("""संघटक\s*[:\-]?""")
    )

    fun extractIngredients(rawText: String): List<String> {
        if (rawText.isBlank()) return emptyList()

        // Find the earliest anchor match
        var startIdx = -1
        var anchorLength = 0

        for (pattern in INGREDIENT_ANCHOR_PATTERNS) {
            val match = pattern.find(rawText)
            if (match != null) {
                if (startIdx == -1 || match.range.first < startIdx) {
                    startIdx = match.range.first
                    anchorLength = match.value.length
                }
            }
        }

        val textToProcess = if (startIdx >= 0) {
            rawText.substring(startIdx + anchorLength)
        } else {
            // Fallback: If no explicit anchor found but text mentions ingredients, use raw text
            rawText
        }

        // Section break keywords indicating end of ingredients list
        val sectionBreakPattern = Regex(
            "(?i)(?:nutritional\\s+information|nutrition\\s+facts|nutritive\\s+value|" +
            "net\\s+weight|best\\s+before|manufactured\\s+by|marketed\\s+by|" +
            "storage\\s+instructions|directions\\s+for\\s+use|\\*rda|पोषण)"
        )
        val endIdx = sectionBreakPattern.find(textToProcess)?.range?.first ?: textToProcess.length
        val ingredientBlock = textToProcess.substring(0, endIdx).trim()

        return splitIngredientsBracketSafe(ingredientBlock)
    }

    /**
     * Splits ingredient text by commas, semicolons, or newlines, WITHOUT breaking
     * nested parentheses like "Vegetable Oil (Palm Oil, Antioxidant (INS 319))".
     */
    fun splitIngredientsBracketSafe(block: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var parenDepth = 0

        for (char in block) {
            when (char) {
                '(', '[' -> {
                    parenDepth++
                    current.append(char)
                }
                ')', ']' -> {
                    if (parenDepth > 0) parenDepth--
                    current.append(char)
                }
                ',', ';', '\n' -> {
                    if (parenDepth == 0) {
                        val token = current.toString().trim().trimEnd('.')
                        if (token.length > 1) {
                            result.add(token)
                        }
                        current.clear()
                    } else {
                        current.append(char)
                    }
                }
                else -> current.append(char)
            }
        }

        val lastToken = current.toString().trim().trimEnd('.')
        if (lastToken.length > 1) {
            result.add(lastToken)
        }

        return result
            .filter { it.length in 2..150 }
            .take(50)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Back label: nutrition table
    // ─────────────────────────────────────────────────────────────────────────

    private val NUTRITION_PATTERNS = listOf(
        // Added sugars (checked before general sugars)
        Triple(
            StructuredLabel.KEY_ADDED_SUGARS_G,
            Regex("""(?i)(?:added\s+sugars?)[\s:]*<?\s*(\d+\.?\d*)\s*(?:g|gm)?"""),
            listOf("added sugar", "added sugars")
        ),
        // Total Sugars / Sugars
        Triple(
            StructuredLabel.KEY_SUGARS_G,
            Regex("""(?i)(?:total\s+|of which\s+)?sugars?[\s:]*<?\s*(\d+\.?\d*)\s*(?:g|gm)?"""),
            listOf("sugars", "sugar", "total sugar")
        ),
        // Energy / Calories
        Triple(
            StructuredLabel.KEY_ENERGY_KCAL,
            Regex("""(?i)(?:energy|energy\s+value|calories)[\s:]*(\d+\.?\d*)\s*(?:kcal|kj|cal)?"""),
            listOf("energy", "kcal", "calories")
        ),
        // Protein
        Triple(
            StructuredLabel.KEY_PROTEIN_G,
            Regex("""(?i)(?:protein|proteins)[\s:]*(\d+\.?\d*)\s*(?:g|gm)?"""),
            listOf("protein", "proteins")
        ),
        // Trans Fat (handles < 0.1g, 0g, 0.0g)
        Triple(
            StructuredLabel.KEY_TRANS_FAT_G,
            Regex("""(?i)trans\s+fat(?:ty\s+acids?)?[\s:]*<?\s*(\d+\.?\d*)\s*(?:g|gm)?"""),
            listOf("trans fat", "trans fatty")
        ),
        // Total Fat
        Triple(
            StructuredLabel.KEY_TOTAL_FAT_G,
            Regex("""(?i)(?:total\s+)?(?:fat|fats|lipid)[\s:]*(\d+\.?\d*)\s*(?:g|gm)?"""),
            listOf("total fat", "fat", "fats")
        ),
        // Carbohydrates
        Triple(
            StructuredLabel.KEY_CARBS_G,
            Regex("""(?i)(?:total\s+)?carbohydrates?[\s:]*(\d+\.?\d*)\s*(?:g|gm)?"""),
            listOf("carbohydrate", "carbohydrates", "carbs")
        ),
        // Sodium
        Triple(
            StructuredLabel.KEY_SODIUM_MG,
            Regex("""(?i)sodium[\s:]*(\d+\.?\d*)\s*(?:mg)?"""),
            listOf("sodium")
        )
    )

    fun extractNutritionTable(rawText: String): NutritionExtraction {
        val values = mutableMapOf<String, Double>()
        val rawLines = mutableMapOf<String, String>()
        val rawLinesList = rawText.lines()

        for ((key, pattern, keywords) in NUTRITION_PATTERNS) {
            var foundLineMatch = false

            // Step 1: Scan individual lines containing the target keyword
            for (line in rawLinesList) {
                val lowerLine = line.lowercase()
                if (keywords.any { lowerLine.contains(it) }) {
                    // For KEY_SUGARS_G, skip line if it's explicitly "added sugar" (handled by KEY_ADDED_SUGARS_G)
                    if (key == StructuredLabel.KEY_SUGARS_G && lowerLine.contains("added sugar")) {
                        continue
                    }

                    val match = pattern.find(line)
                    if (match != null) {
                        val parsed = match.groupValues[1].toDoubleOrNull()
                        if (parsed != null) {
                            values[key] = parsed
                            rawLines[key] = line.trim()
                            foundLineMatch = true
                            break
                        }
                    }
                }
            }

            // Step 2: Fallback to full-text scan if line-level match wasn't found
            if (!foundLineMatch) {
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
    // Back label: fine print & disclaimers
    // ─────────────────────────────────────────────────────────────────────────

    private fun extractFinePrint(regions: List<OcrRegion>, disclaimersBlock: String): List<String> {
        val results = mutableListOf<String>()

        if (disclaimersBlock.isNotBlank()) {
            results.addAll(disclaimersBlock.lines().map { it.trim() }.filter { it.isNotBlank() })
        }

        for (region in regions) {
            val text = region.text.trim()
            if (region.location == RegionLocation.FINE_PRINT ||
                region.relativeCenterY > 0.82f ||
                text.startsWith("*") ||
                (text.length > 60 && region.relativeHeight < 0.035f)
            ) {
                if (text.isNotBlank() && !results.contains(text)) {
                    results.add(text)
                }
            }
        }

        return results.distinct().take(30)
    }
}
