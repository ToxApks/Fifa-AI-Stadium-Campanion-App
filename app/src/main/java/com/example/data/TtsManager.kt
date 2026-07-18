package com.example.data

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

object TtsManager {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentSpokenText = MutableStateFlow<String?>(null)
    val currentSpokenText: StateFlow<String?> = _currentSpokenText.asStateFlow()

    fun initialize(context: Context) {
        if (tts != null) return
        
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                Log.d("TtsManager", "TextToSpeech successfully initialized.")
                
                tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        _currentSpokenText.value = null
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        _currentSpokenText.value = null
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        _isSpeaking.value = false
                        _currentSpokenText.value = null
                        Log.e("TtsManager", "TTS Error: $errorCode")
                    }
                })
            } else {
                Log.e("TtsManager", "TextToSpeech initialization failed.")
            }
        }
    }

    fun speak(text: String, language: Language) {
        val ttsEngine = tts ?: return
        if (!isInitialized) {
            Log.w("TtsManager", "TTS requested but engine is not initialized yet.")
            return
        }

        val locale = when (language) {
            Language.SPANISH -> Locale("es", "ES")
            Language.FRENCH -> Locale.FRENCH
            Language.PORTUGUESE -> Locale("pt", "PT")
            Language.ARABIC -> Locale("ar")
            Language.HINDI -> Locale("hi", "IN")
            Language.JAPANESE -> Locale.JAPANESE
            Language.GERMAN -> Locale.GERMAN
            Language.ITALIAN -> Locale.ITALY
            else -> Locale.US
        }
        
        ttsEngine.language = locale
        
        _currentSpokenText.value = text
        _isSpeaking.value = true
        
        val utteranceId = "stadium_narration_${System.currentTimeMillis()}"
        ttsEngine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
        _currentSpokenText.value = null
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
        _isSpeaking.value = false
        _currentSpokenText.value = null
    }
}
