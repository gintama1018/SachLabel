package com.sachlabel.app.engine

import com.sachlabel.app.data.model.ClaimResult
import com.sachlabel.app.data.model.Evidence
import com.sachlabel.app.data.model.Verdict

/**
 * Evidence-first guardrail: verifies that every cited evidence_quote actually
 * exists verbatim in the raw back-label OCR text before any result is displayed.
 *
 * Architecture requirement (Non-negotiable):
 * "Every verdict shown to a user must cite an exact quoted snippet that
 *  actually exists in the OCR text. Never show a verdict the system can't
 *  point to evidence for — show 'Not enough evidence' instead."
 *
 * Zero synthetic evidence policy:
 * - No bypasses for nutrition table entries.
 * - No bypasses for synthetic "No ..." absence notes.
 * - If an evidence quote is not found in rawBackText, the verdict is discarded
 *   and downgraded to NOT_ENOUGH_EVIDENCE.
 */
object EvidenceValidator {

    /**
     * Validate a [ClaimResult] against the raw back-label text.
     *
     * @param result      the result to validate
     * @param rawBackText exact OCR text from the back label
     * @return the original result if valid, or a NOT_ENOUGH_EVIDENCE result if not
     */
    fun validate(result: ClaimResult, rawBackText: String): ClaimResult {
        // NO_CLAIM_DETECTED and NOT_ENOUGH_EVIDENCE don't require text verification
        if (result.verdict == Verdict.NO_CLAIM_DETECTED ||
            result.verdict == Verdict.NOT_ENOUGH_EVIDENCE) {
            return result
        }

        val quote = result.evidence.quote.trim()
        val sourceField = result.evidence.sourceField

        // Absent evidence handling
        if (sourceField == Evidence.SourceField.ABSENT || quote.isBlank()) {
            // A claim cannot be flagged as MISLEADING without citing contradictory text from the label
            if (result.verdict == Verdict.MISLEADING) {
                return result.copy(
                    verdict = Verdict.NOT_ENOUGH_EVIDENCE,
                    evidence = Evidence.absent(),
                    explanationEn = "Not enough clear evidence to verify this claim from the back label."
                )
            }
            // For NEEDS_CONTEXT, only unverified absence rules (organic without cert mark, vague wellness without nutrients)
            // are allowed without a quote. Contradiction-style checks require a quote.
            if (result.verdict == Verdict.NEEDS_CONTEXT &&
                result.ruleKey !in setOf("organic", "vague_wellness", "immunity_booster")) {
                return result.copy(
                    verdict = Verdict.NOT_ENOUGH_EVIDENCE,
                    evidence = Evidence.absent(),
                    explanationEn = "Not enough clear evidence to verify this claim from the back label."
                )
            }
            return result
        }

        // Exact substring verification
        val isDirectSubstring = rawBackText.contains(quote, ignoreCase = true)

        // Internal whitespace-collapsed match (to accommodate OCR line-break formatting)
        val isWhitespaceNormalizedMatch = if (!isDirectSubstring) {
            val collapsedRaw = rawBackText.replace(Regex("\\s+"), " ")
            val collapsedQuote = quote.replace(Regex("\\s+"), " ")
            collapsedRaw.contains(collapsedQuote, ignoreCase = true)
        } else {
            true
        }

        return if (isDirectSubstring || isWhitespaceNormalizedMatch) {
            result
        } else {
            // Guardrail fired: evidence does not exist in the raw back label text
            result.copy(
                verdict = Verdict.NOT_ENOUGH_EVIDENCE,
                evidence = Evidence.absent(),
                explanationEn = "Not enough clear evidence to check this claim. " +
                    "The cited evidence could not be verified in the scanned label text."
            )
        }
    }

    /**
     * Convenience: validate a list of results.
     */
    fun validateAll(results: List<ClaimResult>, rawBackText: String): List<ClaimResult> =
        results.map { validate(it, rawBackText) }
}
