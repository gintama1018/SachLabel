package com.sachlabel.app.engine

import com.sachlabel.app.data.model.ClaimResult
import com.sachlabel.app.data.model.UserLanguage
import com.sachlabel.app.data.model.Verdict

/**
 * Template-based explanation generator for all result types × supported languages.
 *
 * Architecture §3.5: "Constrain with a template, not free generation."
 * This is the Phase 2 approach — no LLM needed for the core demo.
 *
 * Templates are filled with the specific claim text and evidence quote.
 * Phase 6 can optionally add an on-device LLM for more natural phrasing.
 */
object ExplanationTemplates {

    /**
     * Returns the localized explanation for a result in the given language.
     * Falls back to the English explanation if no localized template is defined.
     */
    fun get(result: ClaimResult, language: UserLanguage): String {
        if (language.code == "en") return result.explanationEn
        val template = TEMPLATES[language.code] ?: return result.explanationEn
        return template.format(result) ?: result.explanationEn
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Per-language template sets
    // ─────────────────────────────────────────────────────────────────────────

    private val TEMPLATES: Map<String, LanguageTemplateSet> = mapOf(
        "hi" to HindiTemplates,
        "ta" to TamilTemplates,
        "bn" to BengaliTemplates
    )

    private interface LanguageTemplateSet {
        fun format(result: ClaimResult): String?
    }

    // ── Hindi templates ──
    private object HindiTemplates : LanguageTemplateSet {
        override fun format(result: ClaimResult): String? {
            val claim = result.frontText
            val evidence = result.evidence.quote
            return when (result.verdict) {
                Verdict.MISLEADING ->
                    "पैकेट के सामने \"$claim\" लिखा है, लेकिन पीछे \"$evidence\" दर्ज है — " +
                    "जो इस दावे के विपरीत है।"
                Verdict.NEEDS_CONTEXT ->
                    "पैकेट के सामने \"$claim\" लिखा है। पीछे का लेबल बताता है: \"$evidence\" — " +
                    "इसलिए यह जानकारी ज़रूरी है।"
                Verdict.CONSISTENT ->
                    "\"$claim\" का दावा पीछे के लेबल से मेल खाता है।"
                Verdict.NOT_ENOUGH_EVIDENCE ->
                    "इस दावे की जाँच के लिए पर्याप्त जानकारी नहीं मिली।"
                Verdict.NO_CLAIM_DETECTED ->
                    "इस उत्पाद के सामने कोई प्रमुख दावा नहीं मिला।"
            }
        }
    }

    // ── Tamil templates ──
    private object TamilTemplates : LanguageTemplateSet {
        override fun format(result: ClaimResult): String? {
            val claim = result.frontText
            val evidence = result.evidence.quote
            return when (result.verdict) {
                Verdict.MISLEADING ->
                    "பொருளின் முன்பக்கத்தில் \"$claim\" என்று உள்ளது, ஆனால் பின்பக்கத்தில் " +
                    "\"$evidence\" பட்டியலிடப்பட்டுள்ளது — இது முரண்பட்டதாக உள்ளது."
                Verdict.NEEDS_CONTEXT ->
                    "முன்பக்கத்தில் \"$claim\" என்று உள்ளது. பின்பக்க லேபில்: \"$evidence\" — " +
                    "கூடுதல் தகவல் தேவை."
                Verdict.CONSISTENT ->
                    "\"$claim\" என்ற கோரிக்கை பின்பக்க லேபிலுடன் ஒத்துப்போகிறது."
                Verdict.NOT_ENOUGH_EVIDENCE ->
                    "இந்தக் கோரிக்கையை சரிபார்க்க போதுமான தகவல் இல்லை."
                Verdict.NO_CLAIM_DETECTED ->
                    "இந்தப் பொருளின் முன்பக்கத்தில் முக்கியமான கோரிக்கை எதுவும் இல்லை."
            }
        }
    }

    // ── Bengali templates ──
    private object BengaliTemplates : LanguageTemplateSet {
        override fun format(result: ClaimResult): String? {
            val claim = result.frontText
            val evidence = result.evidence.quote
            return when (result.verdict) {
                Verdict.MISLEADING ->
                    "প্যাকেটের সামনে \"$claim\" লেখা আছে, কিন্তু পেছনে \"$evidence\" তালিকাভুক্ত — " +
                    "যা এই দাবির বিপরীত।"
                Verdict.NEEDS_CONTEXT ->
                    "সামনে \"$claim\" লেখা আছে। পেছনের লেবেলে: \"$evidence\" — " +
                    "এই তথ্য জানা জরুরি।"
                Verdict.CONSISTENT ->
                    "\"$claim\" দাবিটি পেছনের লেবেলের সাথে সামঞ্জস্যপূর্ণ।"
                Verdict.NOT_ENOUGH_EVIDENCE ->
                    "এই দাবি যাচাই করার জন্য যথেষ্ট তথ্য পাওয়া যায়নি।"
                Verdict.NO_CLAIM_DETECTED ->
                    "এই পণ্যের সামনে কোনো বিশিষ্ট দাবি শনাক্ত হয়নি।"
            }
        }
    }
}
