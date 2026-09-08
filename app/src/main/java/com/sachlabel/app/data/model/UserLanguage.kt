package com.sachlabel.app.data.model

import java.util.Locale

/**
 * A supported UI + TTS language.
 *
 * @param code      BCP-47 language tag (e.g. "hi", "ta", "bn", "en")
 * @param displayName   human-readable name in that language
 * @param displayNameEn human-readable name in English (for settings UI fallback)
 * @param ttsLocale     java.util.Locale for Android TextToSpeech
 */
data class UserLanguage(
    val code: String,
    val displayName: String,
    val displayNameEn: String,
    val ttsLocale: Locale
) {
    companion object {
        val ENGLISH = UserLanguage(
            code = "en",
            displayName = "English",
            displayNameEn = "English",
            ttsLocale = Locale.ENGLISH
        )
        val HINDI = UserLanguage(
            code = "hi",
            displayName = "हिंदी",
            displayNameEn = "Hindi",
            ttsLocale = Locale("hi", "IN")
        )
        val TAMIL = UserLanguage(
            code = "ta",
            displayName = "தமிழ்",
            displayNameEn = "Tamil",
            ttsLocale = Locale("ta", "IN")
        )
        val BENGALI = UserLanguage(
            code = "bn",
            displayName = "বাংলা",
            displayNameEn = "Bengali",
            ttsLocale = Locale("bn", "IN")
        )

        val SUPPORTED = listOf(ENGLISH, HINDI, TAMIL, BENGALI)

        fun fromCode(code: String): UserLanguage =
            SUPPORTED.firstOrNull { it.code == code } ?: ENGLISH
    }
}
