package com.sachlabel.app.engine.ai

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import java.io.File

/**
 * Real On-Device Gemma Inference Runner.
 *
 * Sourced from Google MediaPipe Tasks GenAI SDK.
 * Executes on-device quantized Gemma 2B (.bin or .task) models locally on Android CPU/GPU.
 *
 * Conforms to [LocalModelRunner] contract:
 * - Real inference via [LlmInference.generateResponse]
 * - Proper cleanup via [close]
 * - Zero cloud, zero network, 100% on-device
 * - Explicit latency and execution logging
 */
class GemmaLocalModelRunner(
    context: Context,
    val modelFile: File
) : LocalModelRunner {

    private val llmInference: LlmInference

    companion object {
        private const val TAG = "GemmaLocalModelRunner"
        private fun logInfo(msg: String) = try { android.util.Log.i(TAG, msg) } catch (_: Throwable) { println("[$TAG] $msg") }
        private fun logError(msg: String, t: Throwable? = null) = try { android.util.Log.e(TAG, msg, t) } catch (_: Throwable) { System.err.println("[$TAG] $msg: $t") }
    }

    init {
        logInfo("[Runner Creation] Initializing GemmaLocalModelRunner with file: ${modelFile.absolutePath} (${modelFile.length()} bytes)")
        require(modelFile.exists()) { "Model file does not exist: ${modelFile.absolutePath}" }
        require(modelFile.canRead()) { "Model file is not readable: ${modelFile.absolutePath}" }

        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelFile.absolutePath)
            .setMaxTokens(384)
            .setTemperature(0.1f) // Constrained temperature for factual regulatory consistency
            .setTopK(40)
            .build()

        logInfo("[Model Initialization] Calling LlmInference.createFromOptions() on device context")
        llmInference = LlmInference.createFromOptions(context, options)
        logInfo("[Model Initialization] LlmInference engine successfully created and ready for inference")
    }

    override fun generate(prompt: String): String {
        logInfo("[Inference Start] Executing real LlmInference.generateResponse() - promptLength=${prompt.length} chars")
        val startTime = System.currentTimeMillis()
        val response = llmInference.generateResponse(prompt)
        val latencyMs = System.currentTimeMillis() - startTime
        logInfo("[Inference Completion] LlmInference output received in ${latencyMs}ms - outputLength=${response.length} chars")
        return response
    }

    override fun close() {
        try {
            logInfo("[Cleanup] Closing LlmInference engine resources")
            llmInference.close()
        } catch (t: Throwable) {
            logError("[Cleanup] Error while closing LlmInference", t)
        }
    }
}
