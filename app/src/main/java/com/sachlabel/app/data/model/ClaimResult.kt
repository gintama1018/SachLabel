package com.sachlabel.app.data.model

/**
 * The complete result for one claim check.
 *
 * Evidence-first: every displayed result retains the exact source text.
 * This matches the JSON schema defined in the prompt spec and architecture.md.
 *
 * @param claim         the matched front-label claim
 * @param verdict       result of the check
 * @param frontText     exact text from the front label (what the package claims)
 * @param evidence      back-label evidence that produced this verdict
 * @param explanationEn plain-language explanation in English
 * @param explanationLocalized  explanation in the user's selected language (may equal EN)
 * @param ruleKey       which rule produced this (for audit/debug logging)
 */
data class ClaimResult(
    val claim: Claim?,
    val verdict: Verdict,
    val frontText: String,
    val evidence: Evidence,
    val explanationEn: String,
    val explanationLocalized: String = explanationEn,
    val ruleKey: String = "none"
)
