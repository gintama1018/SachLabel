# System Design & Architecture
## Claim ↔ Label Contradiction Detector

*Note: "system design" and "architecture" are combined into this single document — splitting them would produce two largely overlapping documents at hackathon scope.*

## 1. Design Constraints

- **On-device first.** Core flow (capture → OCR → claim detection → rule matching → explanation) should run without a network call, for both the hackathon's on-device evaluation axis and genuine privacy benefit (product photos never need to leave the phone).
- **Android/Kotlin target**, consistent with existing stack and tooling familiarity.
- **Two-photo input** (front, back) rather than one ambiguous shot — trades a small UX cost for a large reduction in layout-detection complexity (see Risks, §6).
- **Rule engine before LLM**, not LLM-only — reduces hallucination risk on claims that carry real-world weight.

## 2. High-Level Pipeline

```
┌─────────────────┐     ┌─────────────────┐
│  Front photo     │     │  Back photo      │
└────────┬─────────┘     └────────┬─────────┘
         │                        │
         ▼                        ▼
┌─────────────────────────────────────────────┐
│              OCR (on-device)                 │
│   ML Kit Text Recognition v2 (Latin +        │
│   Devanagari/regional script support)        │
└────────┬───────────────────────┬─────────────┘
         │                       │
         ▼                       ▼
┌─────────────────┐     ┌──────────────────────┐
│ Claim Detection  │     │ Structured Label      │
│ (front text)     │     │ Extraction (back text)│
│ — matches against│     │ — ingredients list,   │
│ hardcoded claim  │     │ nutrition table,      │
│ pattern list     │     │ disclaimer text       │
└────────┬─────────┘     └──────────┬────────────┘
         │                          │
         └───────────┬──────────────┘
                      ▼
         ┌────────────────────────────┐
         │  Claim ↔ Evidence Matching  │
         │  Rule/semantic engine per   │
         │  claim type (see §4)        │
         └────────────┬────────────────┘
                       ▼
         ┌────────────────────────────┐
         │  Conflict/Qualification     │
         │  Detection → result state   │
         └────────────┬────────────────┘
                       ▼
         ┌────────────────────────────┐
         │  Explanation Generation     │
         │  Small on-device LLM,       │
         │  template-constrained       │
         └────────────┬────────────────┘
                       ▼
         ┌────────────────────────────┐
         │  Regional language render + │
         │  on-device TTS              │
         └────────────────────────────┘
                       │
                       ▼
              ┌────────────────┐
              │  (Optional)     │
              │  Opt-in health  │
              │  context layer  │
              └────────────────┘
```

## 3. Component Detail

### 3.1 OCR
- **ML Kit Text Recognition v2** (on-device, free, supports Latin + Devanagari script). Handles most Indian packaging out of the box.
- Fallback: Tesseract (on-device) for scripts ML Kit doesn't cover, if needed for a specific regional language demo.

### 3.2 Claim Detection
- v1 approach: **heuristic, not ML-heavy.** Front-photo text is scanned against the hardcoded claim pattern list (PRD §7.3) using fuzzy string matching (handles OCR noise) rather than a trained classifier — faster to build, more debuggable, appropriate for a bounded taxonomy.
- Layout heuristic: prioritize the largest-font/most-prominent text block on the front photo as the likely claim (ML Kit returns bounding boxes and can approximate relative text size).

### 3.3 Structured Label Extraction (back photo)
- Segment OCR output into: ingredients list, nutrition table (if present), disclaimer/fine-print text.
- Simple heuristic segmentation (keyword anchors like "Ingredients:", "Nutrition Facts," "Nutritional Information") is sufficient for v1 — full generalized table parsing is a v2+ problem.

### 3.4 Claim ↔ Evidence Matching (rule engine)
- Each entry in the claim taxonomy maps to a **specific check function**, e.g.:
  - `"no added sugar"` → check nutrition table sugar value > threshold, OR check ingredients for sweetener keywords (glucose syrup, high-fructose corn syrup, etc.)
  - `"100% natural"` → check ingredients for synthetic/artificial-flagged keywords, AND check disclaimer text for qualifying language ("quality mark," "does not guarantee," etc.)
- Each check function returns: `{result: contradiction | qualification | clean, cited_front_text, cited_back_text}`.
- This is deterministic, testable code — no LLM involved in the actual detection logic.

### 3.5 Explanation Generation
- Small on-device LLM (e.g., Gemma 2B or Phi-3-mini, quantized, run via MediaPipe LLM Inference API or TFLite) takes the rule engine's structured output (`{claim, cited_text, result_type}`) and generates the plain-language sentence.
- **Constrain with a template, not free generation** — give the model a fixed sentence structure per result_type (contradiction / qualification / clean) with only the specific claim and cited text as variables. This bounds hallucination risk to near-zero since the model is filling a template, not reasoning from scratch.
- This is a much narrower and more reliable task than open-ended reasoning — appropriate for a small quantized model.

### 3.6 Regional Language + TTS
- UI text: maintain a translation string table per supported language (standard Android localization, `strings.xml` per locale) for all fixed UI copy.
- Explanation templates: maintain per-language versions of the templates from §3.5 (translation happens at the template level, not by translating LLM output live — more reliable, avoids double-translation errors).
- TTS: Android's built-in `TextToSpeech` API supports Hindi and several major Indian regional languages on-device already — no custom TTS model needed for MVP. Confirm language/voice availability per target device during testing.

### 3.7 Opt-in Health Context Layer
- Simple keyword cross-reference: stated condition (from preset or free text) → known ingredient-relevance mapping (e.g., "diabetic" → flag sugar/sweetener-related ingredients already extracted in §3.3).
- Output always passes through the fixed disclaimer template (design.md §4) — this is enforced at the template level, not left to model discretion.

## 4. Tech Stack Summary

| Layer | Choice |
|---|---|
| Platform | Android / Kotlin |
| OCR | ML Kit Text Recognition v2 (+ Tesseract fallback if needed) |
| On-device LLM runtime | MediaPipe LLM Inference API or TFLite, running Gemma 2B / Phi-3-mini (quantized) |
| Rule engine | Plain Kotlin logic — deterministic check functions per claim type |
| TTS | Android `TextToSpeech` API (built-in, on-device, multi-language) |
| Localization | Standard Android string resources per locale |

## 5. Data Flow & Privacy

- Photos processed entirely on-device for the core flow; no image upload required for v1.
- If a cloud fallback is ever added (e.g., for claim types beyond the local taxonomy), it should be explicitly opt-in and disclosed — not a silent fallback, consistent with the product's own evidence-transparency principle.

## 6. Known Risks / Open Problems

1. **Claim prominence detection is a heuristic, not solved CV.** Bounding-box size as a proxy for "the claim" will fail on unusual layouts (e.g., a small claim in a colored badge). Test against varied real packaging, not just clean flat-lay photos.
2. **OCR robustness** under glare, curved surfaces (bottles/pouches), and multilingual mixed text needs real-world testing before the demo, not just controlled-lighting test shots.
3. **Claim taxonomy is intentionally narrow.** Expanding it requires manual curation per claim type — this is a content/legal-research bottleneck, not an engineering one, and should be scoped accordingly for anything beyond the hackathon MVP.
4. **Template-constrained LLM output** trades some naturalness of phrasing for reliability — acceptable tradeoff at this stage, but worth flagging as a deliberate choice if judges ask why explanations feel structured rather than freely generated.

## 7. Path Beyond v1

- v2 (personal care) and v3 (supplements) reuse the same pipeline with a new claim taxonomy and ingredient-keyword set per category — no architectural change required.
- v4 (medicines/OTC) requires a separate safety and accuracy review before reusing this architecture — flagged in PRD as roadmap-only, not a natural extension to build casually.
