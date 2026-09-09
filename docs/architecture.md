# System Design & Architecture — SachLabel

## 1. Core Product & Design Constraints

SachLabel is an AI-assisted packaging intelligence app that audits a food product's own marketing claims against its statutory ingredient and nutritional declarations.

**Non-Negotiable Constraints:**
- **On-Device Offline-First Core**: Capture, OCR, layout extraction, claim matching, rule evaluation, evidence validation, and TTS run completely on-device without network dependency.
- **Deterministic Supremacy**: The deterministic `RuleEngine` is always the primary decision system. Local AI is never on the critical verification path and never overrides deterministic findings.
- **Zero Synthetic Evidence**: Every cited piece of evidence must exist verbatim in raw OCR text or use `Evidence.absent()`, verified by `EvidenceValidator`.
- **Android/Kotlin Native Target**: Implemented in modern Kotlin with Jetpack Compose, CameraX, and Google ML Kit.

---

## 2. High-Level Multimodal Pipeline

```
┌─────────────────┐     ┌─────────────────┐
│   Front Photo   │     │   Back Photo    │
└────────┬────────┘     └────────┬────────┘
         │                       │
         ▼                       ▼
┌─────────────────────────────────────────┐
│     ImagePreprocessor (EXIF / Contrast) │
└────────────────────┬────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────┐
│  ML Kit OCR (Latin + Devanagari Script) │
└────────┬───────────────────────┬────────┘
         │                       │
         ▼                       ▼
┌──────────────────┐    ┌──────────────────────┐
│  LayoutAnalyzer  │    │    LabelExtractor    │
│  Prominence &    │    │  Ingredients tokens  │
│  Geometry sort   │    │  Nutrition raw lines │
└────────┬─────────┘    └──────────┬───────────┘
         │                         │
         ▼                         │
┌──────────────────┐               │
│   ClaimMatcher   │               │
│  8 Canonical v1  │               │
│  claim categories│               │
└────────┬─────────┘               │
         │                         │
         └───────────┬─────────────┘
                     ▼
       ┌────────────────────────────┐
       │ Deterministic RuleEngine   │
       │ (8 Canonical FSSAI Rules)  │
       └─────────────┬──────────────┘
                     ▼
       ┌────────────────────────────┐
       │ EvidenceValidator          │
       │ (Zero-synthetic gatekeeper)│
       └─────────────┬──────────────┘
                     ▼
       ┌────────────────────────────┐
       │ Optional LocalAiEngine     │
       │ (MediaPipe Gemma 2B INT4)  │
       └─────────────┬──────────────┘
                     ▼
       ┌────────────────────────────┐
       │ Localized Explanation      │
       │ & Android TextToSpeech     │
       └─────────────┬──────────────┘
                     ▼
       ┌────────────────────────────┐
       │ Optional Health Context    │
       │ (Secondary informational)  │
       └────────────────────────────┘
```

---

## 3. Component Details

### 3.1 Optical Processing & OCR
- **ImagePreprocessor**: Handles EXIF rotation normalization, safe downsampling, and adaptive contrast enhancement for glossy/reflective packaging.
- **ML Kit Text Recognition v2**: Runs on-device Latin and Devanagari models simultaneously to support English and Hindi food packaging in India. Preserves bounding boxes for geometric layout analysis.

### 3.2 Layout Analysis & Claim Detection
- **LayoutAnalyzer**: Analyzes front-of-pack text blocks for font size prominence and spatial position, computing composite scores for candidate marketing claims.
- **ClaimMatcher**: Matches candidates against the **closed 8 canonical v1 claim categories** defined in `CanonicalClaimCategory.kt`. Any unhandled claim triggers `Verdict.NO_CLAIM_DETECTED`.

### 3.3 Structured Label Extraction
- **LabelExtractor**: Segments back-of-pack text using multilingual anchors ("Ingredients:", "सामग्री:", "Nutritional Information", "पोषण संबंधी जानकारी").
- Extracts ordered ingredient tokens and captures unparsed raw nutrition lines into `nutritionRawLines` for verbatim evidence citation.

### 3.4 Deterministic Rule Engine
- **RuleEngine**: Evaluates the claim against extracted ingredients, nutrition tables, and fine print using statutory FSSAI rules.
- Fast (<5ms), deterministic, unit-tested code. Emits `CONSISTENT`, `NEEDS_CONTEXT`, or `MISLEADING`.

### 3.5 Evidence Validator Gatekeeper
- **EvidenceValidator**: Strict enforcement gatekeeper. Verifies that any quote attached to a verdict actually exists character-for-character in the raw OCR text.
- If a quote cannot be verified, it is stripped and the verdict is downgraded to `NOT_ENOUGH_EVIDENCE`.

### 3.6 Optional On-Device Local AI (Gemma 2B)
- **LocalAiEngine**: Triggered only when the deterministic verdict is ambiguous (`NEEDS_CONTEXT` or `NOT_ENOUGH_EVIDENCE`) and an authorized on-device model is ready.
- **GemmaLocalModelRunner**: Executes on-device quantized Gemma 2B INT4 models via Google MediaPipe Tasks GenAI (`com.google.mediapipe:tasks-genai:0.10.14`).
- **Model Compatibility Guardrail**: Enforces that only supported MediaPipe bundles (`.bin` or `.task`) are initialized. Incompatible formats like `.gguf` are rejected.
- **Storage Access Framework (SAF) Import**: Provides an in-app file picker flow allowing users to import models from `/sdcard/Download/` to app-scoped storage (`/sdcard/Android/data/com.sachlabel.app/files/models/`).
- **Deterministic Fallback**: If the model is missing, corrupt, or throws an OOM, the engine falls back immediately to deterministic template explanations with zero UI stalls.
- **Validation Status**: On-device Gemma inference runtime is integrated; physical-device inference validation is pending on-device hardware execution.

### 3.7 Regional Explanation & Voice Playback (TTS)
- **ExplanationTemplates**: Maps verified findings to plain-language, non-accusatory explanations.
- **UserLanguage**: Supports English, Hindi, Marathi, Tamil, Telugu, Kannada, Bengali, and Gujarati.
- **TtsManager**: Uses Android's native `TextToSpeech` engine. Requires **no microphone or audio recording permissions**.

### 3.8 Opt-In Secondary Health Context
- **HealthContextEngine**: Optional, secondary informational cross-reference (e.g. flagging sugar alcohols for diabetic preference).
- Explicitly disclaimed: *"This ingredient may be relevant to the concern you mentioned. This is informational, not a medical determination."*

---

## 4. Tech Stack Summary

| Layer | Component | Choice |
|---|---|---|
| **Platform** | Native Android | Kotlin 2.0 / Jetpack Compose / Material 3 |
| **Camera** | Front & Back Capture | AndroidX CameraX 1.3.4 |
| **Vision / OCR** | On-Device Text Recognition | Google ML Kit (Latin + Devanagari) |
| **Layout & Extraction** | Geometry & Token Parsing | Plain Kotlin (`LayoutAnalyzer`, `LabelExtractor`) |
| **Claim Taxonomy** | Bounded 8 Categories | `CanonicalClaimCategory.kt` (Single source of truth) |
| **Decision Logic** | Statutory Verification | Plain Kotlin (`RuleEngine.kt`) |
| **Evidence Safety** | Quote Gatekeeper | `EvidenceValidator.kt` (Zero synthetic quotes) |
| **Local SLM Runtime** | Optional Ambiguity Reasoning | Google MediaPipe Tasks GenAI 0.10.14 (`GemmaLocalModelRunner`) |
| **Speech Audio** | Spoken Verdicts | Android `TextToSpeech` API (Local on-device) |
| **Persistence** | Local Scan History | DataStore Preferences & JSON internal storage |
| **Network & Privacy** | Core Flow | **100% On-Device / Zero Cloud Dependency** |

---

## 5. What SachLabel Is vs What SachLabel Is Not

| SachLabel Is | SachLabel Is Not |
|---|---|
| A physical-package claim auditing tool | A general 0–100 health-scoring app |
| An evidence-first label interpreter | A medical diagnosis or treatment tool |
| Grounded in verbatim back-of-pack OCR text | A barcode-only database lookup |
| Deterministic verification with optional local AI | An arbitrary "safe / unsafe" classifier |
| Regional-language spoken explanation | An official government certification authority |
