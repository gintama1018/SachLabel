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
 */
class GemmaLocalModelRunner(
    context: Context,
    val modelFile: File
) : LocalModelRunner {

    private val llmInference: LlmInference

    init {
        require(modelFile.exists()) { "Model file does not exist: ${modelFile.absolutePath}" }
        require(modelFile.canRead()) { "Model file is not readable: ${modelFile.absolutePath}" }

        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelFile.absolutePath)
            .setMaxTokens(384)
            .setTemperature(0.1f) // Constrained temperature for factual regulatory consistency
            .setTopK(40)
            .build()

        llmInference = LlmInference.createFromOptions(context, options)
    }

    override fun generate(prompt: String): String {
        return llmInference.generateResponse(prompt)
    }

    override fun close() {
        try {
            llmInference.close()
        } catch (_: Throwable) {
            // Ignored on teardown
        }
    }
}
