# Prompt Templates & Local AI Architecture — SachLabel

This document details the on-device prompt construction used by `com.sachlabel.app.engine.ai.LocalAiEngine`.

---

## 1. Active Architecture: On-Device Constrained Local AI

In the current SachLabel implementation:
- **Stages 1–3 are 100% Deterministic on-device code**:
  - Image Preprocessing: `com.sachlabel.app.cv.ImagePreprocessor`
  - Text Recognition: `com.sachlabel.app.ocr.MlKitOcrProcessor` (ML Kit Latin + Devanagari)
  - Structured Parsing: `com.sachlabel.app.ocr.LabelExtractor`
  - Claim Detection: `com.sachlabel.app.ocr.ClaimMatcher` + `LayoutAnalyzer`
  - Decision Logic: `com.sachlabel.app.engine.RuleEngine`
  - Evidence Verification: `com.sachlabel.app.engine.EvidenceValidator`
- **Stage 4 (Optional Local AI)** is powered on-device by `LocalAiEngine` using `GemmaLocalModelRunner` via Google MediaPipe Tasks GenAI (`LlmInference`).

---

## 2. Active Prompt Template (`LocalAiEngine.kt`)

When a claim produces an ambiguous deterministic verdict (`NEEDS_CONTEXT` or `NOT_ENOUGH_EVIDENCE`) and a supported on-device model is ready, `LocalAiEngine.buildStructuredPrompt()` generates a targeted prompt.

### Targeted Evidence Filtering
To conserve mobile memory and reduce inference latency, the engine filters the full label to only claim-relevant text (e.g. alternate sweeteners for sugar claims, protein sources for protein claims) before generating the prompt:

```text
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
Claim: "{claim_text}" (Category: {pattern_key})
Preliminary Analysis: Verdict={preliminary_verdict} ("{preliminary_explanation}")
Relevant Ingredients: ["{relevant_ingredient_1}", "{relevant_ingredient_2}"]
Relevant Nutrition: {"{nutrient_key}": {value}}
Relevant Fine Print: ["{relevant_fine_print}"]
```

### Expected Output Schema
The model is constrained to strict JSON output:
```json
{
  "verdict": "NEEDS_CONTEXT",
  "explanation": "Front claims No Added Sugar, but Maltodextrin is disclosed in the ingredients.",
  "evidence_quote": "Maltodextrin"
}
```

### Safety Guardrail Post-Processing
1. **JSON Defensive Parsing**: Extracted via resilient regex to handle markdown backticks and formatting quirks.
2. **EvidenceValidator Gatekeeper**: `EvidenceValidator.validate(result, rawBackText)` verifies that `evidence_quote` is a verbatim substring of the raw OCR text. If it fails, the quote is stripped and the verdict is downgraded to `NOT_ENOUGH_EVIDENCE`.
3. **Deterministic Fallback**: If the local AI model is unavailable, corrupt, throws an OOM, or outputs invalid JSON, `LocalAiEngine` immediately falls back to the deterministic template explanation without stalling or crashing.

---

## 3. Legacy Prototype Prompts (Historical Reference Only)

> [!NOTE]
> The prompts below were used during initial cloud-based prototyping (Claude API) before SachLabel was ported to on-device ML Kit OCR and MediaPipe Tasks GenAI. They are retained here solely for architectural lineage and are **NOT** used in the active Android application.

### Legacy Stage 1 — Cloud Vision OCR (Superseded by ML Kit)
```text
SYSTEM:
Extract all readable text from this packaging image grouped by location: top, middle, bottom, fine print.
```

### Legacy Stage 2 — Cloud Label Extraction (Superseded by LabelExtractor.kt)
```text
SYSTEM:
Convert raw text blocks into JSON: front_claims_raw, ingredients, nutrition_table, fine_print.
```

### Legacy Stage 3 — Cloud Claim Detection (Superseded by ClaimMatcher.kt)
```text
SYSTEM:
Identify checkable marketing claims from front_claims_raw.
```
