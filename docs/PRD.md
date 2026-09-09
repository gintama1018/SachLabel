# PRD — SachLabel: Packaging Claim ↔ Label Contradiction Detector
**Product:** SachLabel
**Status:** Canonical Implementation Reference & Product Specification
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

SachLabel is an **AI-assisted packaging intelligence app that audits a product's own words against itself**.

The product is **NOT**:
- a health score or 0–100 rating
- a barcode-first product database
- a medical diagnostic tool
- a nutrition recommendation engine
- a legal/certification authority
- a binary "safe/unsafe" classifier

Core flow:
`REAL PACKAGE → FRONT/BACK PHOTOS → OCR → LABEL STRUCTURE / EXTRACTION → CLAIM MATCHING → DETERMINISTIC EVIDENCE CHECK → EVIDENCE VALIDATION → OPTIONAL LOCAL GEMMA REASONING / EXPLANATION → REGIONAL LANGUAGE → TTS`

## 3. Target Users

| Persona | Need |
|---|---|
| Low-literacy / low digital-literacy shopper | Can't parse fine print; needs a spoken, plain-language explanation |
| Regional-language-first shopper | Package is in English/mixed script; needs explanation in their own language |
| Health-conscious but time-pressed shopper | Wants a fast in-aisle sanity check, not a full nutrition audit |
| Shopper with a stated dietary concern (diabetic, allergy, etc.) | Wants ingredient flags relevant to their concern — informational, not diagnostic |

## 4. Goals (v1)

1. Detect a prominent front-of-pack claim from a product photo.
2. Cross-check that claim against back-of-pack ingredients / nutrition / disclaimer text using deterministic rules.
3. Flag material contradictions, qualifications, or omissions — citing the exact package text that triggered the flag.
4. Enforce strict evidence verification via `EvidenceValidator` (zero synthetic quotes).
5. Explain the flag in plain language, in the user's selected regional language, with native on-device text-to-speech playback.
6. Support optional local Gemma model reasoning/explanation when available, falling back safely to deterministic explanations.
7. Offer an **opt-in**, clearly-labeled-as-informational health context layer, only after the core claim analysis is shown.

## 5. Non-Goals (v1 — explicitly out of scope)

- **Not** a general nutrition/health scoring app. No 0–100 "health score."
- **Not** a medical advice tool. No diagnosis, no "safe for you: yes/no" verdicts.
- **Not** covering personal care, supplements, or medicines/OTC in v1 (see roadmap, §9).
- **Not** attempting open-ended general claim comprehension — v1 detects a **curated, canonical set of 8 claim categories** (see §7.3).
- **Not** claiming perfect accuracy — every flag ships with verbatim source text so the user can independently verify.

## 6. Key Features

### 6.1 Core: Claim ↔ Label Contradiction Detection
- Two-photo capture: front of pack, then back of pack.
- On-device ML Kit OCR extracts text from both with bounding-box geometry.
- System extracts structured label (ingredients, nutrition lines).
- `ClaimMatcher` identifies the prominent claim (front) against canonical patterns.
- `RuleEngine` deterministically evaluates the claim against ingredients/nutrition facts.
- `EvidenceValidator` gatekeeper verifies all evidence quotes exist verbatim in OCR text.
- Optional `LocalAiEngine` provides constrained, grounded explanation via on-device Gemma.

### 6.2 Plain-language explanation + regional language + TTS
- Explanation text generated in plain, hedged, non-accusatory language (see design.md for tone rules).
- Regional language selection supporting 8 Indian languages: English (`en`), Hindi (`hi`), Marathi (`mr`), Tamil (`ta`), Telugu (`te`), Kannada (`kn`), Bengali (`bn`), Gujarati (`gu`).
- Explanation is available as native on-device text-to-speech (`android.speech.tts.TextToSpeech`) in the selected language.

### 6.3 Opt-in health context (secondary layer)
- Shown only after the claim analysis result, via a "Want to check what this means for you?" prompt.
- User may optionally add a dietary goal, allergy, or condition.
- Output is always framed as informational relevance, never a determination: *"This ingredient may be relevant to the concern you mentioned. This is informational, not a medical determination."*

### 6.4 Evidence-first Guarantee
- Every verdict must be grounded in OCR evidence from the physical package.
- The system must never create synthetic quotation evidence.
- If evidence cannot be validated, the system returns `NOT_ENOUGH_EVIDENCE`.
- Clear separation: WHAT THE PACKAGE SAYS vs. SACHLABEL'S EXPLANATION.

## 7. Functional Requirements

### 7.1 Input
- Camera capture, two shots (front, back), on-device CameraX — no cloud upload required.

### 7.2 Processing pipeline
See architecture.md for full technical detail. Summary:
`CameraX → ImagePreprocessor → ML Kit OCR → LayoutAnalyzer → LabelExtractor → ClaimMatcher → Deterministic RuleEngine → EvidenceValidator → Optional LocalAiEngine / Gemma → Plain Language Explanation → Regional Language → Android TTS`

### 7.3 Canonical Claim Taxonomy (The Frozen 8 Categories)
v1 ships with exactly 8 canonical, documented claim categories. Arbitrary claims outside this taxonomy produce `NO_CLAIM_DETECTED`.

| # | Canonical ID | Canonical Category | Front Claim Checked | Back Label Evidence Checked |
|---|---|---|---|---|
| 1 | `no_added_sugar` | No Added Sugar | "No Added Sugar", "0% Added Sugar" | Ingredient list scanned for sucrose, liquid glucose, invert syrup, maltodextrin, high-fructose corn syrup, honey, fruit juice concentrates |
| 2 | `100_percent_natural` | 100% Natural / Pure | "100% Natural", "All Natural", "Pure" | Ingredient list scanned for artificial flavors, synthetic preservatives, nature-identical flavorings, and qualifying disclaimers |
| 3 | `sugar_free` | Sugar-Free / Zero Sugar | "Sugar Free", "Zero Sugar", "0 Sugar" | Nutrition declaration total sugars threshold (> 0.5g/100g or 100ml) |
| 4 | `no_preservatives` | No Preservatives | "No Preservatives", "Zero Preservatives" | Ingredient list scanned for INS 200–299 preservatives (benzoates, sorbates, sulfites, nitrites, propionates) |
| 5 | `organic` | Organic | "Organic", "Certified Organic", "Jaivik" | Package scanned for organic certification mentions (NPOP, Jaivik Bharat, USDA Organic) or synthetic additives |
| 6 | `high_protein` | High Protein | "High Protein", "Protein Rich" | Nutrition table protein per 100g compared against statutory threshold (> 10g/100g or 20% energy) |
| 7 | `zero_trans_fat` | Zero Trans Fat | "Zero Trans Fat", "0g Trans Fat" | Nutrition table trans fat (> 0.2g/100g) and ingredient presence of partially hydrogenated vegetable oils |
| 8 | `vague_wellness` | Vague Wellness / Immunity Booster | "Immunity Booster", "Detox", "Wellness" | Scanned for absence of substantiated functional ingredients or disclaimer qualifications |

> **Important Taxonomy Rule:** "No Added Sugar" (`no_added_sugar`) and "Sugar-Free / Zero Sugar" (`sugar_free`) are **DIFFERENT claim types**. "No Added Sugar" evaluates ingredient composition for added sweeteners; "Sugar-Free" evaluates the absolute sugar quantity in the nutrition table against statutory limits.

### 7.4 Output states
- **MISMATCH** — Contradiction found, cited evidence, plain-language explanation.
- **QUALIFIED** — Qualification found: claim is technically true but narrower than implied (e.g., naturally sugar-heavy).
- **VERIFIED** — No contradiction found for this claim; package evidence corroborates the claim.
- **NOT_ENOUGH_EVIDENCE** — Package text unreadable, incomplete, or absent; evidence cannot be validated.

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
