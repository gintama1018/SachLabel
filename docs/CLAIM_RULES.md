# Claim Rules — Deterministic Pattern Library (v1 Canonical)

This document defines the deterministic rule engine checks executed in `com.sachlabel.app.engine.RuleEngine`.
These deterministic checks are evaluated **BEFORE** any optional local AI call (architecture §3.4 / §3.5).

### Canonical Rule Structure
Every rule defines:
- **CLAIM PATTERN**: Front-of-pack phrases matched by `com.sachlabel.app.engine.ClaimMatcher`
- **CHECK AGAINST**: Structured label fields inspected (`ingredients`, `nutritionTable`, `finePrint`)
- **TRIGGER**: Deterministic condition evaluated
- **VERDICT**: `CONSISTENT`, `NEEDS_CONTEXT`, `MISLEADING`, or `NOT_ENOUGH_EVIDENCE`
- **CITE**: Verbatim quote from raw OCR text, or `Evidence.absent()` if missing

---

### Rule 1 — "No Added Sugar" (`no_added_sugar`)
```
CLAIM PATTERN   : "no added sugar", "without added sugar", "zero added sugar", "बिना चीनी"
CHECK AGAINST   : ingredients[], nutritionTable["sugars_g"], finePrint[]
TRIGGER         : 
  - CONTEXT: Ingredients contain added sugar syrups / alternate-name sugars
    [glucose syrup, high-fructose corn syrup, dextrose, maltodextrin, fructose,
     honey, molasses, fruit juice concentrate, corn syrup, maltose, invert sugar,
     invert syrup, agave, rice syrup]
  - CONTEXT: nutritionTable["sugars_g"] > 0.5g with elevated sugars line
  - NOT ENOUGH EVIDENCE: Ingredients and nutrition table both unreadable
  - CONSISTENT: No added sugar ingredients and sugar values clean
VERDICT         : NEEDS_CONTEXT / CONSISTENT / NOT_ENOUGH_EVIDENCE
CITE            : Matched ingredient quote OR exact nutrition raw sugar line
NOTE            : Distinct from "Sugar-Free" (Rule 3). "No added sugar" allows
                  naturally occurring sugars from milk/fruit, but warrants context
                  when alternate-name sugar syrups or concentrates are present.
```

---

### Rule 2 — "100% Natural" / "100% Pure" (`100_percent_natural`)
```
CLAIM PATTERN   : "100% natural", "100% pure", "all natural", "100% प्राकृतिक", "शुद्ध"
CHECK AGAINST   : ingredients[], finePrint[]
TRIGGER         :
  - MISLEADING: Ingredients contain synthetic / chemical additives:
    [artificial flavor, artificial flavour, artificial color, artificial colour,
     synthetic, nature identical, INS 200–299 preservatives]
  - NEEDS_CONTEXT: finePrint contains qualifier language:
    ["quality mark", "trademark only", "does not imply", "refers only to"]
  - CONSISTENT: No synthetic additives and no qualifying disclaimer found
VERDICT         : MISLEADING / NEEDS_CONTEXT / CONSISTENT
CITE            : Exact synthetic ingredient OR exact fine-print disclaimer sentence
```

---

### Rule 3 — "Sugar-Free" / "Zero Sugar" (`sugar_free`)
```
CLAIM PATTERN   : "sugar free", "sugar-free", "zero sugar", "0% sugar", "शुगर फ्री", "चीनी रहित"
CHECK AGAINST   : nutritionTable["sugars_g"], ingredients[]
TRIGGER         :
  - MISLEADING: nutritionTable total sugars > 0.5g per 100g/serving
  - NEEDS_CONTEXT: Sugars <= 0.5g, but non-nutritive / artificial sweeteners present
    [sucralose, aspartame, acesulfame potassium, acesulfame k, stevia, steviol,
     maltitol, sorbitol, erythritol, xylitol, ins 950, ins 951, ins 955, ins 960]
  - CONSISTENT: Total sugars <= 0.5g/100g with no disclaimer flags
VERDICT         : MISLEADING / NEEDS_CONTEXT / CONSISTENT
CITE            : Exact nutrition raw sugar line OR matched sweetener ingredient
NOTE            : Distinct from "No Added Sugar" (Rule 1). Sugar-free evaluates
                  total sugar concentration against the statutory 0.5g/100g threshold.
```

---

### Rule 4 — "No Preservatives" (`no_preservatives`)
```
CLAIM PATTERN   : "no preservatives", "preservative free", "zero preservatives", "बिना प्रिजर्वेटिव"
CHECK AGAINST   : ingredients[]
TRIGGER         :
  - MISLEADING: Ingredients contain recognized preservative keywords or INS codes:
    [sodium benzoate, potassium sorbate, sodium metabisulphite, potassium metabisulphite,
     sorbic acid, benzoic acid, sulphur dioxide, calcium propionate, INS 211, INS 202,
     INS 224, INS 200, INS 220, INS 282, class ii preservative, preservative]
  - CONSISTENT: No preservative keywords or INS 200-series codes detected
VERDICT         : MISLEADING / CONSISTENT
CITE            : Exact matched preservative ingredient text
```

---

### Rule 5 — "Organic" / "Certified Organic" (`organic`)
```
CLAIM PATTERN   : "organic", "certified organic", "100% organic", "jaivik bharat", "जैविक"
CHECK AGAINST   : rawBackText, finePrint[]
TRIGGER         :
  - CONSISTENT: OCR text confirms recognized certification keyword or mark:
    ["jaivik bharat", "npop", "pgs-india", "usda organic", "certified organic by",
     "np-op", "organic certification", "certification no", "lic no"]
  - NEEDS_CONTEXT: Front claims organic but no recognized certification mark
    or license number appears in back text
VERDICT         : CONSISTENT / NEEDS_CONTEXT
CITE            : Certification text snippet when present; Evidence.absent() when unverified
```

---

### Rule 6 — "High Protein" (`high_protein`)
```
CLAIM PATTERN   : "high protein", "protein rich", "rich in protein", "हाई प्रोटीन"
CHECK AGAINST   : nutritionTable["protein_g"], ingredients[]
TRIGGER         :
  - CONSISTENT: nutritionTable protein >= 12.0g per 100g (statutory high-protein threshold)
  - NEEDS_CONTEXT: Protein is between 6.0g and 11.9g per 100g ("source of protein", but
    not strictly "high protein")
  - MISLEADING: Protein is below 6.0g per 100g
VERDICT         : CONSISTENT / NEEDS_CONTEXT / MISLEADING
CITE            : Exact nutrition raw protein line
```

---

### Rule 7 — "Zero Trans Fat" (`zero_trans_fat`)
```
CLAIM PATTERN   : "zero trans fat", "0g trans fat", "trans fat free", "शून्य ट्रांस फैट"
CHECK AGAINST   : ingredients[], nutritionTable["trans_fat_g"]
TRIGGER         :
  - NEEDS_CONTEXT: Ingredients contain partially hydrogenated vegetable oil / vanaspati
    (disclosing source of industrial trans fatty acids even if rounded to 0g on table)
  - MISLEADING: nutritionTable discloses trans_fat_g > 0.2g per 100g
  - CONSISTENT: Trans fat <= 0.2g/100g and no hydrogenated oils in ingredients
VERDICT         : NEEDS_CONTEXT / MISLEADING / CONSISTENT
CITE            : Matched partially hydrogenated oil ingredient OR raw trans fat nutrition line
```

---

### Rule 8 — Vague Wellness Claims (`vague_wellness`)
```
CLAIM PATTERN   : "immunity booster", "boosts immunity", "detox", "energy booster", "रोग प्रतिरोधक"
CHECK AGAINST   : finePrint[], ingredients[]
TRIGGER         :
  - NEEDS_CONTEXT (Disclaimer): finePrint notes qualifying legal disclaimer:
    ["not been evaluated", "does not diagnose", "not intended to treat",
     "results may vary", "individual results", "quality mark"]
  - CONSISTENT: Ingredients disclose recognized active supporting nutrient:
    [vitamin c, zinc, amla, tulsi, ashwagandha, curcumin, turmeric, echinacea, vitamin d]
  - NEEDS_CONTEXT (Omission): No supporting active ingredient identifiable
VERDICT         : NEEDS_CONTEXT / CONSISTENT / NOT_ENOUGH_EVIDENCE
CITE            : Exact disclaimer quote OR supporting ingredient quote
```

---

## Evidence-First Policy

1. **Verbatim Quotation Requirement**: Every evidence quote MUST be an exact character-for-character substring of the scanned OCR text.
2. **Zero Synthetic Quotations**: The engine will never invent a quote. When a statutory disclosure or certification is absent, `Evidence.absent()` is used.
3. **Gatekeeper Validation**: All verdicts emitted by `RuleEngine` pass through `EvidenceValidator.validate(result, rawBackText)`. If an evidence quote cannot be matched to the raw OCR text, the quote is rejected and the verdict is downgraded to `NOT_ENOUGH_EVIDENCE`.
