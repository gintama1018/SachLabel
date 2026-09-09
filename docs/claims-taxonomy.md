# Claim Taxonomy — v1 Canonical Specification

This document defines the closed set of **8 canonical v1 claim categories** recognized by SachLabel.
Single source of truth implemented in `com.sachlabel.app.data.model.CanonicalClaimCategory`.

---

## Canonical Categories Table

| # | Canonical Key (`id`) | User-Facing Display Name | Core Verification Target | Result Types | Source Fields Cited |
|---|---|---|---|---|---|
| **1** | `no_added_sugar` | **No Added Sugar** | Inspects ingredients for alternate-name sugar syrups (glucose syrup, maltodextrin, invert sugar, concentrates) and checks nutrition table sugar values. | `NEEDS_CONTEXT` / `CONSISTENT` | Ingredient name or raw sugar declaration line |
| **2** | `100_percent_natural` | **100% Natural / Pure** | Inspects ingredients for synthetic/artificial additives, colorings, flavorings, and fine print for qualifying trademark/disclaimer language. | `MISLEADING` / `NEEDS_CONTEXT` / `CONSISTENT` | Matched synthetic ingredient or disclaimer sentence |
| **3** | `sugar_free` | **Sugar-Free / Zero Sugar** | Checks total sugars per 100g against the 0.5g/100g regulatory threshold; checks for non-nutritive artificial sweeteners (sucralose, aspartame, polyols). | `MISLEADING` / `NEEDS_CONTEXT` / `CONSISTENT` | Raw sugar declaration line or matched sweetener |
| **4** | `no_preservatives` | **No Preservatives** | Checks ingredient declarations for Class II chemical preservatives (sodium benzoate, sorbates, sulfites, INS 200–299). | `MISLEADING` / `CONSISTENT` | Matched preservative ingredient |
| **5** | `organic` | **Organic / Certified Organic** | Inspects back text for recognized certification marks or license numbers (Jaivik Bharat, NPOP, USDA Organic). | `CONSISTENT` / `NEEDS_CONTEXT` | Certification mark quote or `Evidence.absent()` |
| **6** | `high_protein` | **High Protein** | Evaluates nutrition table protein declaration against statutory high-protein threshold (>= 12g/100g). | `CONSISTENT` / `NEEDS_CONTEXT` / `MISLEADING` | Raw protein declaration line |
| **7** | `zero_trans_fat` | **Zero Trans Fat** | Checks ingredients for partially hydrogenated vegetable oil / vanaspati and checks nutrition table trans fat value (<= 0.2g/100g). | `NEEDS_CONTEXT` / `MISLEADING` / `CONSISTENT` | Matched hydrogenated oil or trans fat line |
| **8** | `vague_wellness` | **Vague Wellness Claims** | Inspects fine print for legal disclaimers and checks ingredients for recognized active supporting nutrients (vitamin C, zinc, turmeric). | `NEEDS_CONTEXT` / `CONSISTENT` / `NOT_ENOUGH_EVIDENCE` | Disclaimer quote or supporting ingredient |

---

## Critical Taxonomy Rules

1. **"No Added Sugar" vs "Sugar-Free" Separation**:
   - `no_added_sugar` checks whether sugars or sugar syrups were added during manufacturing (naturally occurring sugars from milk or fruit may remain).
   - `sugar_free` checks total sugar content against the statutory 0.5g/100g threshold and flags replacement artificial sweeteners.
   - These are distinct regulatory definitions and are evaluated by separate rule functions.

2. **Strictly Closed Scope in v1**:
   - Prototype categories such as `gluten_free`, `low_fat`, `whole_wheat_atta`, `dermatologically_tested`, or `no_artificial_colors` are **retired from the v1 canonical registry**.
   - If a product carries a claim outside these 8 categories, SachLabel emits `Verdict.NO_CLAIM_DETECTED` with a clear explanation rather than making unsupported guesses.

3. **Zero Synthetic Quotations**:
   - Absence of evidence is flagged using `Evidence.absent()`. The system never invents fake quotations such as *"No preservatives detected."*
   - All citations must pass `EvidenceValidator` against the raw OCR text.
