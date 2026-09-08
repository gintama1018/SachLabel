package com.sachlabel.app.health

import com.sachlabel.app.data.model.StructuredLabel
import com.sachlabel.app.data.model.UserContext

/**
 * Opt-in health context engine — Flow 6.
 *
 * This is a simple keyword cross-reference: user condition → ingredient relevance.
 * It NEVER produces a medical verdict, safe/unsafe binary, or numeric score.
 *
 * Every output from this engine carries [UserContext.MANDATORY_DISCLAIMER], always,
 * enforced at the template level — not left to any model or UI discretion.
 *
 * Plan.md Flow 6: "Output always passes through the fixed disclaimer template —
 * this is enforced at the template level, not left to model discretion."
 */
object HealthContextEngine {

    data class HealthContextResult(
        val relevantIngredients: List<String>,
        val note: String,
        /** Always present. Cannot be removed, collapsed, or varied. */
        val disclaimer: String = UserContext.MANDATORY_DISCLAIMER
    )

    /**
     * Cross-reference extracted ingredients against user's stated context.
     *
     * @param label     structured label from the product
     * @param context   user's opt-in health tags
     * @return [HealthContextResult] — always includes the mandatory disclaimer
     */
    fun analyze(label: StructuredLabel, context: UserContext): HealthContextResult {
        if (context.tags.isEmpty()) {
            return HealthContextResult(
                relevantIngredients = emptyList(),
                note = "No health context was provided."
            )
        }

        val relevantIngredients = mutableListOf<String>()
        val notes = mutableListOf<String>()

        for (tag in context.tags) {
            val mapping = CONDITION_INGREDIENT_MAP.entries
                .firstOrNull { tag.lowercase().contains(it.key.lowercase()) }
                ?: continue

            val matched = label.ingredients.filter { ingredient ->
                mapping.value.any { kw -> ingredient.lowercase().contains(kw) }
            }
            relevantIngredients.addAll(matched)

            if (matched.isNotEmpty()) {
                notes.add("Based on \"${tag}\": the back label lists ${matched.joinToString(", ")}.")
            }
        }

        val note = if (notes.isEmpty()) {
            "Nothing in this product's ingredient list appeared relevant to the concern(s) you mentioned."
        } else {
            notes.joinToString(" ")
        }

        return HealthContextResult(
            relevantIngredients = relevantIngredients.distinct(),
            note = note
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Condition → ingredient keyword mapping
    // ─────────────────────────────────────────────────────────────────────────

    private val CONDITION_INGREDIENT_MAP: Map<String, List<String>> = mapOf(
        "diabetic" to listOf(
            "sugar", "glucose", "fructose", "maltodextrin", "honey",
            "sucrose", "dextrose", "corn syrup", "high-fructose",
            "maltose", "molasses", "agave"
        ),
        "blood sugar" to listOf(
            "sugar", "glucose", "fructose", "maltodextrin"
        ),
        "high blood pressure" to listOf(
            "sodium", "salt", "sodium chloride", "monosodium glutamate",
            "soy sauce", "sodium benzoate", "baking soda"
        ),
        "low sodium" to listOf(
            "sodium", "salt", "sodium chloride", "monosodium glutamate"
        ),
        "gluten" to listOf(
            "wheat", "barley", "rye", "malt", "spelt", "semolina",
            "wheat flour", "wheat starch"
        ),
        "nut allergy" to listOf(
            "peanut", "almond", "cashew", "walnut", "hazelnut",
            "pistachio", "brazil nut", "pecan", "macadamia",
            "tree nut", "groundnut"
        ),
        "lactose" to listOf(
            "milk", "lactose", "whey", "casein", "butter",
            "cream", "skimmed milk", "milk powder", "milk solids"
        ),
        "vegan" to listOf(
            "milk", "egg", "honey", "gelatin", "whey", "casein",
            "butter", "cream", "chicken", "fish", "meat", "lard"
        )
    )
}
