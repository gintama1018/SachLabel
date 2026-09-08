package com.sachlabel.app.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.sachlabel.app.data.model.OcrRegion
import com.sachlabel.app.data.model.RegionLocation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * On-device OCR using ML Kit Text Recognition v2.
 *
 * Architecture.md §3.1:
 * - ML Kit Text Recognition v2 (on-device, Latin + Devanagari)
 * - DevanagariTextRecognizerOptions.Builder().build() — NOT ChineseTextRecognizerOptions
 * - Returns bounding boxes for prominence scoring (used by ClaimMatcher)
 *
 * Privacy: images are processed entirely on-device — never uploaded.
 */
object MlKitOcrProcessor {

    private val latinRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private val devanagariRecognizer by lazy {
        TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
    }

    /**
     * Process a bitmap and return a list of OCR regions.
     * Runs both Latin and Devanagari recognizers; merges results by bounding box.
     *
     * @param bitmap  photo bitmap (front or back of product)
     * @param useDevanagari  whether to also run Devanagari recognizer
     * @return list of [OcrRegion] with text and position metadata
     */
    suspend fun process(bitmap: Bitmap, useDevanagari: Boolean = true): List<OcrRegion> {
        val image = InputImage.fromBitmap(bitmap, 0)
        val latinRegions = runRecognizer(image, useDevanagari = false)
        val devanagariRegions = if (useDevanagari) {
            runRecognizer(image, useDevanagari = true)
        } else emptyList()

        // Merge and deduplicate (Devanagari recognizer output may overlap with Latin)
        return (latinRegions + devanagariRegions).distinctBy { it.text.trim() }
    }

    private suspend fun runRecognizer(
        image: InputImage,
        useDevanagari: Boolean
    ): List<OcrRegion> = suspendCancellableCoroutine { cont ->
        val recognizer = if (useDevanagari) devanagariRecognizer else latinRecognizer
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val regions = mutableListOf<OcrRegion>()
                val imageHeight = image.height.coerceAtLeast(1)

                for (block in visionText.textBlocks) {
                    val bbox = block.boundingBox ?: continue
                    val text = block.text.trim()
                    if (text.isBlank()) continue

                    val centerY = bbox.centerY()
                    val location = estimateLocation(
                        centerX = bbox.centerX(),
                        centerY = centerY,
                        imageWidth = image.width,
                        imageHeight = imageHeight,
                        isSmall = bbox.height() < imageHeight * 0.04
                    )

                    regions.add(
                        OcrRegion(
                            location = location,
                            text = text,
                            boundingHeight = bbox.height(),
                            boundingWidth = bbox.width(),
                            centerY = centerY
                        )
                    )
                }
                cont.resume(regions)
            }
            .addOnFailureListener { e -> cont.resumeWithException(e) }
    }

    /**
     * Heuristic location estimation from bounding box position.
     * Small text near edges/bottom → FINE_PRINT.
     */
    private fun estimateLocation(
        centerX: Int, centerY: Int,
        imageWidth: Int, imageHeight: Int,
        isSmall: Boolean
    ): RegionLocation {
        val relX = centerX.toFloat() / imageWidth
        val relY = centerY.toFloat() / imageHeight

        if (isSmall && relY > 0.75f) return RegionLocation.FINE_PRINT

        return when {
            relY < 0.35f -> when {
                relX < 0.35f -> RegionLocation.TOP_LEFT
                relX > 0.65f -> RegionLocation.TOP_RIGHT
                else -> RegionLocation.TOP_CENTER
            }
            relY > 0.70f -> when {
                relX < 0.5f -> RegionLocation.BOTTOM_LEFT
                else -> RegionLocation.BOTTOM_RIGHT
            }
            else -> RegionLocation.MIDDLE
        }
    }
}
