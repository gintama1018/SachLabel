# Plan — User Flows
## Claim ↔ Label Contradiction Detector

This document defines every user-facing flow for the v1 MVP. Each flow lists trigger → steps → exit states.

---

## Flow 0 — First Launch / Onboarding

**Trigger:** App opened for the first time.

1. Welcome screen — one-line product statement: *"We check if a product's claims match its own label."*
2. Language selection screen — user picks their preferred language (English, Hindi, Marathi, Tamil, Telugu, Kannada, Bengali, Gujarati). This selection drives both on-screen text and TTS voice.
3. Optional: 3-screen explainer (claim → contradiction → evidence) — skippable.
4. Land on Home / Scan screen.

**Exit state:** Home screen, language set, ready to scan.

---

## Flow 1 — Core Scan Flow (primary flow)

**Trigger:** User taps "Scan a product" from Home.

1. **Capture front** — camera opens with an on-screen guide frame + hint text: *"Photograph the front of the pack."*
2. User captures front photo → auto-advances.
3. **Capture back** — same camera UI, hint text: *"Now the back — ingredients or nutrition label."*
4. User captures back photo.
5. **Processing screen** — on-device pipeline runs:
   - ML Kit OCR (Latin + Devanagari) + geometry extraction
   - Layout analysis & structured label segmentation
   - Canonical claim matching (`ClaimMatcher`)
   - Deterministic rule check (`RuleEngine`)
   - Zero-synthetic evidence verification (`EvidenceValidator`)
   - Optional local Gemma explanation (`LocalAiEngine`)
6. **Result screen** — one of four states (see Flow 2–5 below).

**Exit state:** Result screen shown.

**Edge cases:**
- Blurry/unreadable photo → prompt to retake before proceeding to processing.
- No claim detected on front photo at all → route to Flow 5 (`NOT_ENOUGH_EVIDENCE` / `NO_CLAIM_DETECTED`).

---

## Flow 2 — Result: Contradiction Found (`MISMATCH`)

1. Result header: **"⚠️ Claim needs context"** (audit finding, not an accusation).
2. Front claim shown as captured text: *"100% Natural"*
3. Back evidence shown as captured text (the exact qualifying/contradicting text verified by `EvidenceValidator`).
4. Plain-language explanation: *"The front claim may create a broader impression than the detailed label supports."*
5. "🔊 Listen" button — plays the explanation via native Android TTS in the selected language.
6. CTA: **"Want to check what this means for you?"** → Flow 6 (opt-in health context).
7. Secondary action: "Scan another product."

---

## Flow 3 — Result: Qualification Found (`QUALIFIED`)

Same layout as Flow 2, but framed as a narrower-than-implied claim rather than a direct contradiction (e.g., "no added sugar" claim is technically true but the product is naturally sugar-heavy). Explanation copy differs; UI structure is identical.

---

## Flow 4 — Result: No Issue Found (`VERIFIED`)

1. Result header: **"✓ No contradiction found for this claim."**
2. Front claim shown.
3. One-line note on what was checked against: *"Checked against ingredients and nutrition label — no conflicting information found."*
4. Still offers "🔊 Listen" and "Want to check what this means for you?" — the opt-in health layer is available regardless of whether a contradiction was found.

---

## Flow 5 — Result: Insufficient Evidence or No Claim Detected (`NOT_ENOUGH_EVIDENCE`)

1. Result header: **"No prominent claim detected or unverified evidence."**
2. Short explanation: this product doesn't carry one of the 8 canonical claim types this version checks for, or package fine print was unreadable.
3. CTA: "Scan another product."

*(This is a legitimate, expected outcome — not an error. Framing it as a limitation of the product's checklist, not a failure, matters for user trust.)*

---

## Flow 6 — Opt-In Health Context (secondary layer)

**Trigger:** User taps "Want to check what this means for you?" from any result screen.

1. Prompt: "Add a dietary goal, allergy, or condition (optional)." Free text or a short preset list (diabetic, high blood pressure, gluten-free, nut allergy, etc.) — presets reduce typing burden for the same low-literacy persona this product targets.
2. User selects/enters context.
3. System cross-references extracted ingredients against the stated context.
4. Result shown with **mandatory disclaimer framing**, every time, no exceptions:
   *"This ingredient may be relevant to the concern you mentioned. This is informational, not a medical determination."*
5. No numeric score, no "safe/unsafe" binary — ever, in this flow.

**Exit state:** Return to result screen or Home.

---

## Flow 7 — TTS Playback (cross-cutting)

Available from any result screen via the 🔊 icon.

1. Tap → plays explanation text in the selected language via on-device TTS.
2. Playback controls: play/pause, replay.
3. If TTS is unavailable for the selected language on-device, fall back to text-only with a visible note — never fail silently.

---

## Flow 8 — Settings / Change Language

1. Accessible from Home at any time (not buried).
2. Change language → updates both UI text and TTS voice going forward.

---

## Flow Summary Diagram

```
Onboarding → Language select
      ↓
   Home / Scan
      ↓
Capture front → Capture back
      ↓
   Processing
      ↓
   ┌─────────────┬─────────────┬─────────────┐
Contradiction  Qualification  No issue   No claim
   found          found        found     detected
      └─────┬───────┴──────┬──────┘
            ↓              
   [🔊 Listen] [Want to check what this means for you?]
            ↓
   Opt-in health context (always disclaimed)
```
