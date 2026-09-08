# Design Doc
## Claim ↔ Label Contradiction Detector

## 1. Design Principles

1. **Evidence over verdict.** Never show a conclusion without the exact source text next to it. The user should be able to independently agree or disagree with every flag.
2. **Non-alarmist tone.** This is an audit, not an accusation. "Claim needs context" beats "This brand is lying to you." Overly aggressive framing invites both user distrust and legal risk.
3. **Reading is optional, not required.** Every core result must be consumable by voice. This is a first-class requirement, not a fallback — it's the reason the product exists for a meaningful share of its target users.
4. **Say what you don't know.** The product explicitly shows its own scope limits ("What we check for" list) rather than implying it understands every possible claim.

## 2. Visual Language

### 2.1 Result state colors
Avoid a traffic-light health-score palette (that's Yuka's visual language, and also implies a health judgment this product explicitly isn't making). Suggested palette:

| State | Color | Icon | Rationale |
|---|---|---|---|
| Contradiction found | Amber/warm orange | ⚠️ | "Pay attention," not "danger" |
| Qualification found | Muted amber | ℹ️ | Softer than contradiction |
| No issue found | Neutral green-gray | ✓ | Calm confirmation, not celebratory |
| No claim detected | Neutral gray | — | Informational, not a result |

Avoid red/green binary — it reads as a health verdict, which this product is not making.

### 2.2 Typography
- Large, high-contrast text by default (serves low-literacy and older users without requiring a separate "accessibility mode").
- Avoid dense paragraphs in results — short sentences, one idea per line.

### 2.3 Evidence display
- Quoted package text shown in a distinct visual treatment (e.g., monospace or quote-block styling) so it's clearly "what the package says" vs. "what we're telling you."

## 3. Screen-by-Screen Notes

**Home / Scan screen**
- One dominant action: "Scan a product." Nothing else competes for attention.
- Language indicator visible and tappable (not buried in a settings menu).

**Capture screens**
- Guide frame overlay to encourage a full, flat, well-lit shot — reduces OCR failure without adding backend complexity.
- Retake option always visible before committing a photo.

**Processing screen**
- Determinate feedback if possible (e.g., "Reading label…" → "Checking claim…") rather than an unlabeled spinner — builds trust that something specific is happening, especially important for users unfamiliar with this kind of tool.

**Result screen**
- Structure, top to bottom: state header → front claim (quoted) → back evidence (quoted) → plain-language explanation → 🔊 listen button → opt-in health CTA → scan-another action.
- This order is deliberate: evidence before interpretation, always.

**Opt-in health context screen**
- Preset chips (diabetic, high BP, gluten-free, allergy types) alongside free text — reduces typing for the exact users least likely to want to type.
- Disclaimer text is not dismissible/collapsible — it renders every time, same weight as the result itself.

## 4. Tone of Voice — Explanation Copy Rules

1. Always attribute to the package, not to Claude/the app's own judgment: *"the package's own fine print"* not *"we think this is misleading."*
2. Hedge appropriately: *"may create a broader impression than..."* not *"this is false."*
3. Never use the words "lie," "scam," "fraud," or similar — legally and tonally, this product audits information gaps, not intent.
4. Health-context responses always end with the fixed disclaimer sentence — do not vary or soften it away over iterations.

## 5. Accessibility & Regional Language

- Language selection is a first-run, top-level decision — not buried in settings.
- TTS is available on every result screen, not just as a global accessibility toggle — treat it as core UX for the target persona described in the PRD, not a bolt-on.
- Preset options (in Flow 6) reduce reliance on typed free text in any language.
- Icon + color + text redundancy on every result state (never convey a result through color alone).

## 6. What This Product Deliberately Does Not Look Like

- Not a 0–100 health score gauge (Yuka's visual signature) — avoids both direct visual comparison and the implied precision that comes with a single number.
- Not a red/green pass-fail badge — avoids implying certainty the underlying rule engine doesn't have.
- Not text-dense — the target user is explicitly someone who does not want to read a paragraph of ingredient analysis.
