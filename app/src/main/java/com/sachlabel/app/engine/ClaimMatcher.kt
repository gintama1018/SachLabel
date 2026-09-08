package com.sachlabel.app.engine

import com.sachlabel.app.data.model.Claim

/**
 * Fuzzy claim matcher for front-label text detection.
 *
 * Architecture.md §3.2: v1 uses heuristic matching (not ML) against a hardcoded
 * claim pattern list. Handles OCR noise via normalized comparison + keyword containment.
 *
 * Claim prominence: combines bounding-box text size + vertical position + claim-pattern match.
 * A small "100% Natural" badge is not ignored just because a giant brand name fills more pixels.
 */
object ClaimMatcher {

    /**
     * Attempt to match a raw OCR text string to a known claim pattern.
     *
     * @param text  raw OCR text from one region of the front label
     * @param prominenceScore  from OcrRegion (size + position combined)
     * @return matched [Claim] or null if no pattern matches
     */
    fun match(text: String, prominenceScore: Float = 0f): Claim? {
        val normalized = normalize(text)
        for (pattern in CLAIM_PATTERNS) {
            if (pattern.matches(normalized)) {
                return Claim(
                    rawText = text.trim(),
                    patternKey = pattern.key,
                    prominenceScore = prominenceScore
                )
            }
        }
        return null
    }

    /**
     * Find the best claim from a list of candidate regions.
     * Scoring: claim-pattern match is required; then rank by prominenceScore.
     * This avoids the "largest bounding box wins" failure mode for small badge claims.
     */
    fun findBestClaim(candidates: List<Pair<String, Float>>): Claim? {
        return candidates
            .mapNotNull { (text, score) -> match(text, score) }
            .maxByOrNull { it.prominenceScore }
    }

    /**
     * Normalize text for matching: lowercase, collapse whitespace, strip punctuation noise.
     */
    fun normalize(text: String): String =
        text.lowercase()
            .replace(Regex("[\\n\\r\\t]+"), " ")
            .replace(Regex("[^a-z0-9% ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    /**
     * Levenshtein distance — for fuzzy matching short phrases against OCR noise.
     * Only used for short strings (< 30 chars) where edit distance is meaningful.
     */
    fun levenshtein(a: String, b: String): Int {
        val m = a.length
        val n = b.length
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        for (i in 1..m) {
            for (j in 1..n) {
                dp[i][j] = if (a[i - 1] == b[j - 1]) dp[i - 1][j - 1]
                else 1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
            }
        }
        return dp[m][n]
    }

    /**
     * Returns true if [candidate] is a fuzzy match for [target]:
     * either contained within, or within edit distance threshold.
     */
    fun fuzzyContains(candidate: String, target: String, threshold: Int = 2): Boolean {
        val normCandidate = normalize(candidate)
        val normTarget = normalize(target)
        if (normCandidate.contains(normTarget)) return true
        if (normTarget.length <= 5) return normCandidate.contains(normTarget)
        return levenshtein(normCandidate, normTarget) <= threshold
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Claim pattern definitions
    // Each entry: key (used by rule engine), list of trigger phrases
    // ─────────────────────────────────────────────────────────────────────────

    private val CLAIM_PATTERNS: List<ClaimPattern> = listOf(

        ClaimPattern(
            key = "no_added_sugar",
            triggers = listOf(
                "no added sugar", "no added sugars",
                "sugar free", "sugar-free", "zero sugar", "0% sugar",
                "without added sugar"
            )
        ),

        ClaimPattern(
            key = "100_percent_natural",
            triggers = listOf(
                "100% natural", "100% pure", "100 percent natural",
                "100 percent pure", "all natural", "purely natural"
            )
        ),

        ClaimPattern(
            key = "no_preservatives",
            triggers = listOf(
                "no preservatives", "preservative free", "preservative-free",
                "no artificial preservatives", "without preservatives"
            )
        ),

        ClaimPattern(
            key = "no_artificial_colors",
            triggers = listOf(
                "no artificial colors", "no artificial colours",
                "no artificial color", "no artificial colour",
                "colour free", "color free"
            )
        ),

        ClaimPattern(
            key = "organic",
            triggers = listOf(
                "organic", "certified organic", "100% organic", "usda organic"
            )
        ),

        ClaimPattern(
            key = "high_protein",
            triggers = listOf(
                "high protein", "protein rich", "rich in protein",
                "good source of protein", "excellent source of protein"
            )
        ),

        ClaimPattern(
            key = "zero_trans_fat",
            triggers = listOf(
                "zero trans fat", "0g trans fat", "0 trans fat",
                "trans fat free", "no trans fat", "trans-fat free"
            )
        ),

        ClaimPattern(
            key = "immunity_booster",
            triggers = listOf(
                "immunity booster", "boosts immunity", "strengthens immunity",
                "immune support", "immunity support", "builds immunity",
                "detox", "detoxifying", "antioxidant rich",
                "boosts energy", "energy booster"
            )
        ),

        ClaimPattern(
            key = "gluten_free",
            triggers = listOf(
                "gluten free", "gluten-free", "no gluten", "without gluten"
            )
        ),

        ClaimPattern(
            key = "low_fat",
            triggers = listOf(
                "low fat", "low-fat", "fat free", "fat-free",
                "0% fat", "zero fat", "reduced fat"
            )
        )
    )

    /**
     * A single claim pattern with its key and trigger phrases.
     */
    private data class ClaimPattern(
        val key: String,
        val triggers: List<String>
    ) {
        fun matches(normalizedInput: String): Boolean {
            return triggers.any { trigger ->
                val normTrigger = trigger.lowercase()
                normalizedInput.contains(normTrigger) ||
                    levenshtein(normalizedInput, normTrigger) <= 2
            }
        }
    }
}
