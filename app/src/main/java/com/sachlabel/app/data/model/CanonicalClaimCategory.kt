package com.sachlabel.app.data.model

/**
 * The 8 canonical v1 claim categories — the single source of truth for all detectable claims.
 * Rule logic is grounded in applicable food-labeling requirements.
 *
 * Sourced directly from docs/claims-taxonomy.md.
 * Strictly closed to these 8 categories in v1.
 *
 * ClaimMatcher, RuleEngine, WhatWeCheckScreen, and tests all derive from this registry.
 */
enum class CanonicalClaimCategory(
    val id: String,
    val displayName: String,
    val triggers: List<String>,
    val checkDescription: String
) {
    NO_ADDED_SUGAR(
        id = "no_added_sugar",
        displayName = "No Added Sugar",
        triggers = listOf(
            "no added sugar",
            "no added sugars",
            "without added sugar",
            "zero added sugar",
            "no sugar added",
            "बिना चीनी",
            "बिना शर्करा",
            "कोई चीनी नहीं",
            "शर्करा मुक्त"
        ),
        checkDescription = "Checked against: ingredient list for added-sugar terms (glucose syrup, maltodextrin, dextrose, invert syrup, etc.) and nutrition table sugar values."
    ),

    NATURAL_OR_PURE(
        id = "100_percent_natural",
        displayName = "100% Natural / Pure",
        triggers = listOf(
            "100% natural",
            "100% pure",
            "100 percent natural",
            "100 percent pure",
            "all natural",
            "purely natural",
            "100% प्राकृतिक",
            "100% शुद्ध",
            "प्राकृतिक",
            "शुद्ध"
        ),
        checkDescription = "Checked against: fine-print disclaimers and ingredient list for synthetic additives, artificial flavors, or chemical preservatives."
    ),

    SUGAR_FREE(
        id = "sugar_free",
        displayName = "Sugar-Free / Zero Sugar",
        triggers = listOf(
            "sugar free",
            "sugar-free",
            "zero sugar",
            "0% sugar",
            "0 sugar",
            "no sugar",
            "sugarless",
            "शुगर फ्री",
            "शुगर-फ्री",
            "चीनी रहित",
            "शून्य चीनी",
            "0% चीनी"
        ),
        checkDescription = "Checked against: nutrition table total sugars per 100g (evaluates whether total sugars exceed the 0.5g/100g threshold for sugar-free claims)."
    ),

    NO_PRESERVATIVES(
        id = "no_preservatives",
        displayName = "No Preservatives",
        triggers = listOf(
            "no preservatives",
            "preservative free",
            "preservative-free",
            "without preservatives",
            "no artificial preservatives",
            "zero preservatives",
            "बिना प्रिजर्वेटिव",
            "प्रिजर्वेटिव मुक्त",
            "परिरक्षक मुक्त",
            "बिना परिरक्षक"
        ),
        checkDescription = "Checked against: ingredient list for chemical preservatives (sodium benzoate, potassium sorbate, sulfites, INS 200–299 codes)."
    ),

    ORGANIC(
        id = "organic",
        displayName = "Organic / Certified Organic",
        triggers = listOf(
            "organic",
            "certified organic",
            "100% organic",
            "usda organic",
            "jaivik bharat",
            "जैविक",
            "प्रमाणित जैविक",
            "100% जैविक"
        ),
        checkDescription = "Checked for: presence of recognized organic certification mark or certificate number in label text."
    ),

    HIGH_PROTEIN(
        id = "high_protein",
        displayName = "High Protein",
        triggers = listOf(
            "high protein",
            "protein rich",
            "rich in protein",
            "good source of protein",
            "excellent source of protein",
            "protein packed",
            "हाई प्रोटीन",
            "प्रोटीन से भरपूर",
            "अधिक प्रोटीन"
        ),
        checkDescription = "Checked against: nutrition table protein content per 100g or per serving."
    ),

    ZERO_TRANS_FAT(
        id = "zero_trans_fat",
        displayName = "Zero Trans Fat",
        triggers = listOf(
            "zero trans fat",
            "0g trans fat",
            "0 trans fat",
            "trans fat free",
            "no trans fat",
            "trans-fat free",
            "0% trans fat",
            "शून्य ट्रांस फैट",
            "ट्रांस फैट मुक्त"
        ),
        checkDescription = "Checked against: ingredient list for partially hydrogenated vegetable oils and nutrition table trans fat values."
    ),

    VAGUE_WELLNESS(
        id = "vague_wellness",
        displayName = "Vague Wellness Claims",
        triggers = listOf(
            "immunity booster",
            "boosts immunity",
            "strengthens immunity",
            "detox",
            "detoxifying",
            "antioxidant rich",
            "pure & wholesome",
            "energy booster",
            "cleanses body",
            "इम्युनिटी",
            "रोग प्रतिरोधक",
            "ऊर्जा वर्धक",
            "डिटॉक्स"
        ),
        checkDescription = "Checked against: fine print disclaimers and ingredient list for supporting nutrients."
    );

    companion object {
        fun fromId(id: String): CanonicalClaimCategory? =
            values().firstOrNull { it.id == id }

        /** Returns all canonical categories in standard v1 order */
        val all: List<CanonicalClaimCategory> get() = values().toList()
    }
}
