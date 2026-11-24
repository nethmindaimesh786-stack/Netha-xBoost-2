package com.netha.keyboardultra

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.MotionEvent
import android.widget.LinearLayout
import android.widget.TextView
import android.app.AlertDialog
import android.os.Handler
import android.os.Looper
import kotlin.math.max

class NethaKeyboardView(context: Context, private val action: (String, String) -> Unit) : LinearLayout(context) {
    private val bg = Color.BLACK
    private val keyBg = Color.rgb(36, 36, 38)
    private val keyBgAlt = Color.rgb(48, 48, 51)
    private val purple = Color.rgb(163, 92, 255)
    private val letterKeys = mutableListOf<Pair<TextView, String>>()
    private var languageButton: TextView? = null
    private var numberButton: TextView? = null
    private var sinhala = false
    private var caps = false
    private var numbers = false
    private var azerty = false
    private val backspaceHandler = Handler(Looper.getMainLooper())
    private var repeating = false
    private val deleteRunnable = object : Runnable {
        override fun run() {
            if (repeating) { action("backspace", ""); backspaceHandler.postDelayed(this, 65) }
        }
    }

    init {
        orientation = VERTICAL
        setBackgroundColor(bg)
        setPadding(dp(4), dp(8), dp(4), dp(5))
        build()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun shape(color: Int, radius: Int = 9) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }

    private fun label(text: String, size: Float, color: Int = Color.WHITE, bold: Boolean = false): TextView =
        TextView(context).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            if (bold) typeface = Typeface.DEFAULT_BOLD
            isSingleLine = true
            includeFontPadding = false
        }

    private fun tool(text: String, actionName: String, value: String = ""): TextView =
        label(text, 15f, Color.WHITE, true).apply {
            background = shape(Color.rgb(0, 0, 0), 50)
            setOnClickListener { action(actionName, value) }
            layoutParams = LayoutParams(0, dp(42), 1f).apply { setMargins(dp(2), 0, dp(2), dp(5)) }
        }

    private fun build() {
        val toolbar = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER }
        val lang = tool("En", "language")
        languageButton = lang
        toolbar.addView(lang, LayoutParams(0, dp(42), 1f))
        toolbar.addView(tool("☻", "emoji"), LayoutParams(0, dp(42), 1f))
        toolbar.addView(tool("𝓐", "font"), LayoutParams(0, dp(42), 1f))
        toolbar.addView(tool("▤", "clipboard"), LayoutParams(0, dp(42), 1f))
        toolbar.addView(tool("Aa", "copy"), LayoutParams(0, dp(42), 1f))
        toolbar.addView(tool("🎙", "voice"), LayoutParams(0, dp(42), 1f))
        toolbar.addView(tool("⌨", "layout"), LayoutParams(0, dp(42), 1f))
        addView(toolbar)
        addView(emojiRow())
        addView(keyRow("QWERTYUIOP", true))
        addView(keyRow("ASDFGHJKL", false))
        addView(thirdRow())
        addView(bottomRow())
        val gesture = View(context).apply {
            background = shape(Color.rgb(165, 165, 168), 50)
            layoutParams = LayoutParams(dp(110), dp(4)).apply { gravity = Gravity.CENTER; topMargin = dp(10) }
        }
        addView(gesture)
    }

    private fun emojiRow(): View = LinearLayout(context).apply {
        orientation = HORIZONTAL
        listOf("🥲", "🔥", "🌪️", "🎂", "🎬", "★", "🙂", "😒", "☹️").forEach { e ->
            addView(label(e, 23f).apply {
                setOnClickListener { action("letter", e) }
                layoutParams = LayoutParams(0, dp(42), 1f)
            })
        }
    }

    private fun keyRow(chars: String, first: Boolean): View = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
        val values = chars.map { it.toString() }
        if (!first) setPadding(dp(14), 0, dp(14), 0)
        values.forEachIndexed { index, c ->
            val key = makeKey(c, if (first) (index + 1).toString() else "")
            addView(key, LayoutParams(0, dp(51), 1f).apply { setMargins(dp(2), dp(3), dp(2), dp(3)) })
        }
    }

    private fun thirdRow(): View = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
        addView(specialKey("⇧") { action("shift", "") }, LayoutParams(0, dp(51), 1.2f).apply { setMargins(dp(2), dp(3), dp(2), dp(3)) })
        "ZXCVBNM".forEach { c ->
            addView(makeKey(c.toString(), ""), LayoutParams(0, dp(51), 1f).apply { setMargins(dp(2), dp(3), dp(2), dp(3)) })
        }
        val del = specialKey("⌫") { action("backspace", "") }
        del.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { repeating = false; backspaceHandler.postDelayed({ repeating = true; backspaceHandler.post(deleteRunnable) }, 420) }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { repeating = false; backspaceHandler.removeCallbacks(deleteRunnable) }
            }
            false
        }
        addView(del, LayoutParams(0, dp(51), 1.2f).apply { setMargins(dp(2), dp(3), dp(2), dp(3)) })
    }

    private fun bottomRow(): View = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
        val num = specialKey("?123") { action("numbers", "") }
        numberButton = num
        addView(num, LayoutParams(0, dp(49), 1.3f).apply { setMargins(dp(2), dp(3), dp(2), dp(3)) })
        addView(specialKey("🌐") { action("language", "") }, LayoutParams(0, dp(49), 1f).apply { setMargins(dp(2), dp(3), dp(2), dp(3)) })
        addView(specialKey(",") { action("letter", ",") }, LayoutParams(0, dp(49), .65f).apply { setMargins(dp(2), dp(3), dp(2), dp(3)) })
        addView(TextView(context).apply {
            text = "English (US)"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
            background = shape(Color.WHITE, 40)
            setOnClickListener { action("space", "") }
        }, LayoutParams(0, dp(49), 4.2f).apply { setMargins(dp(2), dp(3), dp(2), dp(3)) })
        addView(specialKey(".") { action("letter", ".") }, LayoutParams(0, dp(49), .65f).apply { setMargins(dp(2), dp(3), dp(2), dp(3)) })
        addView(specialKey("↵", true) { action("enter", "") }, LayoutParams(0, dp(49), 1.3f).apply { setMargins(dp(2), dp(3), dp(2), dp(3)) })
    }

    private fun makeKey(char: String, sub: String): View {
        val box = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            background = shape(keyBg, 8)
            isClickable = true
            isFocusable = true
        }
        val main = label(if (caps) char.uppercase() else char, 23f, Color.WHITE, true)
        main.tag = char
        box.setOnClickListener { action("letter", (main.tag as? String ?: char).lowercase()) }
        // Native text remains crisp; key backgrounds stay independent from the RGB glyph styling.
        main.setTextColor(Color.rgb(235, 235, 240))
        box.addView(main, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        if (sub.isNotEmpty()) box.addView(label(sub, 9f, Color.LTGRAY))
        letterKeys.add(main to char)
        return box
    }

    private fun specialKey(text: String, accent: Boolean = false, click: () -> Unit): TextView =
        label(text, 20f, if (accent) Color.WHITE else Color.LTGRAY, true).apply {
            background = shape(if (accent) Color.rgb(72, 72, 76) else Color.BLACK, 9)
            setOnClickListener { click() }
        }

    fun setShift(value: Boolean) { caps = value; refreshKeyLabels() }
    fun setNumbers(value: Boolean) { numbers = value; numberButton?.text = if (numbers) "ABC" else "?123"; refreshKeyLabels() }
    fun setLanguage(value: Boolean) { sinhala = value; languageButton?.text = if (sinhala) "සිං" else "En" }
    fun setLayout(value: Boolean) {
        azerty = value
        val qwerty = "QWERTYUIOPASDFGHJKLZXCVBNM"
        val azertyChars = "AZERTYUIOPQSDFGHJKLMWXCVBN"
        letterKeys.forEachIndexed { index, pair ->
            val active = (if (azerty) azertyChars else qwerty).getOrNull(index)?.toString() ?: pair.second
            pair.first.tag = active
        }
        refreshKeyLabels()
        toast(if (azerty) "AZERTY layout selected" else "QWERTY layout selected")
    }
    private fun refreshKeyLabels() {
        val symbols = mapOf('q' to "1", 'w' to "2", 'e' to "3", 'r' to "4", 't' to "5", 'y' to "6", 'u' to "7", 'i' to "8", 'o' to "9", 'p' to "0", 'a' to "@", 's' to "#", 'd' to "$", 'f' to "%", 'g' to "&", 'h' to "-", 'j' to "+", 'k' to "(", 'l' to ")", 'z' to "*", 'c' to "'", 'v' to ":", 'b' to ";", 'n' to "!", 'm' to "?")
        letterKeys.forEach { (view, _) ->
            val ch = (view.tag as? String)?.firstOrNull() ?: return@forEach
            view.text = if (numbers) symbols[ch.lowercaseChar()] ?: ch.toString() else if (caps) ch.uppercaseChar().toString() else ch.toString().lowercase()
        }
    }

    fun showEmojiPicker(onPick: (String) -> Unit) {
        val emojis = arrayOf("😀", "😂", "🥲", "😍", "🔥", "❤️", "👍", "🙏", "🎂", "🎬", "😒", "🌟", "😭", "😊", "🚀")
        AlertDialog.Builder(context).setTitle("Emoji").setItems(emojis) { _, which -> onPick(emojis[which]) }.show()
    }

    private fun toast(message: String) = android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
}
