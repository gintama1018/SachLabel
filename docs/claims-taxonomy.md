# Claim Taxonomy — v1 Rule Engine Spec

Each row is one detectable claim pattern. This table is the actual spec to build the rule engine's check functions from — one function per row. Keep this list closed for v1; adding a row requires legal/factual verification of the check logic, not just an engineering change.

| # | Claim (front, matched fuzzily) | Check logic (against back label) | Result if triggered | Cited fields |
|---|---|---|---|---|
| 1 | "No Added Sugar" | Nutrition table sugar value > 0g per serving above naturally-occurring baseline, OR ingredients contain added-sugar keywords (glucose syrup, high-fructose corn syrup, dextrose, maltose added separately from whole-food source) | Qualification | Front claim text + nutrition sugar line / ingredient keyword |
| 2 | "100% Natural" / "100% Pure" | Ingredients contain synthetic/artificial-flagged keyword (artificial flavor, artificial color, preservative INS code list) OR disclaimer text contains qualifying language ("quality mark," "does not guarantee," "as per company standard") | Contradiction | Front claim text + matched ingredient or disclaimer text |
| 3 | "Sugar-Free" | Nutrition table sugar value > regulatory threshold for "sugar-free" labeling (verify exact threshold per applicable food regulation before shipping this check) | Contradiction | Front claim text + nutrition sugar line |
| 4 | "No Preservatives" | Ingredients contain preservative-class keyword or INS code (INS 200–299 range, common preservative codes) | Contradiction | Front claim text + matched ingredient |
| 5 | "Organic" | No recognized organic certification mark/number detected in OCR text | Qualification (flag as unverified, not false) | Front claim text + absence note |
| 6 | "High Protein" | Nutrition table protein-per-serving below the claim's implied threshold (verify applicable regulatory threshold before shipping) | Qualification | Front claim text + nutrition protein line |
| 7 | "Zero Trans Fat" | Ingredients contain "partially hydrogenated oil" (a source of trans fat even when the nutrition table rounds down to 0g per serving under labeling rules) | Qualification | Front claim text + matched ingredient |
| 8 | Vague wellness claims ("Immunity Booster," "Detox," etc.) | No ingredient with a commonly recognized supporting property detected | Qualification (flag as unsubstantiated by visible ingredients, not false) | Front claim text + note on absence of supporting ingredient |

## Build notes
- **Fuzzy matching, not exact string match** — OCR output will have noise (misread characters, line breaks mid-phrase). Use a similarity threshold, not exact equality, when matching claim text.
- **Rows 3 and 6 need a verified regulatory threshold before shipping** — don't hardcode a guessed number; confirm the applicable food-labeling standard's actual threshold, or soften the check to a qualification-only flag if you can't verify it in time.
- **Every row must produce a cited-text output** — no row should be able to trigger a result without also returning the exact source text that caused it (this is a hard architectural requirement, not a nice-to-have — see design.md, evidence-first principle).
- **This list is user-visible.** Ship a simple in-app "What we check for" screen listing these 8 patterns in plain language — this is what keeps the product's own claims honest.
