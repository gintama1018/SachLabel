package com.sachlabel.app.ocr

import com.sachlabel.app.data.model.StructuredLabel
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for LabelExtractor — OCR-tolerant anchors, bracket-safe ingredients,
 * dual sugar parsing, and verbatim raw OCR line retention.
 */
class LabelExtractorTest {

    @Test
    fun `extractIngredients handles OCR anchor typos like 1ngredients and lngredients`() {
        val rawText1 = "1ngredients: Whole Wheat Flour, Invert Sugar Syrup, Salt."
        val items1 = LabelExtractor.extractIngredients(rawText1)
        assertEquals(3, items1.size)
        assertTrue(items1[0].contains("Whole Wheat Flour"))

        val rawText2 = "lngredients: Rolled Oats, Cocoa Butter, Sugar."
        val items2 = LabelExtractor.extractIngredients(rawText2)
        assertEquals(3, items2.size)
        assertTrue(items2[0].contains("Rolled Oats"))
    }

    @Test
    fun `extractIngredients supports Devanagari Hindi anchor`() {
        val rawText = "सामग्री: गेहूं का आटा, चीनी, पाम तेल, नमक।"
        val items = LabelExtractor.extractIngredients(rawText)
        assertTrue(items.isNotEmpty())
        assertTrue(items[0].contains("गेहूं का आटा"))
    }

    @Test
    fun `splitIngredientsBracketSafe does not split inside nested parentheses`() {
        val text = "Refined Wheat Flour (Maida) (62%), Edible Vegetable Oil (Palm Oil, Antioxidant (INS 319)), Invert Sugar Syrup, Salt."
        val items = LabelExtractor.splitIngredientsBracketSafe(text)

        assertEquals(4, items.size)
        assertEquals("Refined Wheat Flour (Maida) (62%)", items[0])
        assertEquals("Edible Vegetable Oil (Palm Oil, Antioxidant (INS 319))", items[1])
        assertEquals("Invert Sugar Syrup", items[2])
        assertEquals("Salt", items[3])
    }

    @Test
    fun `extractNutritionTable parses dual sugars, trans fat with less-than symbol, and stores verbatim lines`() {
        val rawTable = """
            Nutritional Facts Per 100g
            Energy: 450 kcal
            Protein: 7.2 g
            Carbohydrates: 68.0 g
            Total Sugars: 14.5 g
            Added Sugars: 8.0 g
            Total Fat: 15.0 g
            Trans Fat: < 0.1 g
            Sodium: 320 mg
        """.trimIndent()

        val extraction = LabelExtractor.extractNutritionTable(rawTable)

        assertEquals(450.0, extraction.values[StructuredLabel.KEY_ENERGY_KCAL]!!, 0.01)
        assertEquals(7.2, extraction.values[StructuredLabel.KEY_PROTEIN_G]!!, 0.01)
        assertEquals(14.5, extraction.values[StructuredLabel.KEY_SUGARS_G]!!, 0.01)
        assertEquals(8.0, extraction.values[StructuredLabel.KEY_ADDED_SUGARS_G]!!, 0.01)
        assertEquals(0.1, extraction.values[StructuredLabel.KEY_TRANS_FAT_G]!!, 0.01)
        assertEquals(320.0, extraction.values[StructuredLabel.KEY_SODIUM_MG]!!, 0.01)

        // Verbatim raw line check (Zero Synthetic Evidence guarantee)
        val rawSugarLine = extraction.rawLines[StructuredLabel.KEY_SUGARS_G]
        assertNotNull(rawSugarLine)
        assertEquals("Total Sugars: 14.5 g", rawSugarLine)

        val rawAddedSugarLine = extraction.rawLines[StructuredLabel.KEY_ADDED_SUGARS_G]
        assertNotNull(rawAddedSugarLine)
        assertEquals("Added Sugars: 8.0 g", rawAddedSugarLine)
    }

    @Test
    fun `extractNutritionTable handles zero trans fat variations`() {
        val table1 = "Trans Fat: 0 g"
        val ext1 = LabelExtractor.extractNutritionTable(table1)
        assertEquals(0.0, ext1.values[StructuredLabel.KEY_TRANS_FAT_G]!!, 0.01)

        val table2 = "Trans Fat: 0.0 g"
        val ext2 = LabelExtractor.extractNutritionTable(table2)
        assertEquals(0.0, ext2.values[StructuredLabel.KEY_TRANS_FAT_G]!!, 0.01)
    }
}
