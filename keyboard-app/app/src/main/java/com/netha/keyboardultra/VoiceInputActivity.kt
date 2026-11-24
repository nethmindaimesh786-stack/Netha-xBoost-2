package com.netha.keyboardultra

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast

class VoiceInputActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val locale = intent.getStringExtra("locale") ?: "en-US"
        val speech = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale)
            putExtra(RecognizerIntent.EXTRA_PROMPT, if (locale == "si-LK") "සිංහලෙන් කතා කරන්න" else "Speak now")
        }
        try { startActivityForResult(speech, 710) }
        catch (_: Exception) { Toast.makeText(this, "Voice input is not available", Toast.LENGTH_SHORT).show(); finish() }
    }

    @Deprecated("Deprecated in Android API; retained for speech activity compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 710 && resultCode == RESULT_OK) {
            val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            if (spoken.isNotBlank()) {
                sendBroadcast(Intent("com.netha.keyboardultra.VOICE_RESULT").setPackage(packageName).putExtra("text", spoken))
            }
        }
        finish()
    }
}
