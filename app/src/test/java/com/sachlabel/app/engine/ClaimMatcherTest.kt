package com.sachlabel.app.engine

import com.sachlabel.app.data.model.CanonicalClaimCategory
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ClaimMatcher — fuzzy matching, OCR noise handling, prominence scoring.
 */
class ClaimMatcherTest {

    @Test
    fun `exact match returns correct pattern key`() {
        val result = ClaimMatcher.match("No Added Sugar")
        assertNotNull(result)
        assertEquals("no_added_sugar", result!!.patternKey)
    }

    @Test
    fun `case insensitive match works`() {
        val result = ClaimMatcher.match("NO ADDED SUGAR")
        assertNotNull(result)
        assertEquals("no_added_sugar", result!!.patternKey)
    }

    @Test
    fun `match with OCR noise (extra space) works`() {
        val result = ClaimMatcher.match("No  Added  Sugar")
        assertNotNull(result)
    }

    @Test
    fun `100 percent natural matches natural_or_pure category`() {
        val result = ClaimMatcher.match("100% Natural")
        assertNotNull(result)
        assertEquals(CanonicalClaimCategory.NATURAL_OR_PURE.id, result!!.patternKey)
    }

    @Test
    fun `pure honey matches natural_or_pure category`() {
        val result = ClaimMatcher.match("100% Pure Honey")
        assertNotNull(result)
        assertEquals(CanonicalClaimCategory.NATURAL_OR_PURE.id, result!!.patternKey)
    }

    @Test
    fun `sugar free strictly matches sugar_free and not no_added_sugar`() {
        val result = ClaimMatcher.match("Sugar Free")
        assertNotNull(result)
        assertEquals("sugar_free", result!!.patternKey)
        assertNotEquals("no_added_sugar", result!!.patternKey)
    }

    @Test
    fun `zero sugar matches sugar_free`() {
        val result = ClaimMatcher.match("Zero Sugar")
        assertNotNull(result)
        assertEquals("sugar_free", result!!.patternKey)
    }

    @Test
    fun `no added sugar matches no_added_sugar`() {
        val result = ClaimMatcher.match("No Added Sugar")
        assertNotNull(result)
        assertEquals("no_added_sugar", result!!.patternKey)
    }

    @Test
    fun `no preservatives matches`() {
        val result = ClaimMatcher.match("No Preservatives")
        assertNotNull(result)
        assertEquals("no_preservatives", result!!.patternKey)
    }

    @Test
    fun `organic matches canonical organic category`() {
        val result = ClaimMatcher.match("Certified 100% Organic")
        assertNotNull(result)
        assertEquals("organic", result!!.patternKey)
    }

    @Test
    fun `high protein matches`() {
        val result = ClaimMatcher.match("High Protein")
        assertNotNull(result)
        assertEquals("high_protein", result!!.patternKey)
    }

    @Test
    fun `zero trans fat matches`() {
        val result = ClaimMatcher.match("Zero Trans Fat")
        assertNotNull(result)
        assertEquals("zero_trans_fat", result!!.patternKey)
    }

    @Test
    fun `immunity booster matches vague_wellness`() {
        val result = ClaimMatcher.match("Immunity Booster")
        assertNotNull(result)
        assertEquals("vague_wellness", result!!.patternKey)
    }

    @Test
    fun `sliding window matches claim embedded in long noisy package text`() {
        val text = "Super Crunch Breakfast Cereals With No Added Sugar Made From Whole Oats"
        val result = ClaimMatcher.match(text)
        assertNotNull(result)
        assertEquals("no_added_sugar", result!!.patternKey)
    }

    @Test
    fun `sliding window matches sugar free in long slogan`() {
        val text = "Refreshing Lemonade 100% Sugar Free Beverage"
        val result = ClaimMatcher.match(text)
        assertNotNull(result)
        assertEquals("sugar_free", result!!.patternKey)
    }

    @Test
    fun `brand name does not match any claim`() {
        val result = ClaimMatcher.match("Britannia Good Day")
        assertNull(result)
    }

    @Test
    fun `product description does not match any claim`() {
        val result = ClaimMatcher.match("Mango Delight Premium Quality")
        assertNull(result)
    }

    @Test
    fun `findBestClaim picks highest prominence match`() {
        val candidates = listOf(
            Pair("Britannia Biscuits", 5000f),  // Not a claim — large brand name
            Pair("No Added Sugar", 800f),         // Claim — smaller but matches pattern
        )
        val result = ClaimMatcher.findBestClaim(candidates)
        assertNotNull(result)
        assertEquals("no_added_sugar", result!!.patternKey)
    }

    @Test
    fun `normalize strips punctuation noise`() {
        val normalized = ClaimMatcher.normalize("100%\nNatural!")
        assertTrue(normalized.contains("100"))
        assertTrue(normalized.contains("natural"))
        assertFalse(normalized.contains("!"))
    }

    @Test
    fun `levenshtein distance zero for identical strings`() {
        assertEquals(0, ClaimMatcher.levenshtein("hello", "hello"))
    }

    @Test
    fun `levenshtein distance one for single insertion`() {
        assertEquals(1, ClaimMatcher.levenshtein("sugar", "sugars"))
    }

    @Test
    fun `levenshtein distance handles OCR common errors`() {
        // OCR might read '0' as 'O' or miss a space
        val dist = ClaimMatcher.levenshtein("zero sugar", "zer0 sugar")
        assertTrue(dist <= 2)
    }
}
