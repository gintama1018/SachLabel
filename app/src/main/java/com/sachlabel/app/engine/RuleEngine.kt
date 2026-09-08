package com.sachlabel.app.engine

import com.sachlabel.app.data.model.*

/**
 * Deterministic rule engine implementing CLAIM_RULES.md.
 *
 * Each claim type has its own check function returning a [ClaimResult].
 * Rules run BEFORE any LLM call. If a rule fires, no LLM is needed.
 * If no rule fires, the result can optionally be escalated to an LLM (Phase 6).
 *
 * CLAIM PATTERN → CHECK AGAINST → TRIGGER → VERDICT → CITE
 */
object RuleEngine {

    /**
     * Run the rule engine against a structured label and detected claim.
     *
     * @param claim  the detected front-label claim
     * @param label  structured extraction from both photos
     * @param language  user's selected language (for explanation text)
     * @return [ClaimResult] — always. Never throws; returns NOT_ENOUGH_EVIDENCE if stuck.
     */
    fun check(
        claim: Claim,
        label: StructuredLabel,
        language: UserLanguage = UserLanguage.ENGLISH
    ): ClaimResult {
        val result = when (claim.patternKey) {
            "no_added_sugar" -> checkNoAddedSugar(claim, label)
            "100_percent_natural" -> check100PercentNatural(claim, label)
            "no_preservatives" -> checkNoPreservatives(claim, label)
            "no_artificial_colors" -> checkNoArtificialColors(claim, label)
            "organic" -> checkOrganic(claim, label)
            "high_protein" -> checkHighProtein(claim, label)
            "zero_trans_fat" -> checkZeroTransFat(claim, label)
            "immunity_booster" -> checkImmunityBooster(claim, label)
            "gluten_free" -> checkGlutenFree(claim, label)
            "low_fat" -> checkLowFat(claim, label)
            else -> notEnoughEvidence(claim, "No rule defined for pattern: ${claim.patternKey}")
        }
        // Apply localized explanation if language is not English
        return if (language.code == "en") result
        else result.copy(
            explanationLocalized = ExplanationTemplates.get(result, language)
        )
    }

    /**
     * Entry point when no claim was detected on the front label.
     */
    fun noClaimDetected(): ClaimResult = ClaimResult(
        claim = null,
        verdict = Verdict.NO_CLAIM_DETECTED,
        frontText = "",
        evidence = Evidence.absent(),
        explanationEn = "No prominent marketing claim was detected on the front of this product. " +
            "This product may not carry one of the claim types this version checks for.",
        ruleKey = "no_claim"
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Rule 1 — "No Added Sugar" / "Sugar Free" / "Zero Sugar"
    // CLAIM_RULES.md Rule 1
    // ─────────────────────────────────────────────────────────────────────────

    private fun checkNoAddedSugar(claim: Claim, label: StructuredLabel): ClaimResult {
        val ruleKey = "no_added_sugar"

        // Check 1: ingredients contain added-sugar keywords
        val sugarIngredientKeywords = listOf(
            "glucose syrup", "high-fructose corn syrup", "dextrose",
            "maltodextrin", "fructose", "honey", "molasses",
            "fruit juice concentrate", "corn syrup", "maltose",
            "invert sugar", "agave", "rice syrup"
        )
        val matchedIngredient = label.ingredients.firstOrNull { ingredient ->
            sugarIngredientKeywords.any { kw -> ingredient.lowercase().contains(kw) }
        }

        // Check 2: nutrition table sugars > 0.5g
        val sugarsG = label.nutritionTable[StructuredLabel.KEY_SUGARS_G]

        return when {
            matchedIngredient != null -> ClaimResult(
                claim = claim,
                verdict = Verdict.NEEDS_CONTEXT,
                frontText = claim.rawText,
                evidence = Evidence(
                    quote = matchedIngredient,
                    sourceField = Evidence.SourceField.INGREDIENTS,
                    key = "matched_ingredient"
                ),
                explanationEn = "The front says \"${claim.rawText}\", but the back label lists " +
                    "\"$matchedIngredient\" — a sugar-related ingredient. " +
                    "\"No added sugar\" can be technically true while the product still contains " +
                    "naturally-occurring or alternate-name sugars.",
                ruleKey = ruleKey
            )

            sugarsG != null && sugarsG > 0.5 -> ClaimResult(
                claim = claim,
                verdict = Verdict.NEEDS_CONTEXT,
                frontText = claim.rawText,
                evidence = Evidence(
                    quote = "Sugars: ${sugarsG}g per 100g",
                    sourceField = Evidence.SourceField.NUTRITION_TABLE,
                    key = StructuredLabel.KEY_SUGARS_G,
                    value = sugarsG
                ),
                explanationEn = "The front says \"${claim.rawText}\", but the nutrition table " +
                    "shows ${sugarsG}g of sugars per 100g. These may be naturally occurring, " +
                    "but worth noting.",
                ruleKey = ruleKey
            )

            label.ingredients.isEmpty() && sugarsG == null ->
                notEnoughEvidence(claim, ruleKey)

            else -> consistent(claim, ruleKey,
                "No added-sugar ingredients or elevated sugar values were found on the back label.")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rule 2 — "100% Natural" / "100% Pure" / "100% Organic"
    // CLAIM_RULES.md Rule 2
    // ─────────────────────────────────────────────────────────────────────────

    private fun check100PercentNatural(claim: Claim, label: StructuredLabel): ClaimResult {
        val ruleKey = "100_percent_natural"

        // Check A: fine print contains qualifying language
        val qualifierKeywords = listOf(
            "quality mark", "does not guarantee", "does not imply",
            "refers only to", "not a claim as to composition",
            "as per company standard", "has not been evaluated",
            "does not diagnose", "not been evaluated by"
        )
        val qualifyingFinePrint = label.finePrint.firstOrNull { line ->
            qualifierKeywords.any { kw -> line.lowercase().contains(kw) }
        }

        // Check B: ingredients contain clearly synthetic/artificial keywords
        val syntheticKeywords = listOf(
            "artificial flavour", "artificial flavor",
            "artificial colour", "artificial color",
            "artificial sweetener", "sodium benzoate",
            "potassium sorbate", "tartrazine", "sunset yellow",
            "brilliant blue", "erythrosine", "monosodium glutamate"
        )
        val syntheticIngredient = label.ingredients.firstOrNull { ingredient ->
            syntheticKeywords.any { kw -> ingredient.lowercase().contains(kw) }
        }

        // Also check fine print for synthetic ingredients
        val syntheticInFinePrint = if (syntheticIngredient == null) {
            label.finePrint.firstOrNull { line ->
                syntheticKeywords.any { kw -> line.lowercase().contains(kw) }
            }
        } else null

        return when {
            syntheticIngredient != null -> ClaimResult(
                claim = claim,
                verdict = Verdict.MISLEADING,
                frontText = claim.rawText,
                evidence = Evidence(
                    quote = syntheticIngredient,
                    sourceField = Evidence.SourceField.INGREDIENTS,
                    key = "synthetic_ingredient"
                ),
                explanationEn = "The front says \"${claim.rawText}\", but the back label lists " +
                    "\"$syntheticIngredient\" — an ingredient that is not typically considered natural.",
                ruleKey = ruleKey
            )

            qualifyingFinePrint != null -> ClaimResult(
                claim = claim,
                verdict = Verdict.NEEDS_CONTEXT,
                frontText = claim.rawText,
                evidence = Evidence(
                    quote = qualifyingFinePrint,
                    sourceField = Evidence.SourceField.FINE_PRINT,
                    key = "qualifying_disclaimer"
                ),
                explanationEn = "The front says \"${claim.rawText}\", but the package's own fine print " +
                    "qualifies this: \"$qualifyingFinePrint\"",
                ruleKey = ruleKey
            )

            syntheticInFinePrint != null -> ClaimResult(
                claim = claim,
                verdict = Verdict.NEEDS_CONTEXT,
                frontText = claim.rawText,
                evidence = Evidence(
                    quote = syntheticInFinePrint,
                    sourceField = Evidence.SourceField.FINE_PRINT,
                    key = "synthetic_in_fine_print"
                ),
                explanationEn = "The front says \"${claim.rawText}\", but the fine print mentions " +
                    "\"$syntheticInFinePrint\".",
                ruleKey = ruleKey
            )

            label.ingredients.isEmpty() && label.finePrint.isEmpty() ->
                notEnoughEvidence(claim, ruleKey)

            else -> consistent(claim, ruleKey,
                "No qualifying disclaimers or synthetic ingredients were found on the back label.")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rule 3 — "No Preservatives" / "Preservative Free"
    // CLAIM_RULES.md Rule 7 (also covers no_artificial_colors)
    // ─────────────────────────────────────────────────────────────────────────

    private fun checkNoPreservatives(claim: Claim, label: StructuredLabel): ClaimResult {
        val ruleKey = "no_preservatives"

        val preservativeKeywords = listOf(
            "sodium benzoate", "potassium sorbate", "sodium nitrate",
            "sodium nitrite", "sulfur dioxide", "sulphur dioxide",
            "calcium propionate", "sodium propionate", "sorbic acid",
            "benzoic acid", "bha", "bht", "tbhq",
            "ins 211", "ins 202", "ins 220", "ins 221", "ins 282",
            "ins 320", "ins 321", "ins 319"
        )

        // Also check INS code range 200–299 (preservatives)
        val insPreservativePattern = Regex("ins\\s*2[0-9]{2}", RegexOption.IGNORE_CASE)

        val matchedIngredient = label.ingredients.firstOrNull { ingredient ->
            preservativeKeywords.any { kw -> ingredient.lowercase().contains(kw) } ||
                insPreservativePattern.containsMatchIn(ingredient)
        }

        return if (matchedIngredient != null) {
            ClaimResult(
                claim = claim,
                verdict = Verdict.MISLEADING,
                frontText = claim.rawText,
                evidence = Evidence(
                    quote = matchedIngredient,
                    sourceField = Evidence.SourceField.INGREDIENTS,
                    key = "preservative_ingredient"
                ),
                explanationEn = "The front says \"${claim.rawText}\", but the back label lists " +
                    "\"$matchedIngredient\" — a preservative.",
                ruleKey = ruleKey
            )
        } else if (label.ingredients.isEmpty()) {
            notEnoughEvidence(claim, ruleKey)
        } else {
            consistent(claim, ruleKey,
                "No preservative-class ingredients were detected on the back label.")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rule 4 — "No Artificial Colors"
    // ─────────────────────────────────────────────────────────────────────────

    private fun checkNoArtificialColors(claim: Claim, label: StructuredLabel): ClaimResult {
        val ruleKey = "no_artificial_colors"

        val colorKeywords = listOf(
            "tartrazine", "sunset yellow", "brilliant blue", "erythrosine",
            "allura red", "ponceau", "quinoline yellow", "indigo carmine",
            "artificial colour", "artificial color",
            "ins 102", "ins 110", "ins 122", "ins 123", "ins 124",
            "ins 127", "ins 129", "ins 133"
        )
        val insColorPattern = Regex("ins\\s*1[0-9]{2}", RegexOption.IGNORE_CASE)

        val matchedIngredient = label.ingredients.firstOrNull { ingredient ->
            colorKeywords.any { kw -> ingredient.lowercase().contains(kw) } ||
                insColorPattern.containsMatchIn(ingredient)
        }

        return if (matchedIngredient != null) {
            ClaimResult(
                claim = claim,
                verdict = Verdict.MISLEADING,
                frontText = claim.rawText,
                evidence = Evidence(
                    quote = matchedIngredient,
                    sourceField = Evidence.SourceField.INGREDIENTS,
                    key = "artificial_color_ingredient"
                ),
                explanationEn = "The front says \"${claim.rawText}\", but the back label lists " +
                    "\"$matchedIngredient\" — an artificial coloring agent.",
                ruleKey = ruleKey
            )
        } else if (label.ingredients.isEmpty()) {
            notEnoughEvidence(claim, ruleKey)
        } else {
            consistent(claim, ruleKey,
                "No artificial coloring agents were detected on the back label.")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rule 5 — "Organic"
    // ─────────────────────────────────────────────────────────────────────────

    private fun checkOrganic(claim: Claim, label: StructuredLabel): ClaimResult {
        val ruleKey = "organic"

        // Look for organic certification indicators
        val certKeywords = listOf(
            "usda organic", "certified organic", "npop certified",
            "india organic", "eu organic", "jas organic",
            "organic certification", "reg.", "certificate no",
            "certification number"
        )
        val allText = (label.ingredients + label.finePrint + label.frontClaimsRaw)
            .joinToString(" ").lowercase()

        val hasCertification = certKeywords.any { kw -> allText.contains(kw) }

        return if (!hasCertification && label.finePrint.isNotEmpty()) {
            ClaimResult(
                claim = claim,
                verdict = Verdict.NEEDS_CONTEXT,
                frontText = claim.rawText,
                evidence = Evidence(
                    quote = "No organic certification mark or number detected on the label.",
                    sourceField = Evidence.SourceField.FINE_PRINT,
                    key = "certification_absent"
                ),
                explanationEn = "The front says \"${claim.rawText}\", but no recognized organic " +
                    "certification mark or certificate number was detected on the back label. " +
                    "Organic claims should be backed by a verifiable certification.",
                ruleKey = ruleKey
            )
        } else if (label.finePrint.isEmpty() && label.ingredients.isEmpty()) {
            notEnoughEvidence(claim, ruleKey)
        } else {
            consistent(claim, ruleKey,
                "An organic certification indicator was detected on the label.")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rule 6 — "High Protein"
    // Threshold: ≥ 20g protein per 100g is generally considered "high protein"
    // (verify against FSSAI/applicable standard before production)
    // ─────────────────────────────────────────────────────────────────────────

    private fun checkHighProtein(claim: Claim, label: StructuredLabel): ClaimResult {
        val ruleKey = "high_protein"
        val HIGH_PROTEIN_THRESHOLD_G = 20.0  // per 100g

        val proteinG = label.nutritionTable[StructuredLabel.KEY_PROTEIN_G]

        return when {
            proteinG == null -> {
                // No nutrition table data — check if ingredient list suggests protein source
                val proteinIngredients = listOf(
                    "whey protein", "casein", "soy protein", "pea protein",
                    "egg white", "chicken", "fish", "lentils", "chickpea"
                )
                val hasProteinIngredient = label.ingredients.any { ingredient ->
                    proteinIngredients.any { kw -> ingredient.lowercase().contains(kw) }
                }
                if (hasProteinIngredient) {
                    notEnoughEvidence(claim, ruleKey,
                        "A protein-source ingredient is listed, but the nutrition table isn't readable. " +
                        "Cannot verify the protein quantity.")
                } else {
                    notEnoughEvidence(claim, ruleKey)
                }
            }
            proteinG >= HIGH_PROTEIN_THRESHOLD_G -> consistent(
                claim, ruleKey,
                "The nutrition table shows ${proteinG}g of protein per 100g, " +
                    "consistent with a high-protein claim."
            ).copy(
                evidence = Evidence(
                    quote = "Protein: ${proteinG}g per 100g",
                    sourceField = Evidence.SourceField.NUTRITION_TABLE,
                    key = StructuredLabel.KEY_PROTEIN_G,
                    value = proteinG
                )
            )
            else -> ClaimResult(
                claim = claim,
                verdict = Verdict.NEEDS_CONTEXT,
                frontText = claim.rawText,
                evidence = Evidence(
                    quote = "Protein: ${proteinG}g per 100g",
                    sourceField = Evidence.SourceField.NUTRITION_TABLE,
                    key = StructuredLabel.KEY_PROTEIN_G,
                    value = proteinG
                ),
                explanationEn = "The front says \"${claim.rawText}\", but the nutrition table " +
                    "shows only ${proteinG}g of protein per 100g — below the typical threshold " +
                    "for a high-protein claim.",
                ruleKey = ruleKey
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rule 7 — "Zero Trans Fat"
    // ─────────────────────────────────────────────────────────────────────────

    private fun checkZeroTransFat(claim: Claim, label: StructuredLabel): ClaimResult {
        val ruleKey = "zero_trans_fat"

        // "Partially hydrogenated oil" is a source of trans fat even when nutrition table
        // rounds to 0g (under labeling rules, <0.5g per serving may be labeled 0g)
        val phOilKeywords = listOf(
            "partially hydrogenated", "hydrogenated vegetable oil",
            "partially hydrogenated vegetable oil", "partially hydrogenated palm oil",
            "hydrogenated fat"
        )

        val matchedIngredient = label.ingredients.firstOrNull { ingredient ->
            phOilKeywords.any { kw -> ingredient.lowercase().contains(kw) }
        }

        return if (matchedIngredient != null) {
            ClaimResult(
                claim = claim,
                verdict = Verdict.NEEDS_CONTEXT,
                frontText = claim.rawText,
                evidence = Evidence(
                    quote = matchedIngredient,
                    sourceField = Evidence.SourceField.INGREDIENTS,
                    key = "partially_hydrogenated_oil"
                ),
                explanationEn = "The front says \"${claim.rawText}\", but the back label lists " +
                    "\"$matchedIngredient\". Partially hydrogenated oils are a source of trans fat. " +
                    "Labels can show 0g trans fat per serving even when this ingredient is present, " +
                    "if the per-serving amount falls below the labeling threshold.",
                ruleKey = ruleKey
            )
        } else if (label.ingredients.isEmpty()) {
            notEnoughEvidence(claim, ruleKey)
        } else {
            consistent(claim, ruleKey,
                "No partially hydrogenated oil was detected on the back label.")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rule 8 — "Immunity Booster" / vague wellness claims
    // ─────────────────────────────────────────────────────────────────────────

    private fun checkImmunityBooster(claim: Claim, label: StructuredLabel): ClaimResult {
        val ruleKey = "immunity_booster"

        // Check fine print for regulatory disclaimer
        val disclaimerKeywords = listOf(
            "has not been evaluated", "not evaluated by",
            "does not diagnose", "does not treat", "does not cure",
            "not intended to diagnose", "results may vary",
            "individual results"
        )
        val disclaimer = label.finePrint.firstOrNull { line ->
            disclaimerKeywords.any { kw -> line.lowercase().contains(kw) }
        }

        // Check for supporting active ingredients
        val supportingIngredients = listOf(
            "vitamin c", "zinc", "vitamin d", "elderberry",
            "echinacea", "turmeric", "ginger", "probiotics",
            "vitamin e", "selenium", "beta glucan"
        )
        val hasSupportingIngredient = label.ingredients.any { ingredient ->
            supportingIngredients.any { kw -> ingredient.lowercase().contains(kw) }
        }

        return when {
            disclaimer != null -> ClaimResult(
                claim = claim,
                verdict = Verdict.NEEDS_CONTEXT,
                frontText = claim.rawText,
                evidence = Evidence(
                    quote = disclaimer,
                    sourceField = Evidence.SourceField.FINE_PRINT,
                    key = "regulatory_disclaimer"
                ),
                explanationEn = "The front says \"${claim.rawText}\", but the package's own fine print " +
                    "states: \"$disclaimer\"",
                ruleKey = ruleKey
            )

            !hasSupportingIngredient && label.ingredients.isNotEmpty() -> ClaimResult(
                claim = claim,
                verdict = Verdict.NEEDS_CONTEXT,
                frontText = claim.rawText,
                evidence = Evidence(
                    quote = "No ingredient commonly associated with immune support (e.g. Vitamin C, Zinc) detected.",
                    sourceField = Evidence.SourceField.INGREDIENTS,
                    key = "supporting_ingredient_absent"
                ),
                explanationEn = "The front says \"${claim.rawText}\", but the back label does not list " +
                    "any ingredient commonly associated with immune support, such as Vitamin C or Zinc.",
                ruleKey = ruleKey
            )

            label.ingredients.isEmpty() -> notEnoughEvidence(claim, ruleKey)

            else -> consistent(claim, ruleKey,
                "Ingredients associated with immune support were detected on the back label.")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rule 9 — "Gluten Free"
    // ─────────────────────────────────────────────────────────────────────────

    private fun checkGlutenFree(claim: Claim, label: StructuredLabel): ClaimResult {
        val ruleKey = "gluten_free"

        val glutenIngredients = listOf(
            "wheat", "barley", "rye", "malt", "spelt", "kamut",
            "triticale", "wheat flour", "wheat starch", "semolina"
        )
        val crossContaminationKeywords = listOf(
            "may contain traces of gluten", "may contain wheat",
            "processed in a facility that also processes wheat",
            "produced in a facility that handles gluten"
        )

        val directGlutenIngredient = label.ingredients.firstOrNull { ingredient ->
            glutenIngredients.any { kw -> ingredient.lowercase().contains(kw) }
        }

        val crossContaminationWarning = label.finePrint.firstOrNull { line ->
            crossContaminationKeywords.any { kw -> line.lowercase().contains(kw) }
        }

        return when {
            directGlutenIngredient != null -> ClaimResult(
                claim = claim,
                verdict = Verdict.MISLEADING,
                frontText = claim.rawText,
                evidence = Evidence(
                    quote = directGlutenIngredient,
                    sourceField = Evidence.SourceField.INGREDIENTS,
                    key = "gluten_ingredient"
                ),
                explanationEn = "The front says \"${claim.rawText}\", but the back label lists " +
                    "\"$directGlutenIngredient\" — a gluten-containing ingredient.",
                ruleKey = ruleKey
            )

            crossContaminationWarning != null -> ClaimResult(
                claim = claim,
                verdict = Verdict.NEEDS_CONTEXT,
                frontText = claim.rawText,
                evidence = Evidence(
                    quote = crossContaminationWarning,
                    sourceField = Evidence.SourceField.FINE_PRINT,
                    key = "cross_contamination"
                ),
                explanationEn = "The front says \"${claim.rawText}\", but the fine print warns: " +
                    "\"$crossContaminationWarning\" — relevant for those with celiac disease.",
                ruleKey = ruleKey
            )

            label.ingredients.isEmpty() -> notEnoughEvidence(claim, ruleKey)

            else -> consistent(claim, ruleKey,
                "No gluten-containing ingredients or cross-contamination warnings were detected.")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rule 10 — "Low Fat" / "Fat Free"
    // ─────────────────────────────────────────────────────────────────────────

    private fun checkLowFat(claim: Claim, label: StructuredLabel): ClaimResult {
        val ruleKey = "low_fat"

        val fatG = label.nutritionTable[StructuredLabel.KEY_TOTAL_FAT_G]
        val sugarsG = label.nutritionTable[StructuredLabel.KEY_SUGARS_G]
        val sodiumMg = label.nutritionTable[StructuredLabel.KEY_SODIUM_MG]

        val LOW_FAT_THRESHOLD = 3.0  // per 100g — standard threshold
        val HIGH_SUGAR_THRESHOLD = 15.0
        val HIGH_SODIUM_THRESHOLD = 600.0

        return when {
            fatG == null -> notEnoughEvidence(claim, ruleKey)

            fatG <= LOW_FAT_THRESHOLD && (sugarsG ?: 0.0) > HIGH_SUGAR_THRESHOLD -> ClaimResult(
                claim = claim,
                verdict = Verdict.NEEDS_CONTEXT,
                frontText = claim.rawText,
                evidence = Evidence(
                    quote = "Fat: ${fatG}g per 100g | Sugars: ${sugarsG}g per 100g",
                    sourceField = Evidence.SourceField.NUTRITION_TABLE,
                    key = "low_fat_high_sugar"
                ),
                explanationEn = "The fat claim appears accurate (${fatG}g per 100g), but the nutrition " +
                    "table also shows ${sugarsG}g of sugars per 100g — a common trade-off in " +
                    "low-fat products worth noting.",
                ruleKey = ruleKey
            )

            fatG <= LOW_FAT_THRESHOLD && (sodiumMg ?: 0.0) > HIGH_SODIUM_THRESHOLD -> ClaimResult(
                claim = claim,
                verdict = Verdict.NEEDS_CONTEXT,
                frontText = claim.rawText,
                evidence = Evidence(
                    quote = "Fat: ${fatG}g per 100g | Sodium: ${sodiumMg}mg per 100g",
                    sourceField = Evidence.SourceField.NUTRITION_TABLE,
                    key = "low_fat_high_sodium"
                ),
                explanationEn = "The fat claim appears accurate (${fatG}g per 100g), but the nutrition " +
                    "table shows ${sodiumMg}mg of sodium per 100g — worth noting.",
                ruleKey = ruleKey
            )

            fatG <= LOW_FAT_THRESHOLD ->
                consistent(claim, ruleKey, "Fat: ${fatG}g per 100g — consistent with the claim.")
                    .copy(
                        evidence = Evidence(
                            quote = "Fat: ${fatG}g per 100g",
                            sourceField = Evidence.SourceField.NUTRITION_TABLE,
                            key = StructuredLabel.KEY_TOTAL_FAT_G,
                            value = fatG
                        )
                    )

            else -> ClaimResult(
                claim = claim,
                verdict = Verdict.MISLEADING,
                frontText = claim.rawText,
                evidence = Evidence(
                    quote = "Total Fat: ${fatG}g per 100g",
                    sourceField = Evidence.SourceField.NUTRITION_TABLE,
                    key = StructuredLabel.KEY_TOTAL_FAT_G,
                    value = fatG
                ),
                explanationEn = "The front says \"${claim.rawText}\", but the nutrition table shows " +
                    "${fatG}g of fat per 100g — above the typical threshold for this claim.",
                ruleKey = ruleKey
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun consistent(claim: Claim, ruleKey: String, note: String): ClaimResult =
        ClaimResult(
            claim = claim,
            verdict = Verdict.CONSISTENT,
            frontText = claim.rawText,
            evidence = Evidence(
                quote = note,
                sourceField = Evidence.SourceField.INGREDIENTS,
                key = "consistent_check"
            ),
            explanationEn = "The front claim \"${claim.rawText}\" appears to be supported by the back label. $note",
            ruleKey = ruleKey
        )

    private fun notEnoughEvidence(
        claim: Claim,
        ruleKey: String,
        note: String = "The back label did not contain enough readable information to check this claim."
    ): ClaimResult =
        ClaimResult(
            claim = claim,
            verdict = Verdict.NOT_ENOUGH_EVIDENCE,
            frontText = claim.rawText,
            evidence = Evidence.absent(),
            explanationEn = note,
            ruleKey = ruleKey
        )
}
