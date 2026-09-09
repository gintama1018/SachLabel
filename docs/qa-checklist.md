# QA & Verification Checklist — SachLabel

Run this test suite before any deployment or live physical demonstration.

---

## 1. Physical Packaging & Optical Capture (CameraX)
- [ ] **Dual Capture Sequence**: Captures Front frame, validates thumbnail, and transitions smoothly to Back frame.
- [ ] **Flash Toggle**: Operates correctly for low-light store environments.
- [ ] **Surface Glare Handling**: `ImagePreprocessor` handles mild specular reflection on glossy plastic/foil pouches.
- [ ] **Curved Surfaces**: Successfully captures cylindrical containers (bottles/cans) without perspective crashes.
- [ ] **Retake Flow**: User can retake front or back independently without resetting the full session.

---

## 2. On-Device OCR & Language Scripts (Google ML Kit)
- [ ] **English / Latin Script**: Transcribes dense statutory ingredients and nutrition tables accurately.
- [ ] **Devanagari Script (Hindi/Marathi)**: Recognizes Hindi packaging text (सामग्री, पोषण संबंधी जानकारी) without character corruption.
- [ ] **Mixed-Script Labels**: Accurately processes dual-language English + Hindi packaging.
- [ ] **Small / Dense Fine Print**: Captures footnote legal disclaimers at actual package scale.

---

## 3. Heuristic Layout & Structured Label Extraction
- [ ] **Header Segmentation**: Identifies anchor headers ("Ingredients:", "सामग्री:", "Nutritional Information").
- [ ] **Ingredients Predominance**: Tokenizes comma-delimited ingredients in order of appearance.
- [ ] **Nutrition Raw Lines**: Captures exact verbatim lines for sugar, trans fat, and protein values.
- [ ] **Claim Prominence Scoring**: LayoutAnalyzer identifies the most prominent front claim via bounding-box geometry.

---

## 4. Canonical 8-Rule Deterministic Evaluation
- [ ] **Rule 1 (`no_added_sugar`)**: Distinguishes alternate sugar syrups (maltodextrin, invert syrup, concentrates).
- [ ] **Rule 2 (`100_percent_natural`)**: Flags synthetic flavors, INS preservatives, and qualifying disclaimers.
- [ ] **Rule 3 (`sugar_free`)**: Evaluates against 0.5g/100g threshold; flags replacement non-nutritive sweeteners.
- [ ] **Rule 4 (`no_preservatives`)**: Detects Class II chemical preservatives (INS 200–299).
- [ ] **Rule 5 (`organic`)**: Detects recognized marks (Jaivik Bharat, NPOP, USDA); flags absence cleanly.
- [ ] **Rule 6 (`high_protein`)**: Evaluates protein threshold (>= 12g/100g).
- [ ] **Rule 7 (`zero_trans_fat`)**: Flags partially hydrogenated vegetable oils even when table reports 0g.
- [ ] **Rule 8 (`vague_wellness`)**: Evaluates fine print disclaimers and active botanical supporting ingredients.
- [ ] **Clean Products**: Confirms that compliant products produce `Verdict.CONSISTENT` without false positives.
- [ ] **No Claim Found**: Packaging without one of the 8 canonical claims cleanly emits `NO_CLAIM_DETECTED`.

---

## 5. Evidence-First Safety & Gatekeeper (`EvidenceValidator`)
- [ ] **Verbatim Verification**: Every evidence quote shown in the UI exists character-for-character in the raw OCR text.
- [ ] **Zero Synthetic Quotations**: Rejects fabricated or paraphrased quotes.
- [ ] **Absence Indication**: Missing disclosures use `Evidence.absent()`, never a fake quotation.
- [ ] **Invalid Quote Downgrade**: If an unverified quote is provided, it is stripped and the verdict is downgraded to `NOT_ENOUGH_EVIDENCE`.

---

## 6. Local AI / Gemma Runtime & Fallback (`LocalAiEngine`)
- [ ] **Non-Interference Guarantee**: A failed local AI call does **NOT** destroy the deterministic result.
- [ ] **Missing Model Fallback**: If no model is loaded (`MODEL_NOT_FOUND`), pipeline immediately uses deterministic template explanations.
- [ ] **Initialization Error Handling**: Corrupted model or native crash sets `INITIALIZATION_FAILED` and falls back gracefully.
- [ ] **Model Format Guardrail**: Unsupported formats (e.g. `.gguf`) are rejected before initialization.
- [ ] **SAF File Picker Import**: Model import from phone's `/sdcard/Download/` to app-scoped storage works via Android SAF.
- [ ] **Memory Management**: OCR bitmaps are recycled before large model inference to prevent OutOfMemory crashes.
- [ ] **Diagnostic Ping**: Built-in test ping (`"Reply with exactly: GEMMA_OK"`) executes cleanly when model is loaded.

---

## 7. Speech Audio (TTS) & Accessibility
- [ ] **Multi-Language Voice Playback**: Text-to-Speech speaks explanations in selected language (Hindi, Tamil, Bengali, etc.).
- [ ] **Zero Microphone Permission**: App requires no `RECORD_AUDIO` permission.

---

## 8. Offline Execution & History Isolation
- [ ] **100% Offline Core**: Full capture → OCR → rule evaluation → result flow executes in Airplane Mode.
- [ ] **Local History Persistence**: Audits save to device internal storage without cloud synchronization.
- [ ] **Sample Demo Isolation**: Pre-packaged mock scenarios are strictly flagged as `DEMO DATA` and cannot be confused with real user scans.
