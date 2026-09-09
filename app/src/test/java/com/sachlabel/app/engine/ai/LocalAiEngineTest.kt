package com.sachlabel.app.engine.ai

import com.sachlabel.app.data.model.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive test suite for LocalAiEngine:
 * - model unavailable
 * - model initialization failure
 * - valid local inference
 * - malformed model output
 * - invalid evidence quote (hallucination rejection via EvidenceValidator)
 * - fallback to template explanation
 * - no network dependency
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

    @Test
    fun `test model unavailable gracefully returns deterministic result`() {
        // Ensure engine is uninitialized / no model
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
        assertTrue("Latency should be recorded", outcome.latencyMs >= 0)
    }

    @Test
    fun `test valid local inference executes and cites verified evidence`() {
        val rawBackText = "Ingredients: Green Tea Leaves, Natural Mint, Quality Mark Only."
        val label = StructuredLabel(
            ingredients = listOf("Green Tea Leaves", "Natural Mint"),
            finePrint = listOf("Quality Mark Only"),
            rawBackText = rawBackText
        )
        val claim = Claim(rawText = "100% Natural", patternKey = "100_percent_natural")

        // Mock a valid local model runner adhering to Stage 4/5 JSON schema
        LocalAiEngine.setRunnerForTesting(object : LocalModelRunner {
            override fun generate(prompt: String): String {
                return """
                    {
                      "verdict": "NEEDS_CONTEXT",
                      "explanation": "The product claims 100% Natural, but fine print notes this is a quality mark.",
                      "evidence_quote": "Quality Mark Only"
                    }
                """.trimIndent()
            }
            override fun close() {}
        })

        val outcome = LocalAiEngine.process(claim, label)

        assertTrue("Outcome should be from Local AI", outcome.isFromLocalAi)
        assertEquals(Verdict.NEEDS_CONTEXT, outcome.claimResult.verdict)
        assertEquals("Quality Mark Only", outcome.claimResult.evidence.quote)
        assertTrue(outcome.claimResult.explanationEn.contains("quality mark"))
    }

    @Test
    fun `test malformed model output falls back gracefully to deterministic result`() {
        val rawBackText = "Ingredients: Whole Oats, Invert Sugar Syrup."
        val label = StructuredLabel(
            ingredients = listOf("Whole Oats", "Invert Sugar Syrup"),
            rawBackText = rawBackText
        )
        val claim = Claim(rawText = "No Added Sugar", patternKey = "no_added_sugar")

        // Runner outputs invalid non-JSON output
        LocalAiEngine.setRunnerForTesting(object : LocalModelRunner {
            override fun generate(prompt: String): String {
                return "I think this product has some syrup in it, so be careful!"
            }
            override fun close() {}
        })

        val outcome = LocalAiEngine.process(claim, label)

        // Must not crash; must fall back to deterministic verdict
        assertEquals(Verdict.NEEDS_CONTEXT, outcome.claimResult.verdict)
        assertEquals("Invert Sugar Syrup", outcome.claimResult.evidence.quote)
    }

    @Test
    fun `test hallucinated evidence quote is rejected by EvidenceValidator guardrail`() {
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

    @Test
    fun `test model runtime exception falls back to template explanation without crashing`() {
        val rawBackText = "Ingredients: Milk, Sugar."
        val label = StructuredLabel(
            ingredients = listOf("Milk", "Sugar"),
            rawBackText = rawBackText
        )
        val claim = Claim(rawText = "Immunity Booster", patternKey = "vague_wellness")

        // Runner throws OutOfMemoryError or native crash simulation
        LocalAiEngine.setRunnerForTesting(object : LocalModelRunner {
            override fun generate(prompt: String): String {
                throw RuntimeException("Simulated native model inference failure")
            }
            override fun close() {}
        })

        val outcome = LocalAiEngine.process(claim, label)

        assertFalse("Outcome should flag that AI failed", outcome.isFromLocalAi)
        assertEquals(LocalAiEngine.EngineState.ERROR, outcome.engineState)
        // Deterministic template must still be returned
        assertNotNull(outcome.claimResult)
        assertFalse(outcome.claimResult.explanationEn.isBlank())
    }

    @Test
    fun `test buildStructuredPrompt adheres to prompt templates specification`() {
        val claim = Claim(rawText = "Immunity Booster", patternKey = "vague_wellness")
        val label = StructuredLabel(
            ingredients = listOf("Turmeric Extract", "Black Pepper"),
            nutritionTable = mapOf("energy_kcal" to 120.0),
            finePrint = listOf("*This product is not intended to treat any disease.")
        )

        val prompt = LocalAiEngine.buildStructuredPrompt(claim, label)

        assertTrue(prompt.contains("SYSTEM:"))
        assertTrue(prompt.contains("USER:"))
        assertTrue(prompt.contains("Immunity Booster"))
        assertTrue(prompt.contains("Turmeric Extract"))
        assertTrue(prompt.contains("Black Pepper"))
        assertTrue(prompt.contains("energy_kcal"))
        assertTrue(prompt.contains("evidence_quote"))
    }
}
