package com.sachlabel.app.engine

import com.sachlabel.app.data.model.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for EvidenceValidator — the critical guardrail that prevents fabricated evidence.
 */
class EvidenceValidatorTest {

    @Test
    fun `valid evidence quote passes validation`() {
        val rawBackText = "Ingredients: Water, Sugar, Sodium benzoate (INS 211)"
        val result = ClaimResult(
            claim = Claim("No Preservatives", "no_preservatives"),
            verdict = Verdict.MISLEADING,
            frontText = "No Preservatives",
            evidence = Evidence(
                quote = "Sodium benzoate (INS 211)",
                sourceField = Evidence.SourceField.INGREDIENTS
            ),
            explanationEn = "The front says No Preservatives, but sodium benzoate is listed."
        )
        val validated = EvidenceValidator.validate(result, rawBackText)
        assertEquals(Verdict.MISLEADING, validated.verdict)
    }

    @Test
    fun `fabricated evidence quote fails and returns NOT_ENOUGH_EVIDENCE`() {
        val rawBackText = "Ingredients: Water, Sugar, Salt"
        val result = ClaimResult(
            claim = Claim("No Preservatives", "no_preservatives"),
            verdict = Verdict.MISLEADING,
            frontText = "No Preservatives",
            evidence = Evidence(
                quote = "This quote does not exist in the back label text at all",
                sourceField = Evidence.SourceField.INGREDIENTS
            ),
            explanationEn = "Fabricated explanation."
        )
        val validated = EvidenceValidator.validate(result, rawBackText)
        assertEquals(Verdict.NOT_ENOUGH_EVIDENCE, validated.verdict)
    }

    @Test
    fun `NOT_ENOUGH_EVIDENCE result passes through without modification`() {
        val result = ClaimResult(
            claim = Claim("No Preservatives", "no_preservatives"),
            verdict = Verdict.NOT_ENOUGH_EVIDENCE,
            frontText = "No Preservatives",
            evidence = Evidence.absent(),
            explanationEn = "Not enough evidence."
        )
        val validated = EvidenceValidator.validate(result, "some back text")
        assertEquals(Verdict.NOT_ENOUGH_EVIDENCE, validated.verdict)
    }

    @Test
    fun `NO_CLAIM_DETECTED passes through without modification`() {
        val result = RuleEngine.noClaimDetected()
        val validated = EvidenceValidator.validate(result, "")
        assertEquals(Verdict.NO_CLAIM_DETECTED, validated.verdict)
    }

    @Test
    fun `CONSISTENT result with absent evidence passes through`() {
        val result = ClaimResult(
            claim = Claim("No Preservatives", "no_preservatives"),
            verdict = Verdict.CONSISTENT,
            frontText = "No Preservatives",
            evidence = Evidence.absent(),
            explanationEn = "Consistent."
        )
        val validated = EvidenceValidator.validate(result, "Water, Salt, Sugar")
        assertEquals(Verdict.CONSISTENT, validated.verdict)
    }

    @Test
    fun `MISLEADING result with empty evidence quote fails and downgrades to NOT_ENOUGH_EVIDENCE`() {
        val result = ClaimResult(
            claim = Claim("No Preservatives", "no_preservatives"),
            verdict = Verdict.MISLEADING,
            frontText = "No Preservatives",
            evidence = Evidence.absent(),
            explanationEn = "Missing evidence."
        )
        val validated = EvidenceValidator.validate(result, "Water, Salt, Sugar")
        assertEquals(Verdict.NOT_ENOUGH_EVIDENCE, validated.verdict)
    }

    @Test
    fun `fabricated synthetic quote is rejected under zero synthetic evidence policy`() {
        val result = ClaimResult(
            claim = Claim("No Preservatives", "no_preservatives"),
            verdict = Verdict.MISLEADING,
            frontText = "No Preservatives",
            evidence = Evidence(
                quote = "No preservative-class ingredients were detected.",
                sourceField = Evidence.SourceField.INGREDIENTS
            ),
            explanationEn = "Fake quote."
        )
        val validated = EvidenceValidator.validate(result, "Water, Salt, Sugar")
        assertEquals(Verdict.NOT_ENOUGH_EVIDENCE, validated.verdict)
    }

    @Test
    fun `health context disclaimer is always present`() {
        // Verify the disclaimer constant is exactly as specified
        assertEquals(
            "This ingredient may be relevant to the concern you mentioned. " +
            "This is informational, not a medical determination.",
            com.sachlabel.app.data.model.UserContext.MANDATORY_DISCLAIMER
        )
    }

    @Test
    fun `health engine always attaches disclaimer`() {
        val label = StructuredLabel(
            ingredients = listOf("Sugar", "Milk"),
            rawBackText = "Sugar, Milk"
        )
        val context = com.sachlabel.app.data.model.UserContext(tags = listOf("Diabetic"))
        val result = com.sachlabel.app.health.HealthContextEngine.analyze(label, context)
        assertEquals(
            com.sachlabel.app.data.model.UserContext.MANDATORY_DISCLAIMER,
            result.disclaimer
        )
    }
}
