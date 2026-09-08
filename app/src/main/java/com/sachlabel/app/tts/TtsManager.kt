package com.sachlabel.app.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.sachlabel.app.data.model.UserLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Android TextToSpeech wrapper for on-device TTS playback.
 *
 * Architecture.md §3.6: Android's built-in TextToSpeech API supports Hindi and
 * several major Indian regional languages on-device. No custom TTS model needed.
 *
 * Key design choices:
 * - No RECORD_AUDIO permission needed (TTS is output only)
 * - Language availability is checked before attempting playback
 * - Falls back to English if the selected language is unavailable
 * - Exposes [playbackState] for UI binding (play/pause/stop indicators)
 */
class TtsManager(private val context: Context) {

    enum class PlaybackState {
        IDLE, SPEAKING, ERROR, LANGUAGE_UNAVAILABLE
    }

    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingText: String? = null
    private var pendingLocale: Locale? = null

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(context) { status ->
            isInitialized = status == TextToSpeech.SUCCESS
            if (isInitialized) {
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _playbackState.value = PlaybackState.SPEAKING
                    }
                    override fun onDone(utteranceId: String?) {
                        _playbackState.value = PlaybackState.IDLE
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _playbackState.value = PlaybackState.ERROR
                    }
                })
                // Play any pending text that arrived before init completed
                val text = pendingText
                val locale = pendingLocale
                if (text != null && locale != null) {
                    speak(text, locale)
                    pendingText = null
                    pendingLocale = null
                }
            }
        }
    }

    /**
     * Speak the given text in the user's selected language.
     * Falls back to English if the language is unavailable on-device.
     *
     * Per design.md: "If TTS is unavailable for the selected language on-device,
     * fall back to text-only with a visible note — never fail silently."
     */
    fun speak(text: String, language: UserLanguage) {
        speak(text, language.ttsLocale)
    }

    fun speak(text: String, locale: Locale) {
        if (!isInitialized) {
            // Queue for after init
            pendingText = text
            pendingLocale = locale
            return
        }

        val result = tts?.setLanguage(locale)
        val effectiveLocale = when (result) {
            TextToSpeech.LANG_MISSING_DATA, TextToSpeech.LANG_NOT_SUPPORTED -> {
                // Fall back to English
                tts?.setLanguage(Locale.ENGLISH)
                _playbackState.value = PlaybackState.LANGUAGE_UNAVAILABLE
                Locale.ENGLISH
            }
            else -> locale
        }

        tts?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            UTTERANCE_ID
        )
    }

    /**
     * Check if a language is available for TTS on this device.
     * Returns true if available, false if language data is missing.
     */
    fun isLanguageAvailable(language: UserLanguage): Boolean {
        if (!isInitialized) return false
        val result = tts?.isLanguageAvailable(language.ttsLocale) ?: return false
        return result != TextToSpeech.LANG_MISSING_DATA &&
               result != TextToSpeech.LANG_NOT_SUPPORTED
    }

    fun stop() {
        tts?.stop()
        _playbackState.value = PlaybackState.IDLE
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    companion object {
        private const val UTTERANCE_ID = "sachlabel_tts"
    }
}
