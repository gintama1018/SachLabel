package com.sachlabel.app.engine

import com.sachlabel.app.data.model.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for the deterministic rule engine.
 *
 * Tests cover:
 * - Happy path for each claim rule
 * - No-evidence → NOT_ENOUGH_EVIDENCE
 * - Clean product → CONSISTENT (false-positive protection)
 * - Evidence quote presence
 * - Health disclaimer enforcement
 */
class RuleEngineTest {

    // ─────────────────────────────────────────────────────────────────────────
    // Rule 1 — No Added Sugar
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `no added sugar with maltodextrin returns NEEDS_CONTEXT`() {
        val claim = Claim("No Added Sugar", "no_added_sugar")
        val label = StructuredLabel(
            ingredients = listOf("Whole grain oats", "Maltodextrin", "Skimmed milk powder"),
            nutritionTable = mapOf(StructuredLabel.KEY_SUGARS_G to 8.0),
            rawBackText = "Whole grain oats, Maltodextrin, Skimmed milk powder"
        )
        val result = RuleEngine.check(claim, label)
        assertEquals(Verdict.NEEDS_CONTEXT, result.verdict)
        assertTrue("Evidence quote must not be blank", result.evidence.quote.isNotBlank())
        assertFalse("Evidence quote must not be fabricated",
            result.evidence.quote.contains("FABRICATED"))
    }

    @Test
    fun `no added sugar with elevated sugars_g returns NEEDS_CONTEXT`() {
        val claim = Claim("Zero Sugar", "no_added_sugar")
        val label = StructuredLabel(
            ingredients = listOf("Water", "Citric acid", "Natural flavour"),
            nutritionTable = mapOf(StructuredLabel.KEY_SUGARS_G to 5.0),
            rawBackText = "Water, Citric acid, Natural flavour. Sugars: 5g"
        )
        val result = RuleEngine.check(claim, label)
        assertEquals(Verdict.NEEDS_CONTEXT, result.verdict)
    }

    @Test
    fun `no added sugar clean product returns CONSISTENT`() {
        val claim = Claim("No Added Sugar", "no_added_sugar")
        val label = StructuredLabel(
            ingredients = listOf("Mineral water", "CO2"),
            nutritionTable = mapOf(StructuredLabel.KEY_SUGARS_G to 0.0),
            rawBackText = "Mineral water, CO2. Sugars: 0g"
        )
        val result = RuleEngine.check(claim, label)
        assertEquals(Verdict.CONSISTENT, result.verdict)
    }

    @Test
    fun `no added sugar no evidence returns NOT_ENOUGH_EVIDENCE`() {
        val claim = Claim("No Added Sugar", "no_added_sugar")
        val label = StructuredLabel(
            ingredients = emptyList(),
            nutritionTable = emptyMap(),
            rawBackText = ""
        )
        val result = RuleEngine.check(claim, label)
        assertEquals(Verdict.NOT_ENOUGH_EVIDENCE, result.verdict)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rule 2 — 100% Natural
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `100 percent natural with artificial flavour returns MISLEADING`() {
        val claim = Claim("100% Natural", "100_percent_natural")
        val label = StructuredLabel(
            ingredients = listOf("Wheat flour", "Sugar", "Artificial flavour (vanilla)"),
            finePrint = emptyList(),
            rawBackText = "Wheat flour, Sugar, Artificial flavour (vanilla)"
        )
        val result = RuleEngine.check(claim, label)
        assertEquals(Verdict.MISLEADING, result.verdict)
        assertTrue(result.evidence.quote.contains("Artificial flavour", ignoreCase = true))
    }

    @Test
    fun `100 percent natural with qualifier fine print returns NEEDS_CONTEXT`() {
        val claim = Claim("100% Natural", "100_percent_natural")
        val label = StructuredLabel(
            ingredients = listOf("Wheat flour", "Sugar", "Salt"),
            finePrint = listOf("\"100% Natural\" is a quality mark and does not imply the product is free from processing aids."),
            rawBackText = "Wheat flour, Sugar, Salt. \"100% Natural\" is a quality mark and does not imply the product is free from processing aids."
        )
        val result = RuleEngine.check(claim, label)
        assertEquals(Verdict.NEEDS_CONTEXT, result.verdict)
        assertTrue("Fine print quote must be in evidence",
            result.evidence.quote.contains("quality mark", ignoreCase = true))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rule 3 — No Preservatives
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `no preservatives with sodium benzoate returns MISLEADING`() {
        val claim = Claim("No Preservatives", "no_preservatives")
        val label = StructuredLabel(
            ingredients = listOf("Water", "Sugar", "Sodium benzoate (INS 211)"),
            rawBackText = "Water, Sugar, Sodium benzoate (INS 211)"
        )
        val result = RuleEngine.check(claim, label)
        assertEquals(Verdict.MISLEADING, result.verdict)
        assertTrue(result.evidence.quote.contains("Sodium benzoate", ignoreCase = true) ||
                   result.evidence.quote.contains("sodium benzoate", ignoreCase = true))
    }

    @Test
    fun `no preservatives with INS 211 code returns MISLEADING`() {
        val claim = Claim("No Preservatives", "no_preservatives")
        val label = StructuredLabel(
            ingredients = listOf("Water", "Sugar", "INS 211"),
            rawBackText = "Water, Sugar, INS 211"
        )
        val result = RuleEngine.check(claim, label)
        assertEquals(Verdict.MISLEADING, result.verdict)
    }

    @Test
    fun `no preservatives clean product returns CONSISTENT`() {
        val claim = Claim("No Preservatives", "no_preservatives")
        val label = StructuredLabel(
            ingredients = listOf("Milk", "Cream", "Salt"),
            rawBackText = "Milk, Cream, Salt"
        )
        val result = RuleEngine.check(claim, label)
        assertEquals(Verdict.CONSISTENT, result.verdict)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rule 4 — High Protein
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `high protein with 30g protein returns CONSISTENT`() {
        val claim = Claim("High Protein", "high_protein")
        val label = StructuredLabel(
            ingredients = listOf("Whey protein isolate (80%)"),
            nutritionTable = mapOf(StructuredLabel.KEY_PROTEIN_G to 30.0),
            rawBackText = "Whey protein isolate (80%). Protein: 30g"
        )
        val result = RuleEngine.check(claim, label)
        assertEquals(Verdict.CONSISTENT, result.verdict)
    }

    @Test
    fun `high protein with 8g protein returns NEEDS_CONTEXT`() {
        val claim = Claim("High Protein", "high_protein")
        val label = StructuredLabel(
            ingredients = listOf("Wheat flour", "Sugar", "Cocoa"),
            nutritionTable = mapOf(StructuredLabel.KEY_PROTEIN_G to 8.0),
            rawBackText = "Wheat flour, Sugar, Cocoa. Protein: 8g"
        )
        val result = RuleEngine.check(claim, label)
        assertEquals(Verdict.NEEDS_CONTEXT, result.verdict)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rule 5 — Zero Trans Fat
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `zero trans fat with partially hydrogenated oil returns NEEDS_CONTEXT`() {
        val claim = Claim("Zero Trans Fat", "zero_trans_fat")
        val label = StructuredLabel(
            ingredients = listOf("Wheat flour", "Partially hydrogenated vegetable oil", "Sugar"),
            rawBackText = "Wheat flour, Partially hydrogenated vegetable oil, Sugar"
        )
        val result = RuleEngine.check(claim, label)
        assertEquals(Verdict.NEEDS_CONTEXT, result.verdict)
        assertTrue(result.evidence.quote.contains("hydrogenated", ignoreCase = true))
    }

    @Test
    fun `zero trans fat clean product returns CONSISTENT`() {
        val claim = Claim("Zero Trans Fat", "zero_trans_fat")
        val label = StructuredLabel(
            ingredients = listOf("Milk", "Sugar", "Cocoa butter"),
            rawBackText = "Milk, Sugar, Cocoa butter"
        )
        val result = RuleEngine.check(claim, label)
        assertEquals(Verdict.CONSISTENT, result.verdict)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rule 6 — Immunity Booster
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `immunity booster with regulatory disclaimer returns NEEDS_CONTEXT`() {
        val claim = Claim("Immunity Booster", "immunity_booster")
        val label = StructuredLabel(
            ingredients = listOf("Water", "Honey", "Ginger extract"),
            finePrint = listOf("This statement has not been evaluated by FSSAI as a health claim."),
            rawBackText = "Water, Honey, Ginger extract. This statement has not been evaluated by FSSAI as a health claim."
        )
        val result = RuleEngine.check(claim, label)
        assertEquals(Verdict.NEEDS_CONTEXT, result.verdict)
        assertTrue(result.evidence.quote.contains("not been evaluated", ignoreCase = true))
    }

    @Test
    fun `immunity booster with no supporting ingredient returns NEEDS_CONTEXT`() {
        val claim = Claim("Boosts Immunity", "immunity_booster")
        val label = StructuredLabel(
            ingredients = listOf("Sugar", "Gelatin", "Artificial flavour"),
            finePrint = emptyList(),
            rawBackText = "Sugar, Gelatin, Artificial flavour"
        )
        val result = RuleEngine.check(claim, label)
        assertEquals(Verdict.NEEDS_CONTEXT, result.verdict)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // No claim detected
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `no claim detected returns NO_CLAIM_DETECTED verdict`() {
        val result = RuleEngine.noClaimDetected()
        assertEquals(Verdict.NO_CLAIM_DETECTED, result.verdict)
        assertNull(result.claim)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Evidence quote must not be blank for non-absent results
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `every non-absent verdict has non-blank front text`() {
        val claim = Claim("No Preservatives", "no_preservatives")
        val label = StructuredLabel(
            ingredients = listOf("Water", "Sodium benzoate (INS 211)"),
            rawBackText = "Water, Sodium benzoate (INS 211)"
        )
        val result = RuleEngine.check(claim, label)
        assertTrue("Front text must be present", result.frontText.isNotBlank())
    }
}
