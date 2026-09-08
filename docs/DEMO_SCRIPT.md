# Demo Script — SachLabel (for judges)

Target: ~60-90 seconds core demo + 30-60 seconds framing/close. Rehearse
with the exact physical products you'll bring — don't improvise product
choice live.

---

## 0. Pre-demo checklist (do this before you're called up)

- [ ] 3-5 real products pre-tested end-to-end, at least 1 with a strong,
      visually obvious "gotcha" (front claim vs. fine print).
- [ ] Phone/device charged, app open to Home screen already (skip
      onboarding live).
- [ ] Lighting at the demo table checked — glare on packaging kills OCR.
- [ ] Backup: screen recording of a successful run, in case live demo
      fails (network, lighting, etc.) — never let a live-demo failure be
      the last thing judges see.

## 1. Opening line (5-10 seconds)

> "Every packaged product makes a promise on the front. We built something
> that checks that promise against what the product's own back label
> actually says — and shows you the exact words that prove it."

Do NOT open with "we built a health app" — that undersells the
differentiation and invites the "another health app" reaction.

## 2. The core demo (60-90 seconds)

1. Hold up the pre-tested product. Point at the front claim out loud:
   *"This says '100% Pure.'"*
2. Scan front -> scan back (or gallery-picker fallback if lighting is
   unpredictable).
3. While processing, narrate once, briefly: *"It's reading the front
   claim, then the ingredients and fine print on the back."*
4. Result appears -> point at the verdict badge first (NEEDS_CONTEXT /
   MISLEADING), then tap "Show evidence."
5. Read the quoted fine print out loud, then physically point to that
   exact sentence on the real package in your hand.
   *"That's not us guessing — that's their own fine print."*
6. Tap the speaker icon -> play the regional-language audio for a few
   seconds. *"And for someone who doesn't read English fine print
   comfortably, they get this spoken back to them in their language."*

## 3. One-line personalization beat (10-15 seconds, only if time allows)

> "If someone tells us they're managing diabetes, we add one extra note —
> clearly separate from the core check, never a medical directive — just
> 'this may be relevant to you.'"

Show the "For you" card briefly. Do not spend more than this on
personalization — it's the secondary feature, not the headline.

## 4. The close (15-20 seconds)

> "The point isn't 'is this product healthy.' The point is: can a
> consumer trust what's printed on the front, based on what the
> manufacturer already printed on the back? Every flag we show is a
> direct quote from their own package — nothing here is our opinion."

Optional, if there's time and the judges seem technical:

> "Underneath, it's a rule engine first — deterministic checks for common
> patterns like sugar claims and purity disclaimers — and we only call an
> AI model for the genuinely ambiguous cases, and even then we verify its
> quote actually exists in the label text before showing it. So it's not
> a black box guessing at health scores."

## 5. Anticipated judge questions (prepare short answers)

- **"How is this different from just reading the label yourself?"**
  -> Most people don't read fine print at all; this surfaces the specific
  contradiction in seconds and reads it aloud, which matters a lot for
  low-literacy/regional-language users.

- **"What if the AI gets it wrong?"**
  -> Every verdict must cite an exact quote that we verify exists in the
  scanned text server-side; if it can't find solid evidence, it says
  "Not enough evidence" instead of guessing — walk through CLAIM_RULES.md
  rule engine briefly if asked for depth.

- **"Is this medical advice?"**
  -> No — explicitly not. The core product is a claim-vs-label fact
  check; personalization is a clearly separate, hedged, opt-in layer that
  never gives a directive.

- **"What's next after the hackathon?"**
  -> Personal-care claims, then supplements, then (with much stricter
  review) OTC/medicine labels — see PRD.md §10 for the phased roadmap.

- **"Why regional language/TTS specifically?"**
  -> The people most likely to miss a misleading fine-print claim are
  often the same people least likely to comfortably read dense English
  fine print — this isn't an add-on, it's core to who actually needs this
  tool.

## 6. Things to avoid saying

- Don't say "health score" or "we rate products" — undermines the core
  thesis.
- Don't say "AI decides if it's good for you" — reframe as "AI checks
  the package against itself."
- Don't over-promise medical usefulness of the personalization layer.
