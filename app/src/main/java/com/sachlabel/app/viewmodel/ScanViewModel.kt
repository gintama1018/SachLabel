package com.sachlabel.app.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sachlabel.app.data.mock.MockProducts
import com.sachlabel.app.data.mock.MockScenario
import com.sachlabel.app.data.model.*
import com.sachlabel.app.engine.ClaimMatcher
import com.sachlabel.app.engine.EvidenceValidator
import com.sachlabel.app.engine.ExplanationTemplates
import com.sachlabel.app.engine.RuleEngine
import com.sachlabel.app.ocr.LabelExtractor
import com.sachlabel.app.ocr.MlKitOcrProcessor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Coordinates the full SachLabel pipeline:
 * capture → OCR → claim detection → rule engine → evidence validation → result
 *
 * Exposes [uiState] as a StateFlow for Compose UI to observe.
 */
class ScanViewModel(application: Application) : AndroidViewModel(application) {

    // ─────────────────────────────────────────────────────────────────────────
    // UI State
    // ─────────────────────────────────────────────────────────────────────────

    sealed class ScanUiState {
        object Idle : ScanUiState()
        object CapturingFront : ScanUiState()
        object CapturingBack : ScanUiState()
        data class Processing(val step: ProcessingStep) : ScanUiState()
        data class Result(val scan: ProductScan) : ScanUiState()
        data class Error(val message: String) : ScanUiState()
    }

    enum class ProcessingStep(val labelResId: Int, val label: String) {
        READING_LABEL(com.sachlabel.app.R.string.processing_reading, "Reading label…"),
        FINDING_CLAIMS(com.sachlabel.app.R.string.processing_claims, "Finding claims…"),
        CHECKING_EVIDENCE(com.sachlabel.app.R.string.processing_evidence, "Checking evidence…"),
        PREPARING_EXPLANATION(com.sachlabel.app.R.string.processing_explanation, "Preparing explanation…")
    }

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    val historyRepository = com.sachlabel.app.data.repository.ScanHistoryRepository.getInstance(application)
    val savedScans: StateFlow<List<com.sachlabel.app.data.repository.SavedScanItem>> = historyRepository.scans

    private val _frontImagePath = MutableStateFlow<String?>(null)
    private val _backImagePath = MutableStateFlow<String?>(null)

    var selectedLanguage: UserLanguage = UserLanguage.ENGLISH

    init {
        // Safely check for on-device local AI model in background
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            com.sachlabel.app.engine.ai.LocalAiEngine.initialize(application)
        }
    }

    override fun onCleared() {
        super.onCleared()
        com.sachlabel.app.engine.ai.LocalAiEngine.unload()
    }

    fun showSavedScan(item: com.sachlabel.app.data.repository.SavedScanItem) {
        val scan = ProductScan(
            id = item.id,
            structuredLabel = StructuredLabel(frontClaimsRaw = listOf(item.frontClaim)),
            result = ClaimResult(
                claim = Claim(item.frontClaim, "saved_claim"),
                frontText = item.frontClaim,
                verdict = item.verdict,
                evidence = if (item.backTruth.isNotBlank() && item.backTruth != "No contradiction found") {
                    Evidence(quote = item.backTruth, sourceField = Evidence.SourceField.INGREDIENTS)
                } else {
                    Evidence.absent()
                },
                explanationEn = item.explanationEn
            ),
            isMock = item.isMock
        )
        _uiState.value = ScanUiState.Result(scan)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Navigation transitions
    // ─────────────────────────────────────────────────────────────────────────

    fun startScan() {
        _uiState.value = ScanUiState.CapturingFront
    }

    fun onFrontCaptured(imagePath: String) {
        _frontImagePath.value = imagePath
        _uiState.value = ScanUiState.CapturingBack
    }

    fun onBackCaptured(imagePath: String) {
        _backImagePath.value = imagePath
        processImages(
            frontPath = _frontImagePath.value ?: return,
            backPath = imagePath
        )
    }

    fun retakeFront() {
        _frontImagePath.value = null
        _uiState.value = ScanUiState.CapturingFront
    }

    fun retakeBack() {
        _backImagePath.value = null
        _uiState.value = ScanUiState.CapturingBack
    }

    fun reset() {
        _frontImagePath.value = null
        _backImagePath.value = null
        _uiState.value = ScanUiState.Idle
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mock demo mode — run a pre-built scenario without real images
    // ─────────────────────────────────────────────────────────────────────────

    fun runMockScenario(scenario: MockScenario) {
        viewModelScope.launch {
            processMock(scenario)
        }
    }

    private suspend fun processMock(scenario: MockScenario) {
        val sessionId = UUID.randomUUID().toString()

        // Simulate processing steps with realistic delays
        _uiState.value = ScanUiState.Processing(ProcessingStep.READING_LABEL)
        delay(600)
        _uiState.value = ScanUiState.Processing(ProcessingStep.FINDING_CLAIMS)
        delay(500)
        _uiState.value = ScanUiState.Processing(ProcessingStep.CHECKING_EVIDENCE)
        delay(700)
        _uiState.value = ScanUiState.Processing(ProcessingStep.PREPARING_EXPLANATION)
        delay(400)

        val label = scenario.structuredLabel

        // Run claim matcher against front claims
        val candidates = label.frontClaimsRaw.map { text ->
            Pair(text, 100f)  // Mock prominence score
        }
        val bestClaim = ClaimMatcher.findBestClaim(candidates)

        val result = if (bestClaim == null) {
            RuleEngine.noClaimDetected()
        } else {
            val rawResult = RuleEngine.check(bestClaim, label, selectedLanguage)
            // Apply evidence validation guardrail
            EvidenceValidator.validate(rawResult, label.rawBackText)
        }

        val scan = ProductScan(
            id = sessionId,
            structuredLabel = label,
            result = result,
            isMock = true
        )

        historyRepository.addScan(scan)
        _uiState.value = ScanUiState.Result(scan)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Real image processing pipeline
    // ─────────────────────────────────────────────────────────────────────────

    private fun processImages(frontPath: String, backPath: String) {
        viewModelScope.launch {
            var frontBitmap: Bitmap? = null
            var backBitmap: Bitmap? = null

            try {
                val sessionId = UUID.randomUUID().toString()

                _uiState.value = ScanUiState.Processing(ProcessingStep.READING_LABEL)

                // 1. Load with EXIF orientation correction, safe downsampling, and conditional enhancement
                frontBitmap = com.sachlabel.app.cv.ImagePreprocessor.loadAndPrepare(frontPath)
                backBitmap = com.sachlabel.app.cv.ImagePreprocessor.loadAndPrepare(backPath)

                if (frontBitmap == null || backBitmap == null) {
                    _uiState.value = ScanUiState.Error(
                        "We couldn't read this clearly. Try another photo."
                    )
                    return@launch
                }

                // 2. Multi-script on-device OCR (Latin + Devanagari) with exact bounding boxes
                val frontRegions = MlKitOcrProcessor.process(frontBitmap, useDevanagari = true)
                val backRegions = MlKitOcrProcessor.process(backBitmap, useDevanagari = true)

                _uiState.value = ScanUiState.Processing(ProcessingStep.FINDING_CLAIMS)

                // 3. Layout analysis & structured label extraction
                val frontAnalysis = com.sachlabel.app.ocr.LayoutAnalyzer.analyzeFront(frontRegions)
                val label = LabelExtractor.extract(frontRegions, backRegions)

                // 4. Composite claim matching (semantic match confidence + layout prominence)
                val bestClaim = ClaimMatcher.findBestClaim(frontAnalysis.candidateClaimsWithScores)

                // 4b. Release OCR bitmaps from memory before Local AI inference to minimize heap pressure (Phase 10)
                try {
                    frontBitmap?.recycle()
                    backBitmap?.recycle()
                } catch (_: Throwable) {}
                frontBitmap = null
                backBitmap = null

                _uiState.value = ScanUiState.Processing(ProcessingStep.CHECKING_EVIDENCE)

                // 5. Deterministic rule evaluation + LocalAiEngine (if unresolved) + EvidenceValidator
                val result = if (bestClaim == null) {
                    RuleEngine.noClaimDetected()
                } else {
                    val aiOutcome = com.sachlabel.app.engine.ai.LocalAiEngine.process(
                        claim = bestClaim,
                        label = label,
                        language = selectedLanguage
                    )
                    aiOutcome.claimResult
                }

                _uiState.value = ScanUiState.Processing(ProcessingStep.PREPARING_EXPLANATION)
                delay(300)

                val scan = ProductScan(
                    id = sessionId,
                    frontImagePath = frontPath,
                    backImagePath = backPath,
                    structuredLabel = label,
                    result = result,
                    isMock = false
                )

                historyRepository.addScan(scan)
                _uiState.value = ScanUiState.Result(scan)

            } catch (e: Exception) {
                _uiState.value = ScanUiState.Error(
                    "We couldn't read this clearly. Try another photo."
                )
            } finally {
                // Free memory
                try {
                    frontBitmap?.recycle()
                    backBitmap?.recycle()
                } catch (ignored: Throwable) {}
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Local Model Management & Diagnostics (Phase 2 & Phase 9)
    // ─────────────────────────────────────────────────────────────────────────

    fun importLocalModel(uri: android.net.Uri, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val res = com.sachlabel.app.engine.ai.LocalAiEngine.importModelFromUri(getApplication(), uri)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                if (res.isSuccess) {
                    onComplete(true, "Model imported successfully: ${res.getOrNull()?.name}")
                } else {
                    onComplete(false, "Import failed: ${res.exceptionOrNull()?.message}")
                }
            }
        }
    }

    fun runGemmaDiagnostic(onResult: (com.sachlabel.app.engine.ai.LocalAiEngine.DiagnosticResult) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val diag = com.sachlabel.app.engine.ai.LocalAiEngine.runDiagnostic(getApplication())
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onResult(diag)
            }
        }
    }
}
