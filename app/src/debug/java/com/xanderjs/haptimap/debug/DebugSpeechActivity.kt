package com.xanderjs.haptimap.debug

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.inputmethod.EditorInfo
import android.widget.EditText

/**
 * Solo debug y apagada por defecto (src/debug/AndroidManifest.xml): el micrófono del emulador no llega al
 * reconocedor, así que esta actividad hace de él. Muestra el EXTRA_PROMPT y devuelve lo escrito
 * (adb shell input text …, Intro) en EXTRA_RESULTS, igual que un reconocedor real.
 */
class DebugSpeechActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val field = EditText(this).apply {
            hint = intent.getStringExtra(RecognizerIntent.EXTRA_PROMPT)
            imeOptions = EditorInfo.IME_ACTION_DONE
            isSingleLine = true
            setOnEditorActionListener { view, _, _ ->
                val results = arrayListOf(view.text.toString())
                setResult(RESULT_OK, Intent().putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS, results))
                finish()
                true
            }
        }
        setContentView(field)
        field.requestFocus()
    }
}
