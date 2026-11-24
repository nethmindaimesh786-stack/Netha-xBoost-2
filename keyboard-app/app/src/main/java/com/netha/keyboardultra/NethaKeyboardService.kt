package com.netha.keyboardultra

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.Toast

class NethaKeyboardService : InputMethodService() {
    private var currentWord = StringBuilder()
    private var sinhala = false
    private var caps = false
    private var numbers = false
    private var azerty = false
    private var fontStyle = "normal"
    private var keyboardView: NethaKeyboardView? = null

    override fun onCreateInputView(): View {
        keyboardView = NethaKeyboardView(this) { action, value -> handleKey(action, value) }
        return keyboardView!!
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        currentWord.clear()
    }

    private fun ic(): InputConnection? = currentInputConnection

    private fun previewWord() {
        val text = if (sinhala) SinhalaPhonetic.convert(currentWord.toString()) else currentWord.toString()
        ic()?.setComposingText(text, 1)
    }

    private fun commitWord() {
        if (currentWord.isNotEmpty()) {
            val text = if (sinhala) SinhalaPhonetic.convert(currentWord.toString()) else currentWord.toString()
            ic()?.commitText(applyFont(text), 1)
            currentWord.clear()
        }
    }

    private fun applyFont(text: String): String {
        val starts = when (fontStyle) {
            "bold" -> 0x1D400 to 0x1D41A
            "italic" -> 0x1D434 to 0x1D44E
            "mono" -> 0x1D670 to 0x1D68A
            else -> return text
        }
        return buildString {
            text.forEach { ch ->
                when (ch) {
                    in 'A'..'Z' -> appendCodePoint(starts.first + ch.code - 65)
                    in 'a'..'z' -> appendCodePoint(starts.second + ch.code - 97)
                    else -> append(ch)
                }
            }
        }
    }

    private fun handleKey(action: String, value: String) {
        when (action) {
            "letter" -> {
                val raw = if (numbers) symbolFor(value) else if (caps && !sinhala) value.uppercase() else value.lowercase()
                if (sinhala && raw.length == 1 && raw[0].isLetter() && raw[0].code < 128) {
                    currentWord.append(raw.lowercase())
                    previewWord()
                } else {
                    commitWord()
                    ic()?.commitText(applyFont(raw), 1)
                }
            }
            "space" -> { commitWord(); ic()?.commitText(" ", 1) }
            "enter" -> { commitWord(); ic()?.commitText("\n", 1) }
            "backspace" -> {
                if (currentWord.isNotEmpty()) {
                    currentWord.deleteCharAt(currentWord.length - 1)
                    if (currentWord.isEmpty()) ic()?.finishComposingText() else previewWord()
                } else ic()?.deleteSurroundingText(1, 0)
            }
            "shift" -> { caps = !caps; keyboardView?.setShift(caps) }
            "numbers" -> { numbers = !numbers; keyboardView?.setNumbers(numbers) }
            "language" -> {
                commitWord(); sinhala = !sinhala
                keyboardView?.setLanguage(sinhala)
                toast(if (sinhala) "සිංහල phonetic" else "English")
            }
            "emoji" -> keyboardView?.showEmojiPicker { emoji -> commitWord(); ic()?.commitText(emoji, 1) }
            "clipboard" -> {
                commitWord()
                val clip = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                if (clip.hasPrimaryClip()) ic()?.commitText(clip.primaryClip?.getItemAt(0)?.coerceToText(this) ?: "", 1)
                else toast("Clipboard is empty")
            }
            "copy" -> toast("Select text in the app and use its Copy command")
            "font" -> {
                fontStyle = when (fontStyle) { "normal" -> "bold"; "bold" -> "italic"; "italic" -> "mono"; else -> "normal" }
                toast("Font: $fontStyle")
            }
            "layout" -> { azerty = !azerty; keyboardView?.setLayout(azerty) }
            "voice" -> toast("Use the microphone button on your phone's voice input")
            "clear" -> { currentWord.clear(); ic()?.finishComposingText(); toast("Composing text cleared") }
        }
    }

    private fun symbolFor(key: String): String {
        val map = mapOf("q" to "1", "w" to "2", "e" to "3", "r" to "4", "t" to "5", "y" to "6", "u" to "7", "i" to "8", "o" to "9", "p" to "0", "a" to "@", "s" to "#", "d" to "$", "f" to "%", "g" to "&", "h" to "-", "j" to "+", "k" to "(", "l" to ")", "z" to "*", "x" to "\"", "c" to "'", "v" to ":", "b" to ";", "n" to "!", "m" to "?")
        return map[key.lowercase()] ?: key
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
