package com.sachlabel.app.data.model

/**
 * Optional health context provided by the user in Flow 6 (opt-in personalization).
 *
 * This is NEVER used to produce a medical verdict.
 * Every output from this layer carries the fixed disclaimer:
 * "This ingredient may be relevant to the concern you mentioned.
 *  This is informational, not a medical determination."
 *
 * @param tags  selected preset conditions or free-text entries
 */
data class UserContext(
    val tags: List<String> = emptyList()
) {
    companion object {
        /** Preset chips shown in the health context screen. */
        val PRESETS = listOf(
            "Diabetic / Managing blood sugar",
            "High blood pressure",
            "Gluten-free",
            "Nut allergy",
            "Lactose intolerant",
            "Vegan",
            "Low sodium"
        )

        /** Fixed disclaimer — must always render. Cannot be removed or collapsed. */
        const val MANDATORY_DISCLAIMER =
            "This ingredient may be relevant to the concern you mentioned. " +
            "This is informational, not a medical determination."

        val EMPTY = UserContext()
    }
}
