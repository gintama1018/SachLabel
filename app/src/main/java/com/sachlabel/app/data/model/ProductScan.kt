package com.sachlabel.app.data.model

/**
 * A complete product scan session.
 *
 * @param id            unique session identifier
 * @param frontImagePath  file path to the captured front photo
 * @param backImagePath   file path to the captured back photo
 * @param structuredLabel  extracted label data (null until extraction completes)
 * @param result          claim check result (null until pipeline completes)
 * @param timestampMs     creation time
 * @param isMock          true when using mock data (demo mode)
 */
data class ProductScan(
    val id: String,
    val frontImagePath: String? = null,
    val backImagePath: String? = null,
    val structuredLabel: StructuredLabel? = null,
    val result: ClaimResult? = null,
    val timestampMs: Long = System.currentTimeMillis(),
    val isMock: Boolean = false
)
