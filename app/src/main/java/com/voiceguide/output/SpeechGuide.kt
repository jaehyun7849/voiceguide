package com.voiceguide.output

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class SpeechGuide(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var ready = false

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            tts?.language = Locale.KOREAN
        }
    }

    fun speak(message: String) {
        if (ready) {
            tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "voice-guide-alert")
        }
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        ready = false
    }
}
