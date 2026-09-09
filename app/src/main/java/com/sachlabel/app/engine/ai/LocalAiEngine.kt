package com.sachlabel.app.engine.ai

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.sachlabel.app.data.model.*
import com.sachlabel.app.engine.EvidenceValidator
import com.sachlabel.app.engine.ExplanationTemplates
import com.sachlabel.app.engine.RuleEngine
import java.io.File

/**
 * On-Device Local AI Engine for SachLabel.
 *
 * Implements real Gemma 2B INT4 on-device inference via Google MediaPipe Tasks GenAI.
 * Strictly adheres to SachLabel's Safety Architecture:
 *
 * 1. The deterministic RuleEngine is ALWAYS the primary decision system.
 * 2. Local AI is NEVER on the critical evidence path. If the local model is
 *    unavailable, corrupted, or runs out of memory, the pipeline falls back
 *    immediately to deterministic template explanations without failing.
 * 3. The Local AI is ONLY used for:
 *    - Explanation generation & human simplification of verified facts
 *    - Reasoning on genuinely ambiguous claim/evidence pairs that RuleEngine cannot resolve
 * 4. The Local AI must NEVER:
 *    - Invent ingredients, nutrition values, or evidence quotes
 *    - Override deterministic rule results
 *    - Generate health scores or medical diagnoses
 * 5. Output quotes are strictly verified by [EvidenceValidator] against raw OCR text.
 */
object LocalAiEngine {

    private const val TAG = "LocalAiEngine"
    private const val MIN_MODEL_SIZE_BYTES = 500L * 1024L * 1024L // 500 MB minimum for ~1.2 GB LLMs

    private fun logInfo(msg: String) = try { android.util.Log.i(TAG, msg) } catch (_: Throwable) { println("[$TAG] $msg") }
    private fun logDebug(msg: String) = try { android.util.Log.d(TAG, msg) } catch (_: Throwable) { println("[$TAG] $msg") }
    private fun logWarn(msg: String, t: Throwable? = null) = try { android.util.Log.w(TAG, msg, t) } catch (_: Throwable) { println("[$TAG] $msg: $t") }
    private fun logError(msg: String, t: Throwable? = null) = try { android.util.Log.e(TAG, msg, t) } catch (_: Throwable) { System.err.println("[$TAG] $msg: $t") }

    // Candidate model paths in order of preference
    val CANDIDATE_MODEL_FILENAMES = listOf(
        "gemma-2b-it-cpu-int4.bin",
        "gemma-2b-it-gpu-int4.bin",
        "gemma-2b.bin",
        "model.bin",
        "gemma-2b-it.tflite",
        "model.task"
    )

    enum class EngineState {
        UNINITIALIZED,
        INITIALIZING,
        READY,
        MODEL_NOT_FOUND,
        INITIALIZATION_FAILED,
        ERROR
    }

    data class ModelMetadata(
        val file: File,
        val fileName: String,
        val sizeBytes: Long,
        val sizeMB: Double,
        val format: String,
        val isReadable: Boolean
    )

    data class LocalAiResult(
        val claimResult: ClaimResult,
        val isFromLocalAi: Boolean,
        val latencyMs: Long,
        val engineState: EngineState,
        val modelName: String? = null
    )

    data class DiagnosticResult(
        val modelFound: Boolean,
        val modelPath: String? = null,
        val modelSizeBytes: Long = 0L,
        val modelSizeMB: Double = 0.0,
        val engineState: EngineState,
        val pingPrompt: String = "Reply with exactly: GEMMA_OK",
        val pingOutput: String? = null,
        val pingSuccess: Boolean = false,
        val pingLatencyMs: Long = 0L,
        val domainClaim: String = "No Added Sugar",
        val domainPrompt: String = "",
        val domainOutput: String? = null,
        val domainVerdict: Verdict? = null,
        val domainEvidenceQuote: String? = null,
        val domainSuccess: Boolean = false,
        val domainLatencyMs: Long = 0L,
        val totalLatencyMs: Long = 0L,
        val errorMessage: String? = null
    )

    private var currentState: EngineState = EngineState.UNINITIALIZED
    private var activeModelFile: File? = null
    private var activeModelMeta: ModelMetadata? = null
    private var customRunner: LocalModelRunner? = null

    val state: EngineState get() = currentState
    val loadedModel: File? get() = activeModelFile
    val activeMetadata: ModelMetadata? get() = activeModelMeta

    /**
     * Set a custom runner (used for unit tests or simulated hardware backends).
     */
    fun setRunnerForTesting(runner: LocalModelRunner?) {
        this.customRunner = runner
        if (runner != null) {
            currentState = EngineState.READY
        } else {
            currentState = EngineState.UNINITIALIZED
        }
    }

    /**
     * Checks if a compatible on-device model file exists in SachLabel's storage.
     * Looks in:
     * 1. App's external files directory: /sdcard/Android/data/com.sachlabel.app/files/models/
     * 2. App's internal files directory: /data/user/0/com.sachlabel.app/files/models/
     * 3. App's root files directories
     */
    fun findAvailableModel(context: Context): ModelMetadata? {
        val candidateDirs = listOfNotNull(
            context.getExternalFilesDir("models"),
            File(context.filesDir, "models"),
            context.getExternalFilesDir(null),
            context.filesDir
        )

        for (dir in candidateDirs) {
            if (!dir.exists()) continue

            // 1. Check known candidate filenames
            for (name in CANDIDATE_MODEL_FILENAMES) {
                val file = File(dir, name)
                if (file.exists() && file.isFile && file.length() >= MIN_MODEL_SIZE_BYTES) {
                    val meta = toModelMetadata(file)
                    activeModelMeta = meta
                    return meta
                }
            }

            // 2. Dynamic discovery: any compatible model file >= 500 MB
            val anyCompatible = dir.listFiles()?.firstOrNull { f ->
                f.isFile && f.length() >= MIN_MODEL_SIZE_BYTES &&
                (f.name.endsWith(".bin", ignoreCase = true) ||
                 f.name.endsWith(".task", ignoreCase = true) ||
                 f.name.endsWith(".tflite", ignoreCase = true) ||
                 f.name.endsWith(".gguf", ignoreCase = true))
            }
            if (anyCompatible != null) {
                val meta = toModelMetadata(anyCompatible)
                activeModelMeta = meta
                return meta
            }
        }
        activeModelMeta = null
        return null
    }

    private fun toModelMetadata(file: File): ModelMetadata {
        val format = when {
            file.name.endsWith(".bin", ignoreCase = true) -> "MediaPipe / TFLite Binary (.bin)"
            file.name.endsWith(".task", ignoreCase = true) -> "MediaPipe Task (.task)"
            file.name.endsWith(".tflite", ignoreCase = true) -> "LiteRT / TFLite (.tflite)"
            file.name.endsWith(".gguf", ignoreCase = true) -> "GGUF Quantized (.gguf)"
            else -> "Binary Model"
        }
        return ModelMetadata(
            file = file,
            fileName = file.name,
            sizeBytes = file.length(),
            sizeMB = file.length() / (1024.0 * 1024.0),
            format = format,
            isReadable = file.canRead()
        )
    }

    /**
     * Safely imports a user-selected model file via Android Storage Access Framework (SAF).
     * Streams the URI to the app's internal/external models directory.
     */
    fun importModelFromUri(context: Context, sourceUri: Uri): Result<File> {
        return try {
            val contentResolver = context.contentResolver
            var fileName = "gemma-2b-it-cpu-int4.bin"

            contentResolver.query(sourceUri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    val name = cursor.getString(nameIndex)
                    if (!name.isNullOrBlank()) fileName = name
                }
            }

            val modelsDir = context.getExternalFilesDir("models") ?: File(context.filesDir, "models")
            if (!modelsDir.exists()) modelsDir.mkdirs()

            val destinationFile = File(modelsDir, fileName)
            logInfo("Importing model from URI to: ${destinationFile.absolutePath}")

            contentResolver.openInputStream(sourceUri)?.use { input ->
                destinationFile.outputStream().use { output ->
                    input.copyTo(output, bufferSize = 128 * 1024)
                }
            }

            initialize(context)
            Result.success(destinationFile)
        } catch (e: Exception) {
            logError("Failed to import model from URI", e)
            Result.failure(e)
        }
    }

    /**
     * Safely initializes the Local AI engine.
     * Guaranteed to never throw an uncaught exception or crash the app.
     */
    @Synchronized
    fun initialize(context: Context): Boolean {
        if (currentState == EngineState.READY && customRunner != null) return true

        currentState = EngineState.INITIALIZING
        val model = findAvailableModel(context)

        if (model == null) {
            logInfo("No local AI model found in app storage. Deterministic fallback mode active.")
            currentState = EngineState.MODEL_NOT_FOUND
            activeModelFile = null
            return false
        }

        if (!model.isReadable) {
            logError("Model file exists but is not readable: ${model.file.absolutePath}")
            currentState = EngineState.INITIALIZATION_FAILED
            return false
        }

        return try {
            activeModelFile = model.file
            // Instantiate real Gemma runner if custom runner is not set
            if (customRunner == null) {
                customRunner = GemmaLocalModelRunner(context, model.file)
            }
            currentState = EngineState.READY
            logInfo("Real Gemma on-device engine initialized: ${model.fileName} (${model.sizeMB.toInt()} MB)")
            true
        } catch (t: Throwable) {
            logError("Failed to initialize Gemma runner", t)
            currentState = EngineState.INITIALIZATION_FAILED
            activeModelFile = null
            customRunner = null
            false
        }
    }

    /**
     * Executes the multimodal reasoning pipeline with graceful deterministic fallback.
     *
     * Pipeline logic:
     * 1. Deterministic RuleEngine evaluates first.
     * 2. If the rule engine produces a decisive verdict (CONSISTENT, MISLEADING) or if the
     *    local model is unavailable, return deterministic result immediately.
     * 3. If the claim is ambiguous / unresolved (NEEDS_CONTEXT, NOT_ENOUGH_EVIDENCE) and the
     *    local model is READY, run local inference to explain or resolve ambiguity.
     * 4. Enforce strict EvidenceValidator guardrail on model output.
     */
    fun process(
        claim: Claim?,
        label: StructuredLabel,
        language: UserLanguage = UserLanguage.ENGLISH
    ): LocalAiResult {
        val startTime = System.currentTimeMillis()

        // 1. Always run deterministic rule engine first
        val deterministicResult = if (claim == null) {
            RuleEngine.noClaimDetected()
        } else {
            val raw = RuleEngine.check(claim, label, language)
            EvidenceValidator.validate(raw, label.rawBackText)
        }

        // If local model is not ready, or claim is null, return deterministic result immediately
        val runner = customRunner
        if (currentState != EngineState.READY || runner == null || claim == null) {
            return LocalAiResult(
                claimResult = deterministicResult,
                isFromLocalAi = false,
                latencyMs = System.currentTimeMillis() - startTime,
                engineState = currentState,
                modelName = activeModelFile?.name
            )
        }

        // 2. Only invoke Local AI if the claim result is ambiguous or needs simplification
        val needsAiReasoning = deterministicResult.verdict == Verdict.NEEDS_CONTEXT ||
                               deterministicResult.verdict == Verdict.NOT_ENOUGH_EVIDENCE

        if (!needsAiReasoning) {
            // Decisive verdict already reached deterministically
            return LocalAiResult(
                claimResult = deterministicResult,
                isFromLocalAi = false,
                latencyMs = System.currentTimeMillis() - startTime,
                engineState = currentState,
                modelName = activeModelFile?.name
            )
        }

        // 3. Construct targeted prompt (Phase 6 & 7: targeted evidence + deterministic result)
        val prompt = buildStructuredPrompt(claim, label, deterministicResult)

        return try {
            val rawModelOutput = runner.generate(prompt)
            val elapsedMs = System.currentTimeMillis() - startTime
            logDebug("Local AI inference completed in ${elapsedMs}ms")

            // 4. Parse JSON output safely
            val parsedResult = parseModelResponse(rawModelOutput, claim, deterministicResult)

            // 5. Strict guardrail: EvidenceValidator verifies evidence_quote exists in rawBackText
            val validatedResult = EvidenceValidator.validate(parsedResult, label.rawBackText)

            // Localize explanation if required
            val localizedResult = if (language.code == "en") validatedResult
            else validatedResult.copy(
                explanationLocalized = ExplanationTemplates.get(validatedResult, language)
            )

            LocalAiResult(
                claimResult = localizedResult,
                isFromLocalAi = true,
                latencyMs = elapsedMs,
                engineState = EngineState.READY,
                modelName = activeModelFile?.name ?: "GemmaLocal"
            )
        } catch (t: Throwable) {
            logWarn("Local AI inference failed; falling back to deterministic result", t)
            LocalAiResult(
                claimResult = deterministicResult,
                isFromLocalAi = false,
                latencyMs = System.currentTimeMillis() - startTime,
                engineState = EngineState.ERROR,
                modelName = activeModelFile?.name
            )
        }
    }

    /**
     * Targeted evidence subset for prompt efficiency (Phase 7).
     */
    data class TargetedEvidence(
        val ingredients: List<String>,
        val nutrition: Map<String, Double>,
        val finePrint: List<String>
    )

    /**
     * Filters extracted label down to only relevant evidence for the given claim.
     * Prevents wasting Gemma's context window on irrelevant text (Phase 7).
     */
    fun filterRelevantEvidence(claim: Claim, label: StructuredLabel): TargetedEvidence {
        val pattern = claim.patternKey.lowercase()

        val relevantIngredients = when {
            pattern.contains("sugar") -> {
                val sugarKeywords = listOf("sugar", "maltitol", "sucrose", "glucose", "fructose", "syrup", "honey", "jaggery", "aspartame", "sweetener", "dextrose", "maltodextrin", "sorbitol", "stevia")
                label.ingredients.filter { ing -> sugarKeywords.any { kw -> ing.lowercase().contains(kw) } }
                    .ifEmpty { label.ingredients.take(6) }
            }
            pattern.contains("protein") -> {
                val proteinKeywords = listOf("protein", "whey", "soy", "casein", "isolate", "pea", "egg", "milk", "gluten")
                label.ingredients.filter { ing -> proteinKeywords.any { kw -> ing.lowercase().contains(kw) } }
                    .ifEmpty { label.ingredients.take(6) }
            }
            pattern.contains("fruit") || pattern.contains("juice") -> {
                val juiceKeywords = listOf("juice", "fruit", "concentrate", "pulp", "water", "sugar", "flavor")
                label.ingredients.filter { ing -> juiceKeywords.any { kw -> ing.lowercase().contains(kw) } }
                    .ifEmpty { label.ingredients.take(6) }
            }
            pattern.contains("wheat") || pattern.contains("atta") -> {
                val wheatKeywords = listOf("wheat", "atta", "maida", "flour", "refined")
                label.ingredients.filter { ing -> wheatKeywords.any { kw -> ing.lowercase().contains(kw) } }
                    .ifEmpty { label.ingredients.take(6) }
            }
            pattern.contains("organic") -> {
                val organicKeywords = listOf("organic", "jaivik", "npop", "natural")
                label.ingredients.filter { ing -> organicKeywords.any { kw -> ing.lowercase().contains(kw) } }
                    .ifEmpty { label.ingredients.take(6) }
            }
            pattern.contains("preservative") -> {
                val presKeywords = listOf("preservative", "ins", "benzoate", "sorbate", "propionate", "class ii")
                label.ingredients.filter { ing -> presKeywords.any { kw -> ing.lowercase().contains(kw) } }
                    .ifEmpty { label.ingredients.take(6) }
            }
            else -> label.ingredients.take(6)
        }

        val relevantNutrition = when {
            pattern.contains("sugar") -> label.nutritionTable.filterKeys {
                it.contains("sugar", ignoreCase = true) || it.contains("carb", ignoreCase = true) || it.contains("energy", ignoreCase = true)
            }
            pattern.contains("protein") -> label.nutritionTable.filterKeys {
                it.contains("protein", ignoreCase = true) || it.contains("energy", ignoreCase = true)
            }
            pattern.contains("fat") -> label.nutritionTable.filterKeys {
                it.contains("fat", ignoreCase = true) || it.contains("saturat", ignoreCase = true)
            }
            else -> label.nutritionTable.entries.take(5).associate { it.key to it.value }
        }

        val relevantFinePrint = label.finePrint.take(2)

        return TargetedEvidence(
            ingredients = relevantIngredients,
            nutrition = relevantNutrition,
            finePrint = relevantFinePrint
        )
    }

    /**
     * Constructs the constrained prompt adhering strictly to Phase 6 & Phase 7 specifications.
     */
    fun buildStructuredPrompt(
        claim: Claim,
        label: StructuredLabel,
        deterministicResult: ClaimResult? = null
    ): String {
        val targeted = filterRelevantEvidence(claim, label)

        val ingredientsJson = targeted.ingredients.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
        val nutritionJson = targeted.nutrition.entries.joinToString(prefix = "{", postfix = "}") {
            "\"${it.key}\": ${it.value}"
        }
        val finePrintJson = targeted.finePrint.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }

        val detSection = if (deterministicResult != null) {
            """
            Preliminary Analysis: Verdict=${deterministicResult.verdict.name} ("${deterministicResult.explanationEn}")
            """.trimIndent()
        } else {
            "Preliminary Analysis: Unresolved"
        }

        return """
            SYSTEM:
            You are SachLabel's on-device claim verification assistant.
            You verify whether a product's front-of-pack claim is supported, contradicted, or qualified by back-of-pack evidence.
            Rules:
            1. Base your verdict strictly on the evidence text provided below. Never use outside knowledge.
            2. The "evidence_quote" MUST be an exact verbatim substring from the ingredients, nutrition, or fine print below. If no quote applies, use "".
            3. Do NOT invent ingredients, nutrition values, or quotations.
            4. Do NOT give medical advice or health scores.
            5. Allowed verdicts: CONSISTENT, MISLEADING, NEEDS_CONTEXT, NOT_ENOUGH_EVIDENCE.
            Return ONLY valid JSON with keys: verdict, explanation, evidence_quote.

            USER:
            Claim: "${claim.rawText}" (Category: ${claim.patternKey})
            $detSection
            Relevant Ingredients: $ingredientsJson
            Relevant Nutrition: $nutritionJson
            Relevant Fine Print: $finePrintJson
        """.trimIndent()
    }

    /**
     * Parses the LLM's response JSON, with graceful fallback to the deterministic result
     * if the model output is malformed or unparseable.
     */
    fun parseModelResponse(
        response: String,
        claim: Claim,
        fallback: ClaimResult
    ): ClaimResult {
        try {
            val verdictRegex = Regex("""(?i)"verdict"\s*:\s*"([^"]+)"""")
            val explanationRegex = Regex("""(?i)"explanation"\s*:\s*"([^"]+)"""")
            val quoteRegex = Regex("""(?i)"evidence_quote"\s*:\s*"([^"]*)"""")

            val verdictMatch = verdictRegex.find(response)?.groupValues?.get(1)?.trim()?.uppercase()
            val explanationMatch = explanationRegex.find(response)?.groupValues?.get(1)?.trim()
            val quoteMatch = quoteRegex.find(response)?.groupValues?.get(1)?.trim() ?: ""

            val verdict = when (verdictMatch) {
                "CONSISTENT" -> Verdict.CONSISTENT
                "NEEDS_CONTEXT" -> Verdict.NEEDS_CONTEXT
                "MISLEADING" -> Verdict.MISLEADING
                "NOT_ENOUGH_EVIDENCE" -> Verdict.NOT_ENOUGH_EVIDENCE
                else -> fallback.verdict
            }

            val explanation = if (!explanationMatch.isNullOrBlank()) explanationMatch else fallback.explanationEn

            val evidence = if (quoteMatch.isNotBlank()) {
                val sourceField = if (fallback.evidence.sourceField != Evidence.SourceField.ABSENT) {
                    fallback.evidence.sourceField
                } else {
                    Evidence.SourceField.FINE_PRINT
                }
                Evidence(
                    quote = quoteMatch,
                    sourceField = sourceField,
                    key = "local_ai_extracted"
                )
            } else {
                fallback.evidence
            }

            return ClaimResult(
                claim = claim,
                verdict = verdict,
                frontText = claim.rawText,
                evidence = evidence,
                explanationEn = explanation,
                ruleKey = fallback.ruleKey
            )
        } catch (e: Exception) {
            logWarn("Failed to parse model response JSON: $response", e)
            return fallback
        }
    }

    /**
     * Runs internal diagnostic test to prove real model loading, token generation, and latency (Phase 9).
     */
    fun runDiagnostic(context: Context): DiagnosticResult {
        val totalStart = System.currentTimeMillis()

        val model = findAvailableModel(context)
        if (model == null) {
            return DiagnosticResult(
                modelFound = false,
                engineState = EngineState.MODEL_NOT_FOUND,
                errorMessage = "No Gemma model found in app storage (/sdcard/Android/data/com.sachlabel.app/files/models/)"
            )
        }

        if (currentState != EngineState.READY || customRunner == null) {
            val initialized = initialize(context)
            if (!initialized || customRunner == null) {
                return DiagnosticResult(
                    modelFound = true,
                    modelPath = model.file.absolutePath,
                    modelSizeBytes = model.sizeBytes,
                    modelSizeMB = model.sizeMB,
                    engineState = currentState,
                    errorMessage = "Failed to initialize GemmaLocalModelRunner for ${model.fileName}"
                )
            }
        }

        val runner = customRunner ?: return DiagnosticResult(
            modelFound = true,
            modelPath = model.file.absolutePath,
            modelSizeBytes = model.sizeBytes,
            modelSizeMB = model.sizeMB,
            engineState = EngineState.ERROR,
            errorMessage = "Runner is null after initialization"
        )

        // Step 1: Ping Prompt
        val pingPrompt = "Reply with exactly: GEMMA_OK"
        var pingOutput: String? = null
        var pingSuccess = false
        val pingStart = System.currentTimeMillis()

        try {
            pingOutput = runner.generate(pingPrompt).trim()
            pingSuccess = pingOutput.contains("GEMMA_OK", ignoreCase = true)
        } catch (e: Throwable) {
            logWarn("Diagnostic ping failed", e)
            return DiagnosticResult(
                modelFound = true,
                modelPath = model.file.absolutePath,
                modelSizeBytes = model.sizeBytes,
                modelSizeMB = model.sizeMB,
                engineState = EngineState.ERROR,
                pingPrompt = pingPrompt,
                pingOutput = pingOutput,
                pingSuccess = false,
                errorMessage = "Inference ping failed: ${e.message}"
            )
        }
        val pingLatency = System.currentTimeMillis() - pingStart

        // Step 2: Domain Specific Test
        val domainClaim = Claim("No Added Sugar", "no_added_sugar")
        val domainLabel = StructuredLabel(
            ingredients = listOf("Whole Wheat", "Maltitol", "Vegetable Oil"),
            nutritionTable = mapOf("sugars_g" to 0.5),
            finePrint = listOf("Contains artificial sweetener"),
            rawBackText = "Ingredients: Whole Wheat, Maltitol, Vegetable Oil. Sugars: 0.5g. Contains artificial sweetener."
        )
        val domainPrompt = buildStructuredPrompt(domainClaim, domainLabel)
        var domainOutput: String? = null
        var domainVerdict: Verdict? = null
        var domainEvidenceQuote: String? = null
        var domainSuccess = false
        val domainStart = System.currentTimeMillis()

        try {
            domainOutput = runner.generate(domainPrompt)
            val fallback = RuleEngine.check(domainClaim, domainLabel)
            val parsed = parseModelResponse(domainOutput, domainClaim, fallback)
            val validated = EvidenceValidator.validate(parsed, domainLabel.rawBackText)
            domainVerdict = validated.verdict
            domainEvidenceQuote = validated.evidence.quote
            domainSuccess = validated.verdict == Verdict.NEEDS_CONTEXT || validated.verdict == Verdict.MISLEADING
        } catch (e: Throwable) {
            logWarn("Diagnostic domain test failed", e)
        }
        val domainLatency = System.currentTimeMillis() - domainStart
        val totalLatency = System.currentTimeMillis() - totalStart

        return DiagnosticResult(
            modelFound = true,
            modelPath = model.file.absolutePath,
            modelSizeBytes = model.sizeBytes,
            modelSizeMB = model.sizeMB,
            engineState = currentState,
            pingPrompt = pingPrompt,
            pingOutput = pingOutput,
            pingSuccess = pingSuccess,
            pingLatencyMs = pingLatency,
            domainClaim = domainClaim.rawText,
            domainPrompt = domainPrompt,
            domainOutput = domainOutput,
            domainVerdict = domainVerdict,
            domainEvidenceQuote = domainEvidenceQuote,
            domainSuccess = domainSuccess,
            domainLatencyMs = domainLatency,
            totalLatencyMs = totalLatency
        )
    }

    /**
     * Unloads model resources from memory.
     */
    @Synchronized
    fun unload() {
        try {
            customRunner?.close()
        } catch (ignored: Throwable) {}
        customRunner = null
        activeModelFile = null
        activeModelMeta = null
        currentState = EngineState.UNINITIALIZED
        logInfo("Local AI engine unloaded.")
    }
}

/**
 * Pluggable runner interface for on-device inference engines.
 */
interface LocalModelRunner : AutoCloseable {
    fun generate(prompt: String): String
}
