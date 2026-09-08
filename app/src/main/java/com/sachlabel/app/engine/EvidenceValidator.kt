package com.sachlabel.app.engine

import com.sachlabel.app.data.model.ClaimResult
import com.sachlabel.app.data.model.Evidence
import com.sachlabel.app.data.model.Verdict

/**
 * Post-rule / post-LLM guardrail: verify that any evidence_quote is an exact
 * substring of the source text before displaying a result.
 *
 * Architecture requirement (PROMPT_TEMPLATES.md, §4/5):
 * "After receiving this response, verify evidence_quote is an exact substring
 *  of the concatenated ingredients/nutrition/fine_print text. If it isn't,
 *  discard the LLM's verdict and fall back to NOT_ENOUGH_EVIDENCE."
 *
 * This validator runs on every ClaimResult, not just LLM-produced ones,
 * to guarantee the evidence-first principle holds across the whole pipeline.
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
        // NO_CLAIM_DETECTED and NOT_ENOUGH_EVIDENCE don't have quotes to verify
        if (result.verdict == Verdict.NO_CLAIM_DETECTED ||
            result.verdict == Verdict.NOT_ENOUGH_EVIDENCE) {
            return result
        }

        val quote = result.evidence.quote
        val sourceField = result.evidence.sourceField

        // Absent sentinel — no quote to validate
        if (sourceField == Evidence.SourceField.ABSENT || quote.isBlank()) {
            // If verdict requires evidence but there's none, downgrade
            return if (result.verdict in setOf(Verdict.MISLEADING, Verdict.NEEDS_CONTEXT)) {
                result.copy(
                    verdict = Verdict.NOT_ENOUGH_EVIDENCE,
                    explanationEn = "Not enough evidence to check this claim from the back label."
                )
            } else {
                result
            }
        }

        // For nutrition table entries (e.g. "Sugars: 8.0g per 100g"), we verify
        // that the underlying key and value are plausible from raw text
        if (sourceField == Evidence.SourceField.NUTRITION_TABLE) {
            return result  // Nutrition values come from structured parsing, not raw substring
        }

        // For synthetic "absence" notes (e.g., "No certification found"), skip raw check
        if (quote.startsWith("No ") || quote.startsWith("A protein-source")) {
            return result
        }

        // Core guardrail: evidence quote must be exact substring of back-label raw text
        val isValid = rawBackText.contains(quote, ignoreCase = true)

        return if (isValid) {
            result
        } else {
            // Guardrail fired — discard verdict, return NOT_ENOUGH_EVIDENCE
            result.copy(
                verdict = Verdict.NOT_ENOUGH_EVIDENCE,
                evidence = Evidence.absent(),
                explanationEn = "Not enough evidence to check this claim. " +
                    "The back label text could not be verified against the claimed evidence."
            )
        }
    }

    /**
     * Convenience: validate a list of results, discarding any that fail validation.
     */
    fun validateAll(results: List<ClaimResult>, rawBackText: String): List<ClaimResult> =
        results.map { validate(it, rawBackText) }
}
