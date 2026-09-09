# SachLabel — Documentation Index

SachLabel is an AI-assisted packaging intelligence app that audits a food product's own words against itself.

---

## Authoritative Documentation Guide

1. **[architecture.md](architecture.md)** — Core multimodal pipeline, component architecture, data flow, tech stack, and safety constraints.
2. **[claims-taxonomy.md](claims-taxonomy.md)** — The closed 8 canonical v1 claim categories, trigger definitions, and verification targets.
3. **[CLAIM_RULES.md](CLAIM_RULES.md)** — Deterministic pattern library and statutory FSSAI verification check functions.
4. **[PRD.md](PRD.md)** — Product requirements, problem statement, user personas, and explicit non-goals.
5. **[PROMPT_TEMPLATES.md](PROMPT_TEMPLATES.md)** — On-device prompt construction for `LocalAiEngine` and targeted evidence filtering.
6. **[design.md](design.md)** — Evidence-first visual language, Stitch design tokens, and UI layout specifications.
7. **[DEMO_SCRIPT.md](DEMO_SCRIPT.md)** — Judge-facing pitch, live packaging demonstration flow, and QA objection handling.
8. **[qa-checklist.md](qa-checklist.md)** — Physical-package testing checklist, edge case handling, and device validation steps.

---

## Core Product North Star

> SachLabel does **not** score how healthy a product is. We check whether what's printed on the front of the pack actually matches what's printed on the back — and show you the exact words that prove it, in your own language.

---

## Non-Negotiable Engineering Principles

1. **Evidence-First Guarantee**: Every verdict must cite an exact verbatim quotation from the raw OCR text or declare `Evidence.absent()`. The system never invents synthetic quotes.
2. **Deterministic Supremacy**: `RuleEngine` is the primary source of truth. Local AI (`LocalAiEngine`) is optional, constrained, and cannot override deterministic verdicts.
3. **Closed Canonical Taxonomy**: v1 supports exactly the 8 canonical categories defined in `CanonicalClaimCategory.kt`.
4. **Offline-First Execution**: The core pipeline runs completely on-device without network calls.
