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

        var bestMatch: Claim? = null
        var highestConfidence = 0f

        // Check canonical categories in order
        for (category in CanonicalClaimCategory.values()) {
            for (trigger in category.triggers) {
                val confidence = evaluateTriggerMatch(normalized, candidateTokens, trigger)
                if (confidence > highestConfidence) {
                    highestConfidence = confidence
                    bestMatch = Claim(
                        rawText = trimmed,
                        patternKey = category.id,
                        prominenceScore = prominenceScore,
                        matchConfidence = confidence
                    )
                }
            }
        }
        return bestMatch
    }

    /**
     * Find the best claim from candidate front regions.
     * Ranks matched claims by compositeScore (semantic match confidence + layout prominence).
     */
    fun findBestClaim(candidates: List<Pair<String, Float>>): Claim? {
        return candidates
            .mapNotNull { (text, score) -> match(text, score) }
            .maxByOrNull { it.compositeScore }
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
    ): Boolean = evaluateTriggerMatch(normalizedCandidate, candidateTokens, trigger) > 0f

    /**
     * Evaluates trigger match confidence:
     * 1.0f = exact match
     * 0.95f = direct substring containment
     * 0.85f = sliding window token fuzzy match
     * 0.0f = no match
     */
    fun evaluateTriggerMatch(
        normalizedCandidate: String,
        candidateTokens: List<String>,
        trigger: String
    ): Float {
        val normTrigger = normalize(trigger)
        if (normTrigger.isBlank()) return 0f

        // 1. Direct equality
        if (normalizedCandidate == normTrigger) return 1.0f

        // 2. Direct containment check
        if (normalizedCandidate.contains(normTrigger)) return 0.95f

        val triggerTokens = normTrigger.split(" ").filter { it.isNotBlank() }
        if (triggerTokens.isEmpty()) return 0f

        val windowSize = triggerTokens.size
        if (candidateTokens.size < windowSize) {
            // If candidate has fewer words than trigger, test entire string similarity
            return if (isTokenSequenceFuzzyMatch(candidateTokens, triggerTokens)) 0.85f else 0f
        }

        // 3. Sliding window across candidate tokens
        for (i in 0..(candidateTokens.size - windowSize)) {
            val window = candidateTokens.subList(i, i + windowSize)
            if (isTokenSequenceFuzzyMatch(window, triggerTokens)) {
                return 0.85f
            }
        }

        return 0f
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
     * Preserves Devanagari Unicode characters (\u0900-\u097F) for Hindi package text.
     */
    fun normalize(text: String): String =
        text.lowercase()
            .replace(Regex("[\\n\\r\\t]+"), " ")
            .replace(Regex("[^a-z0-9%\\u0900-\\u097F ]"), " ")
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
