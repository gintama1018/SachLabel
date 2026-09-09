package com.sachlabel.app.cv

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.File
import kotlin.math.max

/**
 * Computer Vision preprocessing for real packaged food photos.
 *
 * Pipeline requirements:
 * 1. Orientation correction: Detects JPEG EXIF tags and rotates bitmap to upright.
 * 2. Memory safety: Pre-calculates power-of-2 inSampleSize to prevent OOM on 48MP/64MP sensors,
 *    capping the max dimension to ~2048px while keeping 4pt–8pt statutory text crisp.
 * 3. Conditional enhancement: Evaluates image quality (dynamic range & average luminance).
 *    Applies contrast stretching ONLY if packaging is poorly lit or low contrast,
 *    preserving the original bitmap strokes when lighting is normal.
 */
object ImagePreprocessor {

    const val DEFAULT_MAX_DIMENSION = 2048
    private const val LOW_CONTRAST_THRESHOLD = 55 // Min difference between 5th and 95th percentile luminance

    data class QualityReport(
        val meanLuminance: Double,
        val contrastRange: Int,
        val isLowContrast: Boolean,
        val isDim: Boolean
    )

    /**
     * Load, rotate according to EXIF metadata, and downsample safely to [maxDimension].
     * Returns null if file cannot be decoded.
     */
    fun loadAndPrepare(
        imagePath: String,
        maxDimension: Int = DEFAULT_MAX_DIMENSION,
        enableConditionalEnhancement: Boolean = true
    ): Bitmap? {
        val file = File(imagePath)
        if (!file.exists() || file.length() == 0L) return null

        // 1. Read EXIF orientation
        val rotationDegrees = getExifRotationDegrees(imagePath)

        // 2. Decode image bounds only
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(imagePath, options)
        val origWidth = options.outWidth
        val origHeight = options.outHeight
        if (origWidth <= 0 || origHeight <= 0) return null

        // 3. Compute power-of-2 inSampleSize
        options.inSampleSize = calculateInSampleSize(origWidth, origHeight, maxDimension)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.ARGB_8888

        val decodedBitmap = try {
            BitmapFactory.decodeFile(imagePath, options)
        } catch (oom: OutOfMemoryError) {
            // Fallback with higher subsampling if memory is tight
            options.inSampleSize *= 2
            try {
                BitmapFactory.decodeFile(imagePath, options)
            } catch (e: Throwable) {
                null
            }
        } ?: return null

        // 4. Apply EXIF rotation if needed
        val orientedBitmap = if (rotationDegrees != 0) {
            rotateBitmap(decodedBitmap, rotationDegrees)
        } else {
            decodedBitmap
        }

        // 5. Conditional image quality assessment and enhancement
        return if (enableConditionalEnhancement) {
            assessAndEnhanceIfNeeded(orientedBitmap)
        } else {
            orientedBitmap
        }
    }

    /**
     * Inspects EXIF tags to determine required clockwise rotation in degrees.
     */
    fun getExifRotationDegrees(imagePath: String): Int = try {
        val exif = ExifInterface(imagePath)
        when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    } catch (e: Exception) {
        0
    }

    /**
     * Rotates a bitmap by the specified degrees, recycling the old bitmap if a new one was allocated.
     */
    fun rotateBitmap(source: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return source
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        if (rotated != source) {
            source.recycle()
        }
        return rotated
    }

    /**
     * Computes the largest inSampleSize value that is a power of 2 and keeps both
     * height and width within [maxDimension].
     */
    fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var inSampleSize = 1
        val maxDim = max(width, height)

        while ((maxDim / (inSampleSize * 2)) >= maxDimension) {
            inSampleSize *= 2
        }
        return inSampleSize
    }

    /**
     * Analyzes luminance distribution. If packaging has low contrast or is dim,
     * applies linear contrast stretching to enhance text edge legibility.
     * Otherwise returns the untouched original bitmap to prevent stroke degradation.
     */
    fun assessAndEnhanceIfNeeded(bitmap: Bitmap): Bitmap {
        val report = assessQuality(bitmap)
        if (!report.isLowContrast && !report.isDim) {
            // Quality is good — keep untouched original
            return bitmap
        }

        // Low contrast / dim detected — apply mild contrast stretching
        return applyContrastStretch(bitmap, report.contrastRange)
    }

    /**
     * Fast quality assessment using a sampled grid of pixels (step size = 16px).
     */
    fun assessQuality(bitmap: Bitmap): QualityReport {
        val width = bitmap.width
        val height = bitmap.height
        val step = max(1, max(width, height) / 80) // ~80x80 sample points

        var totalLum = 0L
        var sampleCount = 0
        val lumHistogram = IntArray(256)

        for (y in 0 until height step step) {
            for (x in 0 until width step step) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                // ITU-R BT.601 luminance
                val lum = ((r * 299) + (g * 587) + (b * 114)) / 1000
                lumHistogram[lum]++
                totalLum += lum
                sampleCount++
            }
        }

        if (sampleCount == 0) {
            return QualityReport(128.0, 255, isLowContrast = false, isDim = false)
        }

        val meanLum = totalLum.toDouble() / sampleCount

        // Approximate 5th and 95th percentiles to find dynamic range
        val p5Threshold = (sampleCount * 0.05).toInt()
        val p95Threshold = (sampleCount * 0.95).toInt()

        var cum = 0
        var p5 = 0
        var p95 = 255

        for (i in 0..255) {
            cum += lumHistogram[i]
            if (cum >= p5Threshold && p5 == 0) {
                p5 = i
            }
            if (cum >= p95Threshold) {
                p95 = i
                break
            }
        }

        val contrastRange = max(1, p95 - p5)
        val isLowContrast = contrastRange < LOW_CONTRAST_THRESHOLD
        val isDim = meanLum < 50.0

        return QualityReport(
            meanLuminance = meanLum,
            contrastRange = contrastRange,
            isLowContrast = isLowContrast,
            isDim = isDim
        )
    }

    /**
     * Applies mild contrast stretching to improve text clarity without blowing out highlights.
     */
    private fun applyContrastStretch(source: Bitmap, currentRange: Int): Bitmap {
        val width = source.width
        val height = source.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Stretch factor: moderate boost (1.1x to 1.35x), capped to avoid clipping
        val boostFactor = if (currentRange < 40) 1.35f else 1.15f

        val pixels = IntArray(width)
        for (y in 0 until height) {
            source.getPixels(pixels, 0, width, 0, y, width, 1)
            for (x in 0 until width) {
                val p = pixels[x]
                val a = Color.alpha(p)
                val r = Color.red(p)
                val g = Color.green(p)
                val b = Color.blue(p)

                // Center around midtone 128
                val newR = ((r - 128) * boostFactor + 128).toInt().coerceIn(0, 255)
                val newG = ((g - 128) * boostFactor + 128).toInt().coerceIn(0, 255)
                val newB = ((b - 128) * boostFactor + 128).toInt().coerceIn(0, 255)

                pixels[x] = Color.argb(a, newR, newG, newB)
            }
            result.setPixels(pixels, 0, width, 0, y, width, 1)
        }

        return result
    }
}
