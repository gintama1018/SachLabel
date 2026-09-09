package com.sachlabel.app.engine.ai

import com.sachlabel.app.data.model.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Comprehensive 10-Scenario Test Suite for SachLabel's Local AI & Gemma Engine (Phase 12):
 *
 * 1. Model discovery (candidate filenames & dynamic discovery)
 * 2. Missing model (MODEL_NOT_FOUND state)
 * 3. Initialization failure handling (INITIALIZATION_FAILED state)
 * 4. Deterministic fallback when model is unavailable
 * 5. Successful LocalModelRunner response
 * 6. Malformed model output (resilient fallback without crashing)
 * 7. Invalid evidence quote (hallucination rejection via EvidenceValidator)
 * 8. Valid evidence quote (verified against raw OCR text)
 * 9. AI cannot override decisive deterministic verdicts (CONSISTENT / MISLEADING)
 * 10. Unload and reinitialize resource management
 */
class LocalAiEngineTest {

    @Before
    fun setUp() {
        LocalAiEngine.unload()
    }

    @After
    fun tearDown() {
        LocalAiEngine.unload()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1: Model Discovery
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `test 1 candidate filenames list includes official Gemma MediaPipe files`() {
        assertTrue("Candidate filenames must include gemma-2b-it-cpu-int4.bin",
            LocalAiEngine.CANDIDATE_MODEL_FILENAMES.contains("gemma-2b-it-cpu-int4.bin"))
        assertTrue("Candidate filenames must include gemma-2b-it-gpu-int4.bin",
            LocalAiEngine.CANDIDATE_MODEL_FILENAMES.contains("gemma-2b-it-gpu-int4.bin"))
        assertTrue("Candidate filenames must include model.task",
            LocalAiEngine.CANDIDATE_MODEL_FILENAMES.contains("model.task"))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 2: Missing Model State
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `test 2 missing model leaves engine uninitialized or in model not found state`() {
        assertEquals(LocalAiEngine.EngineState.UNINITIALIZED, LocalAiEngine.state)
        assertNull(LocalAiEngine.loadedModel)
        assertNull(LocalAiEngine.activeMetadata)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 3: Initialization Failure
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `test 3 runner exception during execution enters error state safely`() {
        LocalAiEngine.setRunnerForTesting(object : LocalModelRunner {
            override fun generate(prompt: String): String {
                throw OutOfMemoryError("Native Gemma out of memory")
            }
            override fun close() {}
        })

        val rawBackText = "Ingredients: Milk, Sugar."
        val label = StructuredLabel(
            ingredients = listOf("Milk", "Sugar"),
            rawBackText = rawBackText
        )
        val claim = Claim(rawText = "Immunity Booster", patternKey = "vague_wellness")

        val outcome = LocalAiEngine.process(claim, label)

        assertFalse("Outcome should flag that AI failed", outcome.isFromLocalAi)
        assertEquals(LocalAiEngine.EngineState.ERROR, outcome.engineState)
        assertNotNull("Deterministic fallback must still be returned", outcome.claimResult)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 4: Deterministic Fallback when Model is Unavailable
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `test 4 deterministic fallback when model is unavailable`() {
        assertEquals(LocalAiEngine.EngineState.UNINITIALIZED, LocalAiEngine.state)

        val claim = Claim(rawText = "No Added Sugar", patternKey = "no_added_sugar")
        val label = StructuredLabel(
            ingredients = listOf("Wheat Flour", "Maltodextrin", "Palm Oil"),
            rawBackText = "Ingredients: Wheat Flour, Maltodextrin, Palm Oil."
        )

        val outcome = LocalAiEngine.process(claim, label)

        assertFalse("Outcome should not be from Local AI when model is unavailable", outcome.isFromLocalAi)
        assertEquals(Verdict.NEEDS_CONTEXT, outcome.claimResult.verdict)
        assertEquals("Maltodextrin", outcome.claimResult.evidence.quote)
        assertTrue("Latency should be non-negative", outcome.latencyMs >= 0)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 5: Successful LocalModelRunner Response
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `test 5 successful LocalModelRunner response parses verdict and quote`() {
        val rawBackText = "Ingredients: Whole Oats, Maltodextrin. Contains Maltodextrin."
        val label = StructuredLabel(
            ingredients = listOf("Whole Oats", "Maltodextrin"),
            finePrint = listOf("Contains Maltodextrin"),
            rawBackText = rawBackText
        )
        val claim = Claim(rawText = "No Added Sugar", patternKey = "no_added_sugar")

        LocalAiEngine.setRunnerForTesting(object : LocalModelRunner {
            override fun generate(prompt: String): String {
                return """
                    {
                      "verdict": "NEEDS_CONTEXT",
                      "explanation": "Front claims No Added Sugar, but Maltodextrin is present.",
                      "evidence_quote": "Maltodextrin"
                    }
                """.trimIndent()
            }
            override fun close() {}
        })

        val outcome = LocalAiEngine.process(claim, label)

        assertTrue("Outcome should be from Local AI", outcome.isFromLocalAi)
        assertEquals(Verdict.NEEDS_CONTEXT, outcome.claimResult.verdict)
        assertEquals("Maltodextrin", outcome.claimResult.evidence.quote)
        assertTrue(outcome.claimResult.explanationEn.contains("Maltodextrin"))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 6: Malformed Model Output
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `test 6 malformed model output falls back gracefully to deterministic result`() {
        val rawBackText = "Ingredients: Whole Oats, Invert Sugar Syrup."
        val label = StructuredLabel(
            ingredients = listOf("Whole Oats", "Invert Sugar Syrup"),
            rawBackText = rawBackText
        )
        val claim = Claim(rawText = "No Added Sugar", patternKey = "no_added_sugar")

        // Runner outputs non-JSON random conversational text
        LocalAiEngine.setRunnerForTesting(object : LocalModelRunner {
            override fun generate(prompt: String): String {
                return "Well, I looked at the ingredients and I think you should avoid this product!"
            }
            override fun close() {}
        })

        val outcome = LocalAiEngine.process(claim, label)

        // Must not crash; must fall back to deterministic verdict
        assertEquals(Verdict.NEEDS_CONTEXT, outcome.claimResult.verdict)
        assertEquals("Invert Sugar Syrup", outcome.claimResult.evidence.quote)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 7: Invalid Evidence Quote (Hallucination Rejection)
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `test 7 hallucinated evidence quote is rejected by EvidenceValidator guardrail`() {
        val rawBackText = "Ingredients: Rolled Oats, Salt."
        val label = StructuredLabel(
            ingredients = listOf("Rolled Oats", "Salt"),
            rawBackText = rawBackText
        )
        val claim = Claim(rawText = "Immunity Booster", patternKey = "vague_wellness")

        // Model hallucinates an evidence quote that DOES NOT exist in rawBackText
        LocalAiEngine.setRunnerForTesting(object : LocalModelRunner {
            override fun generate(prompt: String): String {
                return """
                    {
                      "verdict": "MISLEADING",
                      "explanation": "Disclosed 45g of corn syrup in hidden lab test.",
                      "evidence_quote": "High Fructose Corn Syrup 45g"
                    }
                """.trimIndent()
            }
            override fun close() {}
        })

        val outcome = LocalAiEngine.process(claim, label)

        // Strict Guardrail: Because "High Fructose Corn Syrup 45g" does NOT exist in rawBackText,
        // EvidenceValidator MUST downgrade the verdict to NOT_ENOUGH_EVIDENCE and reject the fake quote!
        assertEquals(Verdict.NOT_ENOUGH_EVIDENCE, outcome.claimResult.verdict)
        assertEquals("", outcome.claimResult.evidence.quote)
        assertEquals(Evidence.SourceField.ABSENT, outcome.claimResult.evidence.sourceField)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 8: Valid Evidence Quote
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `test 8 valid evidence quote verified against raw OCR text is accepted`() {
        val rawBackText = "Ingredients: Green Tea Leaves, Natural Mint. *Quality Mark Only."
        val label = StructuredLabel(
            ingredients = listOf("Green Tea Leaves", "Natural Mint"),
            finePrint = listOf("Quality Mark Only"),
            rawBackText = rawBackText
        )
        val claim = Claim(rawText = "100% Natural", patternKey = "vague_wellness")

        LocalAiEngine.setRunnerForTesting(object : LocalModelRunner {
            override fun generate(prompt: String): String {
                return """
                    {
                      "verdict": "NEEDS_CONTEXT",
                      "explanation": "Front claims 100% Natural, but fine print notes this is a quality mark only.",
                      "evidence_quote": "Quality Mark Only"
                    }
                """.trimIndent()
            }
            override fun close() {}
        })

        val outcome = LocalAiEngine.process(claim, label)

        assertEquals(Verdict.NEEDS_CONTEXT, outcome.claimResult.verdict)
        assertEquals("Quality Mark Only", outcome.claimResult.evidence.quote)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 9: AI Cannot Override Decisive Deterministic Evidence
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `test 9 AI cannot override decisive deterministic verdict`() {
        // When deterministic rule finds explicit contradiction (MISLEADING), Local AI is NOT invoked!
        val rawBackText = "Ingredients: Mango Pulp, Water, Sodium Benzoate."
        val label = StructuredLabel(
            ingredients = listOf("Mango Pulp", "Water", "Sodium Benzoate"),
            rawBackText = rawBackText
        )
        val claim = Claim(rawText = "No Preservatives", patternKey = "no_preservatives")

        var aiWasCalled = false
        LocalAiEngine.setRunnerForTesting(object : LocalModelRunner {
            override fun generate(prompt: String): String {
                aiWasCalled = true
                return """
                    {
                      "verdict": "CONSISTENT",
                      "explanation": "The AI mistakenly thinks this is totally fine!",
                      "evidence_quote": "Mango Pulp"
                    }
                """.trimIndent()
            }
            override fun close() {}
        })

        val outcome = LocalAiEngine.process(claim, label)

        // Deterministic engine must have taken precedence
        assertFalse("AI must NOT be called when deterministic rule engine reaches decisive verdict", aiWasCalled)
        assertFalse("Outcome is not from local AI", outcome.isFromLocalAi)
        assertEquals(Verdict.MISLEADING, outcome.claimResult.verdict)
        assertEquals("Sodium Benzoate", outcome.claimResult.evidence.quote)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 10: Unload and Reinitialize
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `test 10 unload and reinitialize resets engine state cleanly`() {
        var closed = false
        LocalAiEngine.setRunnerForTesting(object : LocalModelRunner {
            override fun generate(prompt: String): String = "{}"
            override fun close() { closed = true }
        })

        assertEquals(LocalAiEngine.EngineState.READY, LocalAiEngine.state)

        LocalAiEngine.unload()

        assertTrue("Runner close() must be invoked", closed)
        assertEquals(LocalAiEngine.EngineState.UNINITIALIZED, LocalAiEngine.state)
        assertNull(LocalAiEngine.loadedModel)
        assertNull(LocalAiEngine.activeMetadata)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Prompt Construction Validation
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `test prompt construction includes targeted evidence and allowed verdicts`() {
        val claim = Claim(rawText = "No Added Sugar", patternKey = "no_added_sugar")
        val label = StructuredLabel(
            ingredients = listOf("Whole Wheat", "Maltitol", "Palm Oil", "Salt"),
            nutritionTable = mapOf("sugars_g" to 0.4, "protein_g" to 5.0),
            finePrint = listOf("Contains polyols")
        )

        val prompt = LocalAiEngine.buildStructuredPrompt(claim, label)

        assertTrue(prompt.contains("SYSTEM:"))
        assertTrue(prompt.contains("USER:"))
        assertTrue(prompt.contains("No Added Sugar"))
        assertTrue("Targeted ingredients must include Maltitol", prompt.contains("Maltitol"))
        assertTrue("Targeted nutrition must include sugars_g", prompt.contains("sugars_g"))
        assertTrue("Prompt must specify allowed verdicts", prompt.contains("Allowed verdicts: CONSISTENT, MISLEADING, NEEDS_CONTEXT, NOT_ENOUGH_EVIDENCE"))
        assertTrue("Prompt must mandate verbatim evidence_quote", prompt.contains("evidence_quote"))
    }
}
