package com.sachlabel.app.data.model

/**
 * Represents the result verdict for a claim check.
 * These map directly to the result states defined in the PRD §7.4 and design.md.
 */
enum class Verdict {
    /** Claim is supported by the back-label evidence — explicit clean result. */
    CONSISTENT,

    /**
     * Claim is technically true but narrower than it appears, or qualified by fine print.
     * E.g., "No Added Sugar" while naturally sugar-heavy, or "100% Natural" with a disclaimer.
     */
    NEEDS_CONTEXT,

    /**
     * Claim is directly contradicted by an ingredient, nutrition value, or fine-print statement.
     * E.g., "No Preservatives" but sodium benzoate listed.
     */
    MISLEADING,

    /**
     * OCR extracted some text but there's not enough relevant back-label content
     * to make a reliable determination. Show evidence deficit, not a verdict.
     */
    NOT_ENOUGH_EVIDENCE,

    /**
     * No prominent marketing claim was detected on the front label.
     * This is a legitimate outcome, not an error.
     */
    NO_CLAIM_DETECTED
}
