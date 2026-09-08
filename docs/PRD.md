# PRD — Claim ↔ Label Contradiction Detector
**Working name:** LabelTruth (placeholder — rename freely)
**Status:** Hackathon MVP spec
**Owner:** Sonu Jangir (GINTAMA) / Silver Soul Studios

---

## 1. Problem Statement

Packaged products routinely carry prominent front-of-pack claims ("100% Natural," "No Added Sugar," "Sugar-Free") that are legally qualified, contradicted, or meaningfully narrowed by text elsewhere on the same package — ingredient lists, nutrition tables, or fine-print disclaimers. Most consumers:

- Don't read the fine print at all.
- Can't reliably parse dense ingredient/nutrition text even when they try.
- In many cases, struggle with reading the label language itself (literacy or language barrier — regional language readers, elderly users, low-literacy users).

No mainstream tool solves this specific problem. Ingredient-scoring apps (e.g. Yuka) work off barcode-to-database lookups and score products; they do not read the physical package text and therefore cannot catch a marketing claim contradicting its own fine print. This is an unaddressed gap, not an incremental improvement on an existing category.

## 2. Product Thesis

> Can AI reconstruct what a product is communicating to a consumer, and identify information that materially changes that interpretation?

The product is not a health scanner. It is a **claim-auditing tool**: it audits a product's own words against itself, and explains the result in plain language — including out loud, in the user's own language, for users who can't or don't want to read it.

## 3. Target Users

| Persona | Need |
|---|---|
| Low-literacy / low digital-literacy shopper | Can't parse fine print; needs a spoken, plain-language explanation |
| Regional-language-first shopper | Package is in English/mixed script; needs explanation in their own language |
| Health-conscious but time-pressed shopper | Wants a fast in-aisle sanity check, not a full nutrition audit |
| Shopper with a stated dietary concern (diabetic, allergy, etc.) | Wants ingredient flags relevant to their concern — informational, not diagnostic |

## 4. Goals (v1)

1. Detect a prominent front-of-pack claim from a product photo.
2. Cross-check that claim against back-of-pack ingredients / nutrition / disclaimer text.
3. Flag material contradictions, qualifications, or omissions — citing the exact package text that triggered the flag.
4. Explain the flag in plain language, in the user's selected regional language, with optional text-to-speech playback.
5. Offer an **opt-in**, clearly-labeled-as-informational health context layer, only after the core claim analysis is shown.

## 5. Non-Goals (v1 — explicitly out of scope)

- **Not** a general nutrition/health scoring app. No 0–100 "health score."
- **Not** a medical advice tool. No diagnosis, no "safe for you: yes/no" verdicts.
- **Not** covering personal care, supplements, or medicines/OTC in v1 (see roadmap, §9).
- **Not** attempting general claim understanding — v1 detects a **curated, hardcoded set** of known claim patterns (see §7.3), not arbitrary claims.
- **Not** claiming perfect accuracy — every flag ships with the source text visible so the user can judge for themselves.

## 6. Key Features

### 6.1 Core: Claim ↔ Label Contradiction Detection
- Two-photo capture: front of pack, then back of pack.
- OCR extracts text from both.
- System identifies the prominent claim (front) and matches it against a known claim-pattern rule.
- Rule/semantic engine checks the claim's conditions against ingredients/nutrition text.
- On match failure or qualification found → flag raised, with exact cited text from both sides.
- On no issue found → clean result state (not just silence — explicit "no contradiction found for this claim").

### 6.2 Plain-language explanation + regional language + TTS
- Explanation text generated in plain, hedged, non-accusatory language (see design.md for tone rules).
- User selects a language on first launch (Hindi + at least 2–3 major regional languages for MVP, English as default/fallback).
- Explanation is available as on-device text-to-speech playback in the selected language — this directly serves the low-literacy persona and is a first-class feature, not an accessibility afterthought.

### 6.3 Opt-in health context (secondary layer)
- Shown only after the claim analysis result, via a "Want to check what this means for you?" prompt.
- User may optionally add a dietary goal, allergy, or condition.
- Output is always framed as informational relevance, never a determination: *"This ingredient may be relevant to the concern you mentioned. This is informational, not a medical determination."*

### 6.4 Evidence-first UX
- Every flag shows the exact quoted source text (front claim + back text) that produced it. No claim without a citation the user can independently verify.

## 7. Functional Requirements

### 7.1 Input
- Camera capture, two shots (front, back), on-device only — no upload to a server required for v1.

### 7.2 Processing pipeline
See architecture.md for full technical detail. Summary: OCR → structured extraction → claim detection → claim/evidence matching → conflict detection → explanation generation → (optional) TTS.

### 7.3 Claim taxonomy (MVP hardcoded set)
v1 ships with a fixed, documented list of claim patterns rather than general claim understanding. Suggested starting set:
1. "No Added Sugar" vs. sugar content / sweetener ingredients
2. "100% Natural" / "100% Pure" vs. qualifying disclaimer text
3. "Sugar-Free" vs. nutrition table sugar value
4. "No Preservatives" vs. preservative-class ingredients (INS codes)
5. "Organic" vs. certification mark presence/absence
6. "High Protein" vs. actual protein-per-serving threshold
7. "Zero Trans Fat" vs. partially hydrogenated oil presence
8. "Immunity Booster" / vague wellness claims vs. absence of any supporting ingredient

This list is explicitly stated as the current scope in-product (e.g. a visible "what we check for" screen) — consistent with the product's own no-overclaiming principle.

### 7.4 Output states
- **Contradiction found** — flag, cited evidence, plain-language explanation.
- **Qualification found** — claim is true but narrower than it appears (e.g., "no added sugar" but naturally sugar-heavy).
- **No issue found** — explicit clean result, not just absence of a flag.
- **Could not analyze** — OCR/claim detection failure state (see design.md for handling).

## 8. Success Metrics (hackathon context)

- Demo reliability: correctly flags at least 3 out of 4 pre-tested real products with known contradictions.
- False-positive control: does not flag a clean product with a straightforwardly true claim.
- On-device: pipeline runs without a network call for the core flow.
- Rubric alignment: uses camera (CV/OCR) and voice (TTS) — directly closes the camera/voice gap noted in prior iQOO evaluation feedback (ThinkFirst, 76/100).

## 9. Roadmap Beyond v1

| Version | Scope |
|---|---|
| v1 | Food packaging claims (this doc) |
| v2 | Personal-care product claims |
| v3 | Supplements |
| v4 | Medicines/OTC label interpretation — **much stricter safety boundaries required; treat as a separate safety review, not an incremental extension** |

v4 is a roadmap slide only. It should not be implied as working or in-scope during a hackathon demo.

## 10. Risks

- **CV/layout risk:** distinguishing "the claim" from other bold text on varied real-world packaging (glare, curved surfaces, multilingual text). Mitigated in v1 by requiring two deliberate photos rather than one ambiguous shot.
- **Rule engine scope risk:** claim taxonomy must stay explicitly bounded; do not imply general claim comprehension.
- **Liability risk:** health-context layer must never issue a yes/no safety verdict. Enforce hedged language at the template level, not just in prompt instructions to an LLM.
- **OCR robustness risk:** must be tested against real, imperfect packaging photos, not just clean flat-lay demo shots.
