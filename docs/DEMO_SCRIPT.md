# Demo Script — SachLabel (for Judges)

Target: ~60–90 seconds core demo + 30–45 seconds framing.
Rehearse with the exact physical packaged product you bring to the table.

---

## 0. Pre-Demo Checklist

- [ ] 1–2 real physical food packages tested end-to-end (e.g. Real Fruit Juice or a Protein Bar).
- [ ] Phone charged, app open to Home screen.
- [ ] Good ambient lighting or CameraX flash toggle verified to avoid excessive glare.
- [ ] Pre-packaged Offline Mock Scenario available as backup in case of adverse table lighting.

---

## 1. The Opening Hook (10 seconds)

> "Every packaged food product makes a promise on the front. We built SachLabel to audit that promise directly against what the product's own back label actually declares — and show the exact verbatim words that prove it, in the consumer's own language."

---

## 2. Live Core Demonstration (60 seconds)

1. **Hold up the physical product** (e.g., a juice carton):
   *"The front prominently claims 'No Added Sugar'."*
2. **Capture Front Photo**:
   Tap capture. Reticle aligns with the front face.
3. **Capture Back Photo**:
   Flip package and capture ingredients and nutritional table.
4. **Narrate during on-device processing**:
   *"The app runs on-device ML Kit OCR, segments ingredients and nutrition lines, and checks the claim against deterministic verification rules. EvidenceValidator ensures zero synthetic quotes, and our local Gemma engine assists with a plain-language explanation."*
5. **Point to Verdict Badge & Evidence**:
   Verdict displays `QUALIFIED` or `MISMATCH`.
   *"Look at the evidence card. It cites the exact line from the back: 'Reconstituted Apple Juice Concentrate'. That is not an AI guess — that is the brand's own verbatim declaration verified by EvidenceValidator."*
6. **Trigger Spoken Audio (TTS)**:
   Tap the speaker icon. Listen to the 1-sentence plain-language explanation in Hindi (or chosen regional language).
   *"For a shopper who cannot comfortably read dense English fine print in a supermarket aisle, this is explained aloud clearly in their preferred language."*

---

## 3. Optional Personalization Beat (10 seconds, if time permits)

> "If a user chooses to set a dietary preference like diabetes, SachLabel adds a secondary informational note: 'This ingredient may be relevant to the concern you mentioned. This is informational, not a medical determination.' We never give medical directives."

---

## 4. Closing Line (15 seconds)

> "SachLabel does not calculate an arbitrary 0–100 health score. It does not guess. Every verdict is backed by an exact quote verified against the package in the shopper's hand."

---

## 5. Anticipated Judge Questions & Technical Answers

- **"How does this differ from barcode scanners like Yuka?"**
  → Barcode scanners look up static cloud databases that fail on unlisted regional Indian products or recent reformulation updates. SachLabel reads the physical package directly using on-device computer vision.
- **"What prevents AI hallucination?"**
  → Architecture is deterministic first. `RuleEngine` verifies statutory criteria. An `EvidenceValidator` gatekeeper strictly requires that every cited evidence quote exists verbatim in the raw OCR text. If evidence cannot be verified, it outputs `NOT_ENOUGH_EVIDENCE`.
- **"Is an internet connection required?"**
  → No. The core pipeline (CameraX, ML Kit OCR, LayoutAnalyzer, LabelExtractor, ClaimMatcher, RuleEngine, EvidenceValidator, and Android TTS) executes 100% locally on-device.
- **"Is this medical advice?"**
  → Absolutely not. SachLabel audits promotional claims against package facts. Health context is strictly informational.
