# Claim Rules — Deterministic Pattern Library

Check these BEFORE calling an LLM (see system_design.txt §2.7 and
PROMPT_TEMPLATES.md stage 4/5 note). Cheaper, faster, more trustworthy for
a demo. Each rule: trigger condition -> verdict -> what to cite.

Format per rule:
```
CLAIM PATTERN   : phrases that match this rule
CHECK AGAINST   : which structured field(s) to inspect
TRIGGER         : condition that fires the rule
VERDICT         : resulting badge
CITE            : what to quote back to the user
```

---

### Rule 1 — "No Added Sugar" / "Sugar Free" / "Zero Sugar"
```
CLAIM PATTERN   : "no added sugar", "sugar free", "zero sugar", "0% sugar"
CHECK AGAINST   : nutrition_table.sugars_g, ingredients[]
TRIGGER         : sugars_g > 0.5  OR  ingredients contains any of
                  [glucose syrup, fructose, maltodextrin, honey, molasses,
                   fruit juice concentrate, corn syrup, dextrose]
VERDICT         : NEEDS_CONTEXT
CITE            : the matched ingredient term + the sugars_g figure
NOTE            : "no added sugar" can be literally true (no sugar added
                  during processing) while the product still contains
                  naturally-occurring or alternate-name sugars — explain
                  this distinction in plain language, don't call it an
                  outright lie.
```

### Rule 2 — "100% <Adjective>" (Pure / Natural / Organic)
```
CLAIM PATTERN   : "100% pure", "100% natural", "100% organic"
CHECK AGAINST   : fine_print[]
TRIGGER         : fine_print contains qualifier language, e.g.
                  "quality mark", "not a claim as to composition",
                  "does not imply", "refers only to"
VERDICT         : NEEDS_CONTEXT (or MISLEADING if ingredients[] directly
                  contradicts, e.g. "100% natural" + a synthetic
                  preservative/color listed by E-number)
CITE            : the exact fine-print qualifier sentence
```

### Rule 3 — "Immunity Booster" / "Boosts Immunity"
```
CLAIM PATTERN   : "immunity booster", "boosts immunity", "strengthens
                  immunity"
CHECK AGAINST   : fine_print[], ingredients[]
TRIGGER         : fine_print contains a disclaimer, e.g. "this statement
                  has not been evaluated", "does not diagnose, treat,
                  cure" -- OR no supporting active ingredient (e.g.
                  vitamin C, zinc) is listed at all
VERDICT         : NEEDS_CONTEXT
CITE            : disclaimer sentence if present; otherwise note the
                  absence of a specific supporting ingredient
```

### Rule 4 — "Dermatologically Tested" / "Clinically Proven"
```
CLAIM PATTERN   : "dermatologically tested", "clinically proven",
                  "clinically tested"
CHECK AGAINST   : fine_print[]
TRIGGER         : fine_print does not specify what was tested, sample
                  size, or references a specific study -- OR explicitly
                  states "results may vary" / "individual results"
VERDICT         : NEEDS_CONTEXT
CITE            : whatever fine print exists about the testing claim; if
                  none exists at all, cite the absence explicitly and use
                  NOT_ENOUGH_EVIDENCE instead
```

### Rule 5 — "Gluten Free"
```
CLAIM PATTERN   : "gluten free"
CHECK AGAINST   : ingredients[], fine_print[]
TRIGGER         : ingredients contains wheat/barley/rye/malt -- OR
                  fine_print contains "may contain traces of gluten" /
                  "produced in a facility that also processes wheat"
VERDICT         : MISLEADING if a gluten-containing ingredient is
                  directly listed; NEEDS_CONTEXT if only a trace/
                  cross-contamination disclaimer is present
CITE            : the specific ingredient or disclaimer sentence
```

### Rule 6 — "Low Fat" / "Fat Free"
```
CLAIM PATTERN   : "low fat", "fat free", "0% fat"
CHECK AGAINST   : nutrition_table.total_fat_g, nutrition_table.sugars_g
TRIGGER         : total_fat_g is genuinely low/zero (claim likely
                  CONSISTENT) BUT sugars_g or sodium_mg is unusually high
VERDICT         : NEEDS_CONTEXT — not because the fat claim is false, but
                  to surface the common "low fat, high sugar" trade-off
CITE            : the fat figure (supports the claim) AND the sugar/sodium
                  figure (the added context)
```

### Rule 7 — "No Preservatives" / "No Artificial Colors"
```
CLAIM PATTERN   : "no preservatives", "no artificial colors/colours",
                  "no artificial flavors/flavours"
CHECK AGAINST   : ingredients[]
TRIGGER         : ingredients contains an E-number or named preservative/
                  synthetic color/flavor compound (e.g. sodium benzoate,
                  potassium sorbate, tartrazine, sunset yellow)
VERDICT         : MISLEADING
CITE            : the specific listed ingredient that contradicts the
                  claim
```

### Default Rule — No Pattern Match
```
TRIGGER         : claim doesn't match any rule above
ACTION          : route to LLM reasoning (PROMPT_TEMPLATES.md stage 4/5)
                  with all available evidence fields
VERDICT         : whatever the LLM returns, subject to the backend
                  evidence-quote validation guardrail
```

### Default Rule — No Evidence Found At All
```
TRIGGER         : claim detected, but no ingredient/nutrition/fine-print
                  field has any plausibly related content (checked by
                  keyword/semantic search returning zero candidates)
VERDICT         : NOT_ENOUGH_EVIDENCE
ACTION          : do NOT call the LLM for a verdict in this case — there
                  is nothing to reason over; show the "limited check"
                  state directly (see plan.txt Flow 6b/6d)
```

---

## Adding new rules later (v2/v3/v4 expansion)

Each new product category (personal-care, supplements, OTC/medicine) will
need its own small rule set following this same template:
`CLAIM PATTERN -> CHECK AGAINST -> TRIGGER -> VERDICT -> CITE`.
Keep rules additive in a config/data file rather than branching logic, so
the rule engine stays a flat, auditable list — this also makes it easy to
show judges "here's literally every check we run" if asked how the system
avoids just being an opaque LLM guess.
