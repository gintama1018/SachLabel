# Pitch Outline — Slide by Slide

Content only — build the actual deck in whatever tool you're using once this is locked. Keep each slide to one idea.

---

**Slide 1 — Title**
SachLabel: "AI that audits a product's own words against itself."

**Slide 2 — The problem**
A real product photo (front + back) with a claim and a contradicting fine-print line, side by side. Let the judges read it themselves for 3 seconds before you say anything.

**Slide 3 — Why existing tools miss this**
One line: barcode-lookup health scanners (Yuka etc.) score ingredients from a database — they never read the physical package, so they structurally can't catch this. Not a knock on them — a different problem.

**Slide 4 — How it works (the on-device pipeline)**
CameraX capture → ML Kit OCR → Structured Extraction → Claim Matcher → Deterministic Rule Engine → EvidenceValidator → Optional Local Gemma explanation → Plain-language explanation + TTS. Use icons, not paragraphs.

**Slide 5 — Live demo**
(No content on this slide — this is the cue to switch to the live app. See demo-script.md.)

**Slide 6 — The accessibility angle**
Regional language (8 Indian languages) + native Android TTS voice output. State plainly: this isn't an accessibility bolt-on, it's core to who this is for — people who can't or won't read dense fine print in a second language.

**Slide 7 — What we deliberately didn't build**
No health score. No medical verdict. Bounded canonical 8-claim taxonomy, zero synthetic quotes, and full offline-first deterministic core. This slide matters — it shows judges you understand your own scope, which is a stronger signal than pretending the system does more than it does.

**Slide 8 — Roadmap**
v1 food → v2 personal care → v3 supplements → v4 medicines (flagged as a separate safety review, not a casual extension).

**Slide 9 — Close**
Restate the thesis in one sentence + the one thing you want the judges to remember: "We're not judging products. We're making the product's own label legible."

---

## Presentation notes
- Don't read slide text aloud verbatim — the judges can read. Talk to what's *not* on the slide.
- Slide 4 and slide 7 are your technical-credibility slides — spend real prep time on these two specifically, they're what separates "nice idea" from "they've actually thought about this."
