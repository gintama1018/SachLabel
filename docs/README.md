# SachLabel — Doc Index (IQL Hackathon)

Read in this order:

1. **PRD.md** — what we're building and why, MVP scope, non-goals, risks.
2. **plan.txt** — every user flow, screen by screen, including edge cases.
3. **design.md** — visual language, screen specs, accessibility checklist.
4. **system_design.txt** — the AI pipeline, components, sequence flow, NFRs.
5. **architecture.md** — actual tech stack, API contracts, data models,
   build-order recommendation.
6. **PROMPT_TEMPLATES.md** — ready-to-use Claude prompts for each pipeline
   stage (vision extraction, claim detection, conflict reasoning,
   personalization note).
7. **CLAIM_RULES.md** — the deterministic rule-engine pattern library
   (check this before reaching for an LLM call).
8. **DEMO_SCRIPT.md** — the judge-facing pitch/demo walkthrough.

## One-line pitch

> We don't score how healthy a product is. We check whether what's printed
> on the front of the pack actually matches what's printed on the back —
> and show you the exact words that prove it, in your own language.

## Fastest path to a working demo (see architecture.md §6 for detail)

1. Hardcode/mock OCR output for 3-5 real, pre-tested products.
2. Build the rule engine (CLAIM_RULES.md) against that mock data.
3. Build the Result screen UI (design.md §3.5) against the rule engine's
   output — this is what judges look at longest.
4. Swap mock OCR for real vision/OCR calls (PROMPT_TEMPLATES.md).
5. Add translation + TTS for 1-2 languages.
6. Add personalization layer last, only if time remains.

## Non-negotiable guardrail (repeated across every doc, on purpose)

Every verdict shown to a user must cite an exact quoted snippet that
actually exists in the OCR text. Never show a verdict the system can't
point to evidence for — show "Not enough evidence" instead. This is the
product's entire credibility.
