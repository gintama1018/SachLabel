package com.sachlabel.app.ocr

import com.sachlabel.app.data.model.OcrRegion
import com.sachlabel.app.data.model.RegionLocation
import kotlin.math.abs

/**
 * Spatial Layout and Region Analyzer for food packaging.
 *
 * Distinguishes between:
 * 1. Front Layout:
 *    - Prominent marketing claims (large font, hero/center placement, badges)
 *    - Brand/Product names
 *    - Multi-line claim synthesis (e.g. "NO" [line 1] + "ADDED SUGAR" [line 2] -> "NO ADDED SUGAR")
 *
 * 2. Back Layout:
 *    - Ingredients zone
 *    - Nutrition facts table zone
 *    - Statutory declarations & detected certification marks (FSSAI Lic No, Jaivik Bharat, organic marks)
 *    - Disclaimers and fine-print footnotes
 */
object LayoutAnalyzer {

    data class FrontAnalysisResult(
        val candidateClaimsWithScores: List<Pair<String, Float>>,
        val primaryBrandOrTitle: String?,
        val frontRawLines: List<String>
    )

    data class BackZoneAnalysisResult(
        val ingredientsBlock: String,
        val nutritionBlock: String,
        val statutoryBlock: String,
        val disclaimersBlock: String,
        val detectedCertificationMarks: List<String>
    )

    /**
     * Analyzes front package OCR regions:
     * - Synthesizes vertically stacked multi-line claims
     * - Ranks candidates by layout prominence score
     */
    fun analyzeFront(frontRegions: List<OcrRegion>): FrontAnalysisResult {
        if (frontRegions.isEmpty()) {
            return FrontAnalysisResult(emptyList(), null, emptyList())
        }

        val candidates = mutableListOf<Pair<String, Float>>()
        val lineRegions = frontRegions.filter { it.isLineLevel }
        val blockRegions = frontRegions.filter { !it.isLineLevel }

        // 1. Add block-level regions with their prominence score
        for (block in blockRegions) {
            val trimmed = block.text.trim()
            if (trimmed.length in 3..120) {
                candidates.add(Pair(trimmed, block.prominenceScore))
            }
        }

        // 2. Add line-level regions
        for (line in lineRegions) {
            val trimmed = line.text.trim()
            if (trimmed.length in 3..80) {
                candidates.add(Pair(trimmed, line.prominenceScore))
            }
        }

        // 3. Multi-line claim synthesis: join vertically adjacent lines within similar vertical band
        val sortedLines = lineRegions.sortedBy { it.centerY }
        for (i in 0 until sortedLines.size - 1) {
            val line1 = sortedLines[i]
            val line2 = sortedLines[i + 1]

            // Check if line2 is directly below line1 (vertical gap < 2.5x line height)
            val verticalGap = line2.centerY - line1.centerY
            val avgHeight = (line1.boundingHeight + line2.boundingHeight) / 2
            val maxAllowedGap = if (avgHeight > 0) avgHeight * 3 else 150

            if (verticalGap in 1..maxAllowedGap) {
                val combinedText = "${line1.text.trim()} ${line2.text.trim()}"
                if (combinedText.length in 5..90) {
                    val combinedScore = (line1.prominenceScore + line2.prominenceScore) * 0.65f
                    candidates.add(Pair(combinedText, combinedScore))
                }

                // Also check 3-line stacking (e.g., "100%" + "PURE & NATURAL" + "WHOLE WHEAT")
                if (i + 2 < sortedLines.size) {
                    val line3 = sortedLines[i + 2]
                    val gap2 = line3.centerY - line2.centerY
                    if (gap2 in 1..maxAllowedGap) {
                        val triText = "${line1.text.trim()} ${line2.text.trim()} ${line3.text.trim()}"
                        if (triText.length in 8..110) {
                            val triScore = (line1.prominenceScore + line2.prominenceScore + line3.prominenceScore) * 0.45f
                            candidates.add(Pair(triText, triScore))
                        }
                    }
                }
            }
        }

        // Distinct by normalized text while preserving maximum prominence score
        val deduplicated = candidates
            .groupBy { it.first.lowercase().trim() }
            .map { (_, list) -> list.maxByOrNull { it.second }!! }
            .sortedByDescending { it.second }

        // Estimate primary brand or title (largest text in upper 30% of package)
        val primaryBrand = frontRegions
            .filter { it.relativeCenterY < 0.35f }
            .maxByOrNull { it.boundingHeight }
            ?.text
            ?.trim()

        return FrontAnalysisResult(
            candidateClaimsWithScores = deduplicated,
            primaryBrandOrTitle = primaryBrand,
            frontRawLines = frontRegions.map { it.text }
        )
    }

    /**
     * Segments back package into functional zones:
     * Ingredients, Nutrition facts, Statutory/Certifications, Disclaimers.
     */
    fun analyzeBack(backRegions: List<OcrRegion>, rawBackText: String): BackZoneAnalysisResult {
        val lines = rawBackText.lines().map { it.trim() }.filter { it.isNotBlank() }

        val ingredientsLines = mutableListOf<String>()
        val nutritionLines = mutableListOf<String>()
        val statutoryLines = mutableListOf<String>()
        val disclaimerLines = mutableListOf<String>()
        val detectedMarks = mutableListOf<String>()

        var currentZone = Zone.UNKNOWN

        for (line in lines) {
            val lower = line.lowercase()

            // Check for zone triggers
            when {
                lower.contains("ingredient") || lower.contains("सामग्री") || lower.contains("संघटक") -> {
                    currentZone = Zone.INGREDIENTS
                    ingredientsLines.add(line)
                }
                lower.contains("nutrition") || lower.contains("nutritive") || lower.contains("per 100") ||
                lower.contains("energy") || lower.contains("carbohydrate") || lower.contains("protein") ||
                lower.contains("पोषण") -> {
                    currentZone = Zone.NUTRITION
                    nutritionLines.add(line)
                }
                lower.contains("fssai") || lower.contains("lic. no") || lower.contains("lic no") ||
                lower.contains("organic") || lower.contains("jaivik") || lower.contains("mfg") ||
                lower.contains("manufactured") || lower.contains("marketed by") -> {
                    currentZone = Zone.STATUTORY
                    statutoryLines.add(line)
                }
                lower.startsWith("*") || lower.contains("contains allergen") || lower.contains("may contain") ||
                lower.contains("quality mark") || lower.contains("does not guarantee") -> {
                    disclaimerLines.add(line)
                }
                else -> {
                    when (currentZone) {
                        Zone.INGREDIENTS -> ingredientsLines.add(line)
                        Zone.NUTRITION -> nutritionLines.add(line)
                        Zone.STATUTORY -> statutoryLines.add(line)
                        Zone.UNKNOWN -> {
                            // Categorize by keyword
                            if (lower.contains("kcal") || lower.contains("sugar") || lower.contains("fat")) {
                                nutritionLines.add(line)
                            } else {
                                statutoryLines.add(line)
                            }
                        }
                    }
                }
            }

            // Detect statutory & certification marks (as extracted markers, not regulatory verification)
            if (lower.contains("fssai")) {
                val match = Regex("""(?i)(?:fssai|lic\.?\s*no\.?)[\s:]*([0-9]{14})""").find(line)
                if (match != null) {
                    detectedMarks.add("FSSAI Lic. No.: ${match.groupValues[1]}")
                } else {
                    detectedMarks.add("FSSAI mark detected")
                }
            }
            if (lower.contains("jaivik bharat")) {
                detectedMarks.add("Jaivik Bharat certification mark detected")
            }
            if (lower.contains("usda organic")) {
                detectedMarks.add("USDA Organic certification mark detected")
            }
            if (lower.contains("india organic") || lower.contains("npop")) {
                detectedMarks.add("India Organic / NPOP certification mark detected")
            }
        }

        return BackZoneAnalysisResult(
            ingredientsBlock = ingredientsLines.joinToString("\n"),
            nutritionBlock = nutritionLines.joinToString("\n"),
            statutoryBlock = statutoryLines.joinToString("\n"),
            disclaimersBlock = disclaimerLines.joinToString("\n"),
            detectedCertificationMarks = detectedMarks.distinct()
        )
    }

    private enum class Zone {
        UNKNOWN, INGREDIENTS, NUTRITION, STATUTORY
    }
}
