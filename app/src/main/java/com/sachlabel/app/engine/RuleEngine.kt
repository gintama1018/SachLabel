package com.sachlabel.app.engine

import com.sachlabel.app.data.model.*

/**
 * Deterministic rule engine implementing the 8 canonical v1 claims from CLAIM_RULES.md.
 *
 * Each canonical claim has its own check function returning a [ClaimResult].
 *
 * Zero Synthetic Evidence Rule:
 * - When evidence is cited, it MUST be an exact verbatim substring from the label.
 * - Nutrition claims cite the verbatim raw OCR line stored in [StructuredLabel.nutritionRawLines].
 * - When no contradictory evidence is found or certification is absent, [Evidence.absent()]
 *   is used with NO fabricated quote string.
 */
object RuleEngine {

    /**
     * Run the rule engine against a structured label and detected claim.
     */
    fun check(
        claim: Claim,
        label: StructuredLabel,
        language: UserLanguage = UserLanguage.ENGLISH
    ): ClaimResult {
        val result = when (claim.patternKey) {
            "no_added_sugar" -> checkNoAddedSugar(claim, label)
            "100_percent_natural", "natural_or_pure" -> check100PercentNatural(claim, label)
            "sugar_free" -> checkSugarFree(claim, label)
            "no_preservatives" -> checkNoPreservatives(claim, label)
            "organic" -> checkOrganic(claim, label)
            "high_protein" -> checkHighProtein(claim, label)
            "zero_trans_fat" -> checkZeroTransFat(claim, label)
            "vague_wellness", "immunity_booster" -> checkVagueWellness(claim, label)
            else -> notEnoughEvidence(claim, claim.patternKey)
        }

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
            "This product may not carry one of the 8 claim categories checked in this version.",
        ruleKey = "no_claim"
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Rule 1 — "No Added Sugar"
    // ─────────────────────────────────────────────────────────────────────────

    private fun checkNoAddedSugar(claim: Claim, label: StructuredLabel): ClaimResult {
        val ruleKey = "no_added_sugar"

        // Check 1: ingredients contain added-sugar keywords
        val sugarIngredientKeywords = listOf(
            "glucose syrup", "high-fructose corn syrup", "dextrose",
            "maltodextrin", "fructose", "honey", "molasses",
            "fruit juice concentrate", "corn syrup", "maltose",
            "invert sugar", "invert syrup", "agave", "rice syrup"
        )
        val matchedIngredient = label.ingredients.firstOrNull { ingredient ->
            sugarIngredientKeywords.any { kw -> ingredient.lowercase().contains(kw) }
        }

        // Check 2: nutrition table sugars
        val sugarsG = label.nutritionTable[StructuredLabel.KEY_SUGARS_G]
        val rawSugarLine = label.nutritionRawLines[StructuredLabel.KEY_SUGARS_G]
            ?: findMatchingLine(label.rawBackText, "sugar")

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
                explanationEn = "The front claims \"${claim.rawText}\", but the ingredient list includes " +
                    "\"$matchedIngredient\". \"No added sugar\" can be technically true if sucrose wasn't added " +
                    "directly, while alternate-name sugar syrups are present.",
                ruleKey = ruleKey
            )

            sugarsG != null && sugarsG > 0.5 && rawSugarLine != null -> ClaimResult(
                claim = claim,
                verdict = Verdict.NEEDS_CONTEXT,
                frontText = claim.rawText,
                evidence = Evidence(
                    quote = rawSugarLine,
                    sourceField = Evidence.SourceField.NUTRITION_TABLE,
                    key = StructuredLabel.KEY_SUGARS_G,
                    value = sugarsG
                ),
                explanationEn = "The front claims \"${claim.rawText}\", but the nutrition table discloses " +
                    "${sugarsG}g of sugars per 100g. These may be naturally occurring, but provide context.",
                ruleKey = ruleKey
            )

            label.ingredients.isEmpty() && sugarsG == null ->
                notEnoughEvidence(claim, ruleKey)

            else -> consistent(
                claim = claim,
                ruleKey = ruleKey,
                explanation = "No added-sugar ingredients or elevated sugar values were detected on the back label."
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rule 2 — "100% Natural" / "100% Pure"
    // ─────────────────────────────────────────────────────────────────────────

    private fun check100PercentNatural(claim: Claim, label: StructuredLabel): ClaimResult {
        val ruleKey = "100_percent_natural"

        // Check A: fine print contains qualifying language
        val qualifierKeywords = listOf(
            "quality mark", "does not guarantee", "does not imply",
            "refers only to", "not a claim as to composition",
            "as per company standard", "has not been evaluated",
            "does not diagnose"
        )
        val qualifyingFinePrint = label.finePrint.firstOrNull { line ->
            qualifierKeywords.any { kw -> line.lowercase().contains(kw) }
        }

        // Check B: ingredients contain synthetic additives or chemical preservatives
        val syntheticKeywords = listOf(
            "artificial flavour", "artificial flavor", "artificial colour",
            "artificial color", "synthetic", "acidity regulator",
            "preservative", "stabilizer", "emulsifier",
            "ins 211", "ins 202", "ins 224", "ins 150", "ins 102", "ins 110"
        )
        val syntheticIngredient = label.ingredients.firstOrNull { ingredient ->
            syntheticKeywords.any { kw -> ingredient.lowercase().contains(kw) }
        }

        return when {
            syntheticIngredient != null -> ClaimResult(
                claim = claim,
                verdict = Verdict.MISLEADING,
                frontText = claim.rawText,
                evidence = Evidence(
                    quote = syntheticIngredient,
                    sourceField = Evidence.SourceField.INGREDIENTS,
                    key = "synthetic_additive"
                ),
                explanationEn = "The front claims \"${claim.rawText}\", but the ingredients list " +
                    "\"$syntheticIngredient\" — a synthetic additive or preservative.",
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
                explanationEn = "The front claims \"${claim.rawText}\", but the back-of-pack fine print " +
                    "contains qualifying language: \"$qualifyingFinePrint\".",
                ruleKey = ruleKey
            )

            label.ingredients.isEmpty() && label.finePrint.isEmpty() ->
                notEnoughEvidence(claim, ruleKey)

            else -> consistent(
                claim = claim,
                ruleKey = ruleKey,
                explanation = "No synthetic additives or qualifying fine-print disclaimers were found in the label text."
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rule 3 — "Sugar-Free" / "Zero Sugar" (Canonical Category 3)
    // ─────────────────────────────────────────────────────────────────────────

    private fun checkSugarFree(claim: Claim, label: StructuredLabel): ClaimResult {
        val ruleKey = "sugar_free"
        val sugarsG = label.nutritionTable[StructuredLabel.KEY_SUGARS_G]
        val rawSugarLine = label.nutritionRawLines[StructuredLabel.KEY_SUGARS_G]
            ?: findMatchingLine(label.rawBackText, "sugar")

        // Direct sugar ingredients that contradict a "Sugar-Free" claim
        val sugarIngredientKeywords = listOf(
            "sugar", "sucrose", "glucose", "dextrose", "fructose",
            "maltodextrin", "corn syrup", "honey", "invert syrup"
        )
        val matchedSugarIngredient = label.ingredients.firstOrNull { ingredient ->
            sugarIngredientKeywords.any { kw -> ingredient.lowercase().contains(kw) }
        }

        // Artificial/non-nutritive sweeteners that provide context for sugar-free claims
        val artificialSweetenerKeywords = listOf(
            "sucralose", "aspartame", "acesulfame", "saccharin", "stevia", "erythritol", "xylitol", "sorbitol", "ins 950", "ins 951", "ins 955", "ins 960"
        )
        val matchedSweetener = label.ingredients.firstOrNull { ingredient ->
            artificialSweetenerKeywords.any { kw -> ingredient.lowercase().contains(kw) }
        }

        return when {
            // Contradiction 1: Nutrition table discloses sugar > 0.5g per 100g
            sugarsG != null && sugarsG > 0.5 && rawSugarLine != null -> ClaimResult(
                claim = claim,
                verdict = Verdict.MISLEADING,
                frontText = claim.rawText,
                evidence = Evidence(
                    quote = rawSugarLine,
                    sourceField = Evidence.SourceField.NUTRITION_TABLE,
                    key = StructuredLabel.KEY_SUGARS_G,
                    value = sugarsG
                ),
                explanationEn = "The front claims \"${claim.rawText}\", but the nutrition table shows " +
                    "${sugarsG}g of sugars per 100g, exceeding the 0.5g per 100g threshold for sugar-free claims.",
                ruleKey = ruleKey
            )

            // Contradiction 2: Listed ingredients contain explicit sugar
            matchedSugarIngredient != null -> ClaimResult(
                claim = claim,
                verdict = Verdict.MISLEADING,
                frontText = claim.rawText,
                evidence = Evidence(
                    quote = matchedSugarIngredient,
                    sourceField = Evidence.SourceField.INGREDIENTS,
                    key = "sugar_ingredient"
                ),
                explanationEn = "The front claims \"${claim.rawText}\", but the ingredient list explicitly " +
                    "lists \"$matchedSugarIngredient\".",
                ruleKey = ruleKey
            )

            // Context: Uses artificial/non-nutritive sweeteners
            matchedSweetener != null -> ClaimResult(
                claim = claim,
                verdict = Verdict.NEEDS_CONTEXT,
                frontText = claim.rawText,
                evidence = Evidence(
                    quote = matchedSweetener,
                    sourceField = Evidence.SourceField.INGREDIENTS,
                    key = "artificial_sweetener"
                ),
                explanationEn = "The product is sugar-free (<0.5g sugars), but uses non-nutritive sweetener \"$matchedSweetener\".",
                ruleKey = ruleKey
            )

            label.ingredients.isEmpty() && sugarsG == null ->
                notEnoughEvidence(claim, ruleKey)

            else -> consistent(
                claim = claim,
                ruleKey = ruleKey,
                explanation = "The nutrition table confirms total sugars are within the 0.5g per 100g threshold for sugar-free claims."
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rule 4 — "No Preservatives"
    // ─────────────────────────────────────────────────────────────────────────

    private fun checkNoPreservatives(claim: Claim, label: StructuredLabel): ClaimResult {
        val ruleKey = "no_preservatives"

        val preservativeKeywords = listOf(
            "sodium benzoate", "potassium sorbate", "sodium metabisulphite",
            "potassium metabisulphite", "sorbic acid", "benzoic acid",
            "sulphur dioxide", "sulfur dioxide", "calcium propionate",
            "ins 211", "ins 202", "ins 224", "ins 200", "ins 220", "ins 282",
            "e211", "e202", "e224", "e200", "e220", "e282",
            "class ii preservative", "preservative"
        )
        val matchedPreservative = label.ingredients.firstOrNull { ingredient ->
            preservativeKeywords.any { kw -> ingredient.lowercase().contains(kw) }
        }

        return when {
            matchedPreservative != null -> ClaimResult(
                claim = claim,
                verdict = Verdict.MISLEADING,
                frontText = claim.rawText,
                evidence = Evidence(
                    quote = matchedPreservative,
                    sourceField = Evidence.SourceField.INGREDIENTS,
                    key = "preservative_found"
                ),
                explanationEn = "The front claims \"${claim.rawText}\", but the ingredients list " +
                    "\"$matchedPreservative\" — a recognized preservative.",
                ruleKey = ruleKey
            )

            label.ingredients.isEmpty() ->
                notEnoughEvidence(claim, ruleKey)

            else -> consistent(
                claim = claim,
                ruleKey = ruleKey,
                explanation = "No chemical preservative ingredients or INS 200–299 codes were found on the ingredient list."
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rule 5 — "Organic"
    // ─────────────────────────────────────────────────────────────────────────

    private fun checkOrganic(claim: Claim, label: StructuredLabel): ClaimResult {
        val ruleKey = "organic"

        val certKeywords = listOf(
            "jaivik bharat", "npop", "usda organic", "india organic",
            "certified organic by", "organic certification", "lic no"
        )
        val certLine = findMatchingLine(label.rawBackText, certKeywords)

        return when {
            certLine != null -> ClaimResult(
                claim = claim,
                verdict = Verdict.CONSISTENT,
                frontText = claim.rawText,
                evidence = Evidence(
                    quote = certLine,
                    sourceField = Evidence.SourceField.FINE_PRINT,
                    key = "organic_cert"
                ),
                explanationEn = "The label includes an organic certification reference: \"$certLine\".",
                ruleKey = ruleKey
            )

            label.rawBackText.isBlank() ->
                notEnoughEvidence(claim, ruleKey)

            else -> ClaimResult(
                claim = claim,
                verdict = Verdict.NEEDS_CONTEXT,
                frontText = claim.rawText,
                evidence = Evidence.absent(),
                explanationEn = "The front claims \"${claim.rawText}\", but no recognized statutory organic " +
                    "certification mark or certification number was detected in the visible back-of-pack text.",
                ruleKey = ruleKey
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rule 6 — "High Protein"
    // ─────────────────────────────────────────────────────────────────────────

    private fun checkHighProtein(claim: Claim, label: StructuredLabel): ClaimResult {
        val ruleKey = "high_protein"
        val proteinG = label.nutritionTable[StructuredLabel.KEY_PROTEIN_G]
        val rawProteinLine = label.nutritionRawLines[StructuredLabel.KEY_PROTEIN_G]
            ?: findMatchingLine(label.rawBackText, "protein")

        val proteinSourceKeywords = listOf(
            "whey protein", "soy protein", "milk protein", "pea protein",
            "protein isolate", "casein", "egg white"
        )
        val proteinSource = label.ingredients.firstOrNull { ingredient ->
            proteinSourceKeywords.any { kw -> ingredient.lowercase().contains(kw) }
        }

        return when {
            proteinG != null && rawProteinLine != null -> {
                if (proteinG >= 10.0) {
                    ClaimResult(
                        claim = claim,
                        verdict = Verdict.CONSISTENT,
                        frontText = claim.rawText,
                        evidence = Evidence(
                            quote = rawProteinLine,
                            sourceField = Evidence.SourceField.NUTRITION_TABLE,
                            key = StructuredLabel.KEY_PROTEIN_G,
                            value = proteinG
                        ),
                        explanationEn = "The nutrition table indicates a significant protein content of ${proteinG}g per 100g.",
                        ruleKey = ruleKey
                    )
                } else {
                    ClaimResult(
                        claim = claim,
                        verdict = Verdict.NEEDS_CONTEXT,
                        frontText = claim.rawText,
                        evidence = Evidence(
                            quote = rawProteinLine,
                            sourceField = Evidence.SourceField.NUTRITION_TABLE,
                            key = StructuredLabel.KEY_PROTEIN_G,
                            value = proteinG
                        ),
                        explanationEn = "The front claims \"${claim.rawText}\", but the nutrition table shows " +
                            "${proteinG}g of protein per 100g, which may be modest relative to the claim.",
                        ruleKey = ruleKey
                    )
                }
            }

            proteinSource != null -> ClaimResult(
                claim = claim,
                verdict = Verdict.NEEDS_CONTEXT,
                frontText = claim.rawText,
                evidence = Evidence(
                    quote = proteinSource,
                    sourceField = Evidence.SourceField.INGREDIENTS,
                    key = "protein_source"
                ),
                explanationEn = "The front claims \"${claim.rawText}\". While \"$proteinSource\" is listed in ingredients, " +
                    "exact protein quantity per 100g was not readable in the nutrition table.",
                ruleKey = ruleKey
            )

            label.ingredients.isEmpty() && proteinG == null ->
                notEnoughEvidence(claim, ruleKey)

            else -> notEnoughEvidence(claim, ruleKey)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rule 7 — "Zero Trans Fat"
    // ─────────────────────────────────────────────────────────────────────────

    private fun checkZeroTransFat(claim: Claim, label: StructuredLabel): ClaimResult {
        val ruleKey = "zero_trans_fat"
        val transFatG = label.nutritionTable[StructuredLabel.KEY_TRANS_FAT_G]
        val rawTransFatLine = label.nutritionRawLines[StructuredLabel.KEY_TRANS_FAT_G]
            ?: findMatchingLine(label.rawBackText, "trans fat")

        val hydrogenatedOilKeywords = listOf(
            "partially hydrogenated", "hydrogenated vegetable oil",
            "hydrogenated fat", "vanaspati"
        )
        val matchedOil = label.ingredients.firstOrNull { ingredient ->
            hydrogenatedOilKeywords.any { kw -> ingredient.lowercase().contains(kw) }
        }

        return when {
            matchedOil != null -> ClaimResult(
                claim = claim,
                verdict = Verdict.NEEDS_CONTEXT,
                frontText = claim.rawText,
                evidence = Evidence(
                    quote = matchedOil,
                    sourceField = Evidence.SourceField.INGREDIENTS,
                    key = "trans_fat_source"
                ),
                explanationEn = "The front claims \"${claim.rawText}\", but the ingredients list \"$matchedOil\". " +
                    "Hydrogenated oils are a recognized source of trans fats even when per-serving numbers round down.",
                ruleKey = ruleKey
            )

            transFatG != null && transFatG > 0.2 && rawTransFatLine != null -> ClaimResult(
                claim = claim,
                verdict = Verdict.MISLEADING,
                frontText = claim.rawText,
                evidence = Evidence(
                    quote = rawTransFatLine,
                    sourceField = Evidence.SourceField.NUTRITION_TABLE,
                    key = StructuredLabel.KEY_TRANS_FAT_G,
                    value = transFatG
                ),
                explanationEn = "The front claims \"${claim.rawText}\", but the nutrition table discloses " +
                    "${transFatG}g of trans fat per 100g.",
                ruleKey = ruleKey
            )

            label.ingredients.isEmpty() && transFatG == null ->
                notEnoughEvidence(claim, ruleKey)

            else -> consistent(
                claim = claim,
                ruleKey = ruleKey,
                explanation = "No hydrogenated vegetable oils were found in ingredients, and trans fat is reported within zero-claim thresholds."
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rule 8 — Vague Wellness Claims ("Immunity Booster", "Detox", etc.)
    // ─────────────────────────────────────────────────────────────────────────

    private fun checkVagueWellness(claim: Claim, label: StructuredLabel): ClaimResult {
        val ruleKey = "vague_wellness"

        // Check A: Fine-print disclaimer
        val disclaimerKeywords = listOf(
            "not been evaluated", "does not diagnose", "not intended to treat",
            "results may vary", "individual results", "quality mark"
        )
        val disclaimer = label.finePrint.firstOrNull { line ->
            disclaimerKeywords.any { kw -> line.lowercase().contains(kw) }
        }

        // Check B: Supporting active ingredient
        val supportingKeywords = listOf(
            "vitamin c", "zinc", "amla", "tulsi", "ashwagandha",
            "curcumin", "turmeric", "echinacea", "vitamin d", "antioxidant"
        )
        val supportingIngredient = label.ingredients.firstOrNull { ingredient ->
            supportingKeywords.any { kw -> ingredient.lowercase().contains(kw) }
        }

        return when {
            disclaimer != null -> ClaimResult(
                claim = claim,
                verdict = Verdict.NEEDS_CONTEXT,
                frontText = claim.rawText,
                evidence = Evidence(
                    quote = disclaimer,
                    sourceField = Evidence.SourceField.FINE_PRINT,
                    key = "wellness_disclaimer"
                ),
                explanationEn = "The front claims \"${claim.rawText}\", but the back-of-pack fine print notes: " +
                    "\"$disclaimer\".",
                ruleKey = ruleKey
            )

            supportingIngredient != null -> ClaimResult(
                claim = claim,
                verdict = Verdict.CONSISTENT,
                frontText = claim.rawText,
                evidence = Evidence(
                    quote = supportingIngredient,
                    sourceField = Evidence.SourceField.INGREDIENTS,
                    key = "supporting_ingredient"
                ),
                explanationEn = "The ingredients include \"$supportingIngredient\", which is commonly associated with this claim.",
                ruleKey = ruleKey
            )

            label.ingredients.isEmpty() && label.finePrint.isEmpty() ->
                notEnoughEvidence(claim, ruleKey)

            else -> ClaimResult(
                claim = claim,
                verdict = Verdict.NEEDS_CONTEXT,
                frontText = claim.rawText,
                evidence = Evidence.absent(),
                explanationEn = "The front makes a wellness claim (\"${claim.rawText}\"), but no specific supporting active ingredient was clearly identifiable in the ingredient list.",
                ruleKey = ruleKey
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper builders
    // ─────────────────────────────────────────────────────────────────────────

    private fun consistent(claim: Claim, ruleKey: String, explanation: String): ClaimResult =
        ClaimResult(
            claim = claim,
            verdict = Verdict.CONSISTENT,
            frontText = claim.rawText,
            evidence = Evidence.absent(),
            explanationEn = explanation,
            ruleKey = ruleKey
        )

    fun notEnoughEvidence(claim: Claim, ruleKey: String): ClaimResult =
        ClaimResult(
            claim = claim,
            verdict = Verdict.NOT_ENOUGH_EVIDENCE,
            frontText = claim.rawText,
            evidence = Evidence.absent(),
            explanationEn = "Not enough clear label text was found on the back of the package to verify this claim.",
            ruleKey = ruleKey
        )

    private fun findMatchingLine(rawText: String, keyword: String): String? {
        return rawText.lines().firstOrNull { it.contains(keyword, ignoreCase = true) }?.trim()
    }

    private fun findMatchingLine(rawText: String, keywords: List<String>): String? {
        return rawText.lines().firstOrNull { line ->
            keywords.any { line.contains(it, ignoreCase = true) }
        }?.trim()
    }
}
