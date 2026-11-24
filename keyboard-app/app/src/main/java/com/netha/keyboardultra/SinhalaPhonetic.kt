package com.netha.keyboardultra

object SinhalaPhonetic {
    private val words = mapOf(
        "a" to "ඇ",
        "ayiya" to "අයිය", "ayiya " to "අයිය ", "ayyaa" to "අයියා",
        "thaaththas" to "තාත්තා", "thaththa" to "තාත්තා", "thaaththa" to "තාත්තා",
        "amma" to "අම්මා", "ammaa" to "අම්මා", "kari ya" to "කැරියා",
        "kariya" to "කැරියා", "prashna" to "ප්‍රශ්න", "prashnaya" to "ප්‍රශ්නය",
        "kohomada" to "කොහොමද", "oyaa" to "ඔයා", "oya" to "ඔයා",
        "mama" to "මම", "mata" to "මට", "mage" to "මගේ", "hari" to "හරි",
        "ela" to "එල", "ehema" to "එහෙම", "nadda" to "නැද්ද", "na" to "නෑ",
        "ow" to "ඔව්", "ane" to "අනේ", "machan" to "මචං", "sinhala" to "සිංහල",
        "issara" to "ඉස්සර", "dan" to "දැන්", "karanna" to "කරන්න", "puluwan" to "පුළුවන්"
    )
    private val vowels = linkedMapOf(
        "aa" to "ා", "a" to "", "ae" to "ැ", "aee" to "ෑ",
        "ii" to "ී", "i" to "ි", "uu" to "ූ", "u" to "ු",
        "ee" to "ේ", "e" to "ෙ", "oo" to "ෝ", "o" to "ො"
    )
    private val independent = mapOf(
        "aa" to "ආ", "a" to "අ", "ae" to "ඇ", "aee" to "ඈ",
        "ii" to "ඊ", "i" to "ඉ", "uu" to "ඌ", "u" to "උ",
        "ee" to "ඒ", "e" to "එ", "oo" to "ඕ", "o" to "ඔ"
    )
    private val consonants = linkedMapOf(
        "th" to "ත", "dh" to "ද", "kh" to "ඛ", "gh" to "ඝ",
        "ch" to "ච", "sh" to "ශ", "ssh" to "ෂ", "ng" to "ඟ",
        "ny" to "ඤ", "nn" to "ණ", "n" to "න", "k" to "ක",
        "g" to "ග", "c" to "ච", "j" to "ජ", "t" to "ට",
        "d" to "ඩ", "p" to "ප", "b" to "බ", "m" to "ම",
        "y" to "ය", "r" to "ර", "l" to "ල", "w" to "ව",
        "v" to "ව", "s" to "ස", "h" to "හ", "f" to "ෆ"
    )
    private val tokens = (consonants.keys + vowels.keys).distinct().sortedByDescending { it.length }

    fun convert(input: String): String {
        if (input.isEmpty()) return ""
        words[input.lowercase()]?.let { return it }
        val s = input.lowercase()
        val out = StringBuilder()
        var i = 0
        while (i < s.length) {
            var found: String? = null
            for (token in tokens) {
                if (s.startsWith(token, i)) { found = token; break }
            }
            if (found == null) {
                out.append(s[i]); i++; continue
            }
            if (consonants.containsKey(found)) {
                val base = consonants.getValue(found)
                var vowel: String? = null
                for (v in vowels.keys.sortedByDescending { it.length }) {
                    if (s.startsWith(v, i + found.length)) { vowel = v; break }
                }
                if (vowel != null) {
                    val sign = vowels.getValue(vowel)
                    out.append(if (sign.isEmpty()) base else base + sign)
                    i += found.length + vowel.length
                } else {
                    out.append(base).append("්")
                    i += found.length
                }
            } else {
                out.append(independent[found] ?: found)
                i += found.length
            }
        }
        return out.toString()
    }
}
