package com.sachlabel.app.ocr

import com.sachlabel.app.data.model.OcrRegion
import com.sachlabel.app.data.model.RegionLocation
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for LayoutAnalyzer — front multi-line synthesis, prominence scoring,
 * and back label zone segmentation.
 */
class LayoutAnalyzerTest {

    @Test
    fun `analyzeFront synthesizes vertically adjacent multi-line claims`() {
        val line1 = OcrRegion(
            location = RegionLocation.MIDDLE,
            text = "NO",
            boundingHeight = 60,
            boundingWidth = 100,
            centerY = 300,
            imageWidth = 1000,
            imageHeight = 1500,
            isLineLevel = true
        )
        val line2 = OcrRegion(
            location = RegionLocation.MIDDLE,
            text = "ADDED SUGAR",
            boundingHeight = 65,
            boundingWidth = 350,
            centerY = 380, // Gap of 80px (< 2.5x line height)
            imageWidth = 1000,
            imageHeight = 1500,
            isLineLevel = true
        )

        val result = LayoutAnalyzer.analyzeFront(listOf(line1, line2))
        val candidateTexts = result.candidateClaimsWithScores.map { it.first }

        assertTrue(
            "Expected 'NO ADDED SUGAR' to be synthesized from stacked lines",
            candidateTexts.any { it.equals("NO ADDED SUGAR", ignoreCase = true) }
        )
    }

    @Test
    fun `analyzeFront identifies primary brand in top section`() {
        val brand = OcrRegion(
            location = RegionLocation.TOP_CENTER,
            text = "BRITANNIA",
            boundingHeight = 80,
            boundingWidth = 400,
            centerY = 120, // relCenterY < 0.35
            imageWidth = 1000,
            imageHeight = 1500,
            isLineLevel = false
        )
        val claim = OcrRegion(
            location = RegionLocation.MIDDLE,
            text = "100% Whole Wheat",
            boundingHeight = 50,
            boundingWidth = 300,
            centerY = 500,
            imageWidth = 1000,
            imageHeight = 1500,
            isLineLevel = false
        )

        val result = LayoutAnalyzer.analyzeFront(listOf(brand, claim))
        assertEquals("BRITANNIA", result.primaryBrandOrTitle)
    }

    @Test
    fun `analyzeBack segments zones correctly`() {
        val rawBackText = """
            Nutritional Information per 100g
            Energy: 440 kcal
            Protein: 7.5g
            Total Sugars: 14g
            Ingredients: Refined Wheat Flour, Palm Oil, Invert Sugar Syrup.
            Mfg by: Britannia Industries Ltd.
            FSSAI Lic. No. 10015043001129
            *Contains naturally occurring sugars.
        """.trimIndent()

        val result = LayoutAnalyzer.analyzeBack(emptyList(), rawBackText)

        assertTrue(result.ingredientsBlock.contains("Wheat Flour"))
        assertTrue(result.nutritionBlock.contains("Energy: 440 kcal"))
        assertTrue(result.nutritionBlock.contains("Total Sugars: 14g"))
        assertTrue(result.statutoryBlock.contains("Mfg by"))
        assertTrue(result.disclaimersBlock.contains("Contains naturally occurring sugars"))

        // Certification mark detection
        assertTrue(
            "FSSAI license number should be detected",
            result.detectedCertificationMarks.any { it.contains("10015043001129") }
        )
    }

    @Test
    fun `analyzeBack detects organic certification marks without claiming verification`() {
        val rawBackText = """
            Ingredients: 100% Organic Rolled Oats.
            Certified Jaivik Bharat FSSAI
            USDA Organic Certified
        """.trimIndent()

        val result = LayoutAnalyzer.analyzeBack(emptyList(), rawBackText)

        assertTrue(
            result.detectedCertificationMarks.any { it.contains("Jaivik Bharat") }
        )
        assertTrue(
            result.detectedCertificationMarks.any { it.contains("USDA Organic") }
        )
        // Must say "detected", never "verified"
        for (mark in result.detectedCertificationMarks) {
            assertTrue("Mark should indicate detection, not regulatory verification", mark.contains("detected"))
        }
    }
}
