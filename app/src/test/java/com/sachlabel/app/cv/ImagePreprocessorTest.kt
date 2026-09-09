package com.sachlabel.app.cv

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ImagePreprocessor memory management and sampling algorithms.
 */
class ImagePreprocessorTest {

    @Test
    fun `calculateInSampleSize scales down 48MP image safely`() {
        // 8000x6000 (48MP camera sensor) down to max 2048px
        val sampleSize = ImagePreprocessor.calculateInSampleSize(8000, 6000, 2048)
        assertEquals(2, sampleSize)
        assertTrue(8000 / sampleSize <= 4096)
    }

    @Test
    fun `calculateInSampleSize scales down ultra-high resolution image safely`() {
        // 12000x9000 (108MP camera sensor) down to max 2048px
        val sampleSize = ImagePreprocessor.calculateInSampleSize(12000, 9000, 2048)
        assertEquals(4, sampleSize)
        assertTrue(12000 / sampleSize <= 4000)
    }

    @Test
    fun `calculateInSampleSize returns 1 for standard 1080p images`() {
        // 1920x1080 image is already under 2048px max dimension
        val sampleSize = ImagePreprocessor.calculateInSampleSize(1920, 1080, 2048)
        assertEquals(1, sampleSize)
    }

    @Test
    fun `calculateInSampleSize always returns a power of two`() {
        val testCases = listOf(
            Pair(4000, 3000),
            Pair(8000, 6000),
            Pair(16000, 12000),
            Pair(2500, 1800),
            Pair(600, 800)
        )

        for ((w, h) in testCases) {
            val sample = ImagePreprocessor.calculateInSampleSize(w, h, 2048)
            // Verify sample is a power of 2: (sample & (sample - 1)) == 0
            assertEquals("Sample size $sample must be a power of 2", 0, sample and (sample - 1))
        }
    }
}
