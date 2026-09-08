package com.sachlabel.app.engine

import com.sachlabel.app.data.model.CanonicalClaimCategory
import com.sachlabel.app.data.model.Claim

/**
 * Robust fuzzy claim matcher for front-label text detection.
 *
 * Sourced directly from [CanonicalClaimCategory] (8 documented v1 categories).
 * Uses token/span-based sliding window fuzzy matching to tolerate OCR noise
 * (e.g. mid-phrase typos, extra punctuation, embedded marketing text) without
 * relying solely on whole-block equality or whole-block Levenshtein.
 */
object ClaimMatcher {

    /**
     * Attempt to match a raw OCR text string to a known canonical claim category.
     *
     * @param text  raw OCR text from one region of the front label
     * @param prominenceScore  from OcrRegion (size + position combined)
     * @return matched [Claim] with verbatim rawText, or null if no pattern matches
     */
    fun match(text: String, prominenceScore: Float = 0f): Claim? {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return null

        val normalized = normalize(trimmed)
        val candidateTokens = normalized.split(" ").filter { it.isNotBlank() }
        if (candidateTokens.isEmpty()) return null

        // Check canonical categories in order
        for (category in CanonicalClaimCategory.values()) {
            for (trigger in category.triggers) {
                if (matchesTrigger(normalized, candidateTokens, trigger)) {
                    return Claim(
                        rawText = trimmed,
                        patternKey = category.id,
                        prominenceScore = prominenceScore
                    )
                }
            }
        }
        return null
    }

    /**
     * Find the best claim from candidate front regions.
     * Ranks matched claims by prominenceScore (bounding box size + position weight).
     */
    fun findBestClaim(candidates: List<Pair<String, Float>>): Claim? {
        return candidates
            .mapNotNull { (text, score) -> match(text, score) }
            .maxByOrNull { it.prominenceScore }
    }

    /**
     * Match a candidate against a trigger phrase using:
     * 1. Direct normalized containment
     * 2. Sliding window token matching with character edit-distance tolerance
     */
    fun matchesTrigger(
        normalizedCandidate: String,
        candidateTokens: List<String>,
        trigger: String
    ): Boolean {
        val normTrigger = normalize(trigger)
        if (normTrigger.isBlank()) return false

        // 1. Direct containment check
        if (normalizedCandidate.contains(normTrigger)) return true

        val triggerTokens = normTrigger.split(" ").filter { it.isNotBlank() }
        if (triggerTokens.isEmpty()) return false

        val windowSize = triggerTokens.size
        if (candidateTokens.size < windowSize) {
            // If candidate has fewer words than trigger, test entire string similarity
            return isTokenSequenceFuzzyMatch(candidateTokens, triggerTokens)
        }

        // 2. Sliding window across candidate tokens
        for (i in 0..(candidateTokens.size - windowSize)) {
            val window = candidateTokens.subList(i, i + windowSize)
            if (isTokenSequenceFuzzyMatch(window, triggerTokens)) {
                return true
            }
        }

        return false
    }

    private fun isTokenSequenceFuzzyMatch(
        candidateSeq: List<String>,
        targetSeq: List<String>
    ): Boolean {
        if (candidateSeq.size != targetSeq.size) return false

        for (i in candidateSeq.indices) {
            val c = candidateSeq[i]
            val t = targetSeq[i]

            if (c == t) continue

            // Allow OCR character substitution for 0 vs o / O
            val cNormalized = c.replace('0', 'o')
            val tNormalized = t.replace('0', 'o')
            if (cNormalized == tNormalized) continue

            // For short tokens (<= 3 chars), require exact match
            if (t.length <= 3) return false

            // For longer tokens (>= 4 chars), tolerate edit distance of 1
            if (levenshtein(c, t) > 1) {
                return false
            }
        }
        return true
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
     * Levenshtein edit distance for comparing short tokens.
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
}
