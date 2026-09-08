package com.sachlabel.app.data.mock

import com.sachlabel.app.data.model.*

/**
 * Pre-built mock scenarios for demo/testing without real OCR.
 *
 * Based on the 5 scenarios defined in the implementation spec:
 * A = 100% Natural → NEEDS_CONTEXT
 * B = No Preservatives + preservative → MISLEADING
 * C = No Added Sugar + sweetener → NEEDS_CONTEXT
 * D = High Protein with verified value → CONSISTENT
 * E = No claim detected → NO_CLAIM_DETECTED
 */
object MockProducts {

    // ─────────────────────────────────────────────
    // Scenario A — "100% Natural" with qualifying fine print
    // Expected: NEEDS_CONTEXT
    // ─────────────────────────────────────────────
    val SCENARIO_A = MockScenario(
        label = "Scenario A — 100% Natural",
        description = "Front claims \"100% Natural\", fine print qualifies it",
        structuredLabel = StructuredLabel(
            frontClaimsRaw = listOf("100% Natural", "Farm Fresh"),
            ingredients = listOf(
                "Refined wheat flour", "Sugar", "Edible vegetable oil",
                "Salt", "Artificial flavour (vanilla)"
            ),
            nutritionTable = mapOf(
                StructuredLabel.KEY_ENERGY_KCAL to 420.0,
                StructuredLabel.KEY_SUGARS_G to 18.0,
                StructuredLabel.KEY_PROTEIN_G to 6.0,
                StructuredLabel.KEY_TOTAL_FAT_G to 14.0
            ),
            finePrint = listOf(
                "\"100% Natural\" is a quality mark and does not imply the product is free from processing aids.",
                "Manufactured in a facility that also processes nuts and soy.",
                "This statement has not been evaluated by FSSAI as a health claim."
            ),
            rawBackText = "Ingredients: Refined wheat flour, Sugar, Edible vegetable oil, Salt, Artificial flavour (vanilla)\n" +
                "Nutrition per 100g: Energy 420kcal, Sugars 18g, Protein 6g, Fat 14g\n" +
                "\"100% Natural\" is a quality mark and does not imply the product is free from processing aids. " +
                "Manufactured in a facility that also processes nuts and soy. " +
                "This statement has not been evaluated by FSSAI as a health claim."
        ),
        expectedVerdict = Verdict.NEEDS_CONTEXT
    )

    // ─────────────────────────────────────────────
    // Scenario B — "No Preservatives" but sodium benzoate listed
    // Expected: MISLEADING
    // ─────────────────────────────────────────────
    val SCENARIO_B = MockScenario(
        label = "Scenario B — No Preservatives",
        description = "Front claims \"No Preservatives\", back lists sodium benzoate",
        structuredLabel = StructuredLabel(
            frontClaimsRaw = listOf("No Preservatives", "No Artificial Colors"),
            ingredients = listOf(
                "Water", "Sugar", "Citric acid",
                "Sodium benzoate (INS 211)", "Artificial colour (tartrazine, INS 102)",
                "Natural and artificial mango flavour"
            ),
            nutritionTable = mapOf(
                StructuredLabel.KEY_ENERGY_KCAL to 55.0,
                StructuredLabel.KEY_SUGARS_G to 13.0,
                StructuredLabel.KEY_PROTEIN_G to 0.0,
                StructuredLabel.KEY_SODIUM_MG to 35.0
            ),
            finePrint = listOf(
                "Best before: see bottom of pack.",
                "Store in a cool, dry place."
            ),
            rawBackText = "Ingredients: Water, Sugar, Citric acid, Sodium benzoate (INS 211), " +
                "Artificial colour (tartrazine, INS 102), Natural and artificial mango flavour\n" +
                "Nutrition per 100ml: Energy 55kcal, Sugars 13g, Protein 0g, Sodium 35mg\n" +
                "Best before: see bottom of pack. Store in a cool, dry place."
        ),
        expectedVerdict = Verdict.MISLEADING
    )

    // ─────────────────────────────────────────────
    // Scenario C — "No Added Sugar" but maltodextrin + nutrition shows sugars
    // Expected: NEEDS_CONTEXT
    // ─────────────────────────────────────────────
    val SCENARIO_C = MockScenario(
        label = "Scenario C — No Added Sugar",
        description = "Front claims \"No Added Sugar\", back has maltodextrin + 8g sugars/serving",
        structuredLabel = StructuredLabel(
            frontClaimsRaw = listOf("No Added Sugar", "High Fibre"),
            ingredients = listOf(
                "Whole grain oats", "Maltodextrin", "Skimmed milk powder",
                "Cocoa powder", "Salt", "Vitamins and minerals"
            ),
            nutritionTable = mapOf(
                StructuredLabel.KEY_ENERGY_KCAL to 375.0,
                StructuredLabel.KEY_SUGARS_G to 8.0,
                StructuredLabel.KEY_PROTEIN_G to 9.0,
                StructuredLabel.KEY_TOTAL_FAT_G to 6.5,
                StructuredLabel.KEY_CARBS_G to 62.0
            ),
            finePrint = listOf(
                "Sugars occur naturally from milk and oats.",
                "No sucrose or fructose syrup added during manufacturing."
            ),
            rawBackText = "Ingredients: Whole grain oats, Maltodextrin, Skimmed milk powder, Cocoa powder, Salt, Vitamins and minerals\n" +
                "Nutrition per 100g: Energy 375kcal, Sugars 8g, Protein 9g, Fat 6.5g, Carbohydrates 62g\n" +
                "Sugars occur naturally from milk and oats. " +
                "No sucrose or fructose syrup added during manufacturing."
        ),
        expectedVerdict = Verdict.NEEDS_CONTEXT
    )

    // ─────────────────────────────────────────────
    // Scenario D — "High Protein" with genuinely high protein
    // Expected: CONSISTENT (clean product — verify false-positive protection)
    // ─────────────────────────────────────────────
    val SCENARIO_D = MockScenario(
        label = "Scenario D — High Protein (clean)",
        description = "Front claims \"High Protein\", back label genuinely shows 30g protein/100g",
        structuredLabel = StructuredLabel(
            frontClaimsRaw = listOf("High Protein", "Whey Protein Isolate"),
            ingredients = listOf(
                "Whey protein isolate (80%)", "Cocoa powder", "Soy lecithin",
                "Sucralose", "Vanilla flavour"
            ),
            nutritionTable = mapOf(
                StructuredLabel.KEY_ENERGY_KCAL to 385.0,
                StructuredLabel.KEY_PROTEIN_G to 30.0,
                StructuredLabel.KEY_SUGARS_G to 2.0,
                StructuredLabel.KEY_TOTAL_FAT_G to 5.0,
                StructuredLabel.KEY_CARBS_G to 8.0
            ),
            finePrint = listOf(
                "Per 30g serving provides 9g protein.",
                "Not a substitute for a varied diet."
            ),
            rawBackText = "Ingredients: Whey protein isolate (80%), Cocoa powder, Soy lecithin, Sucralose, Vanilla flavour\n" +
                "Nutrition per 100g: Energy 385kcal, Protein 30g, Sugars 2g, Fat 5g, Carbohydrates 8g\n" +
                "Per 30g serving provides 9g protein. Not a substitute for a varied diet."
        ),
        expectedVerdict = Verdict.CONSISTENT
    )

    // ─────────────────────────────────────────────
    // Scenario E — No prominent claim on front
    // Expected: NO_CLAIM_DETECTED
    // ─────────────────────────────────────────────
    val SCENARIO_E = MockScenario(
        label = "Scenario E — No claim detected",
        description = "Front has only brand name and product description, no verifiable claims",
        structuredLabel = StructuredLabel(
            frontClaimsRaw = listOf("Mango Delight", "Premium Quality"),
            ingredients = listOf(
                "Mango pulp (65%)", "Sugar", "Citric acid", "Water",
                "Pectin"
            ),
            nutritionTable = mapOf(
                StructuredLabel.KEY_ENERGY_KCAL to 280.0,
                StructuredLabel.KEY_SUGARS_G to 55.0
            ),
            finePrint = listOf("Store refrigerated after opening."),
            rawBackText = "Ingredients: Mango pulp (65%), Sugar, Citric acid, Water, Pectin\n" +
                "Nutrition per 100g: Energy 280kcal, Sugars 55g\n" +
                "Store refrigerated after opening."
        ),
        expectedVerdict = Verdict.NO_CLAIM_DETECTED
    )

    val ALL = listOf(SCENARIO_A, SCENARIO_B, SCENARIO_C, SCENARIO_D, SCENARIO_E)
}

/**
 * A pre-packaged demo scenario with its expected verdict for testing.
 */
data class MockScenario(
    val label: String,
    val description: String,
    val structuredLabel: StructuredLabel,
    val expectedVerdict: Verdict
)
