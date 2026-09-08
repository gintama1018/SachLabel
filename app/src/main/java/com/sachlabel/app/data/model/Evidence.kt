package com.sachlabel.app.data.model

/**
 * A piece of evidence from the back label that supports a claim verdict.
 *
 * Architecture constraint: evidence_quote MUST be an exact substring of the source text.
 * This is verified by EvidenceValidator before any result is displayed.
 *
 * @param quote         exact text quoted from the back label — must be verifiable
 * @param sourceField   which field produced this (INGREDIENTS, NUTRITION, FINE_PRINT)
 * @param key           optional — which nutrition key triggered this (e.g., "sugars_g")
 * @param value         optional — the numeric value (for nutrition table entries)
 */
data class Evidence(
    val quote: String,
    val sourceField: SourceField,
    val key: String? = null,
    val value: Double? = null
) {
    enum class SourceField {
        INGREDIENTS, NUTRITION_TABLE, FINE_PRINT, ABSENT
    }

    companion object {
        /** Sentinel used when there is no relevant evidence to cite. */
        fun absent(): Evidence = Evidence(
            quote = "",
            sourceField = SourceField.ABSENT
        )
    }
}
