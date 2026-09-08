# Pre-Demo QA Checklist

Run this the night before and the morning of. Don't discover a failure live.

## Product test set — build this list before the demo
Test against at least one real product per claim taxonomy row (see `claims-taxonomy.md`):

- [ ] "No Added Sugar" product with a sweetener in ingredients
- [ ] "100% Natural" product with a qualifying disclaimer or synthetic ingredient
- [ ] "Sugar-Free" product — verify against actual nutrition table
- [ ] "No Preservatives" product with a preservative-class ingredient
- [ ] "Organic" product with no visible certification mark
- [ ] "High Protein" product — verify against actual per-serving value
- [ ] "Zero Trans Fat" product with partially hydrogenated oil listed
- [ ] A vague wellness claim product ("immunity booster" type)
- [ ] **At least one clean product** — a claim that's actually accurate, to confirm the "no issue found" state works and doesn't false-positive

## Conditions to test under (not just good lighting)
- [ ] Normal indoor lighting (baseline)
- [ ] Slight glare on packaging (common with plastic wrap/foil)
- [ ] Curved surface (bottle, not flat box)
- [ ] Small/dense fine print at actual size, not zoomed
- [ ] At least one product with mixed English + regional-language text on pack

## App-level checks
- [ ] Retake flow works if a photo is rejected
- [ ] Processing screen shows determinate feedback, not an indefinite spinner
- [ ] TTS plays correctly in at least 2 of the supported languages
- [ ] "No claim detected" state triggers correctly on a product with no headline claim
- [ ] Opt-in health context always shows the fixed disclaimer sentence, every time, no exceptions
- [ ] Core flow (capture → result) completes without a network connection

## Team logistics (HackTracker-relevant)
- [ ] Confirm which build windows are "Red Light" (phone-only) vs. open, and plan work accordingly — this is 25% of score and is measured automatically
- [ ] Office Kit (screen mirror / clipboard / remote control) tested and working before the event, not during it

## Day-of
- [ ] Charge the demo phone fully; bring a backup charged phone with the app pre-installed
- [ ] Pre-select the exact product(s) you'll demo with — see demo-script.md — and have tested each one successfully 3+ times
- [ ] Have the static fallback screenshot ready in case live capture fails in the room's lighting
