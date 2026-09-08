package com.purewords1611.android.study.ui

object OrthographyUtils {
    /**
     * Modernizes 1611 orthography while keeping the original wording.
     * Handles u/v and i/j swaps.
     */
    fun modernize(text: String): String {
        return text.split(" ").joinToString(" ") { word ->
            var w = word
            // 1. Handle i/j swap (usually initial I for J)
            if (w.startsWith("I") && w.length > 1 && isVowel(w[1])) {
                // If it's "I" followed by a vowel, it might be "J"
                // but 1611 used "I" for both. Modern readers prefer "J".
                val suspiciousInitialI = listOf("Iesus", "Ierusalem", "Iohn", "Ioseph", "Iudah", "Iacob")
                val clean = w.filter { it.isLetter() }
                if (suspiciousInitialI.contains(clean)) {
                    w = w.replaceFirst("I", "J")
                }
            }
            
            // 2. Handle u/v swap
            // In 1611: 
            // - 'v' was used at the beginning of a word (vnto)
            // - 'u' was used in the middle of a word (euery)
            
            val result = StringBuilder()
            w.forEachIndexed { index, char ->
                val modernizedChar = when {
                    // Initial 'v' -> 'u' if it should be a vowel (rare in 1611, usually v was consonant)
                    // Actually, usually 'v' at start was 'u' if it's "vnto" -> "unto"
                    index == 0 && char.lowercaseChar() == 'v' -> {
                        if (isVowelWordStart(w)) {
                           if (char.isUpperCase()) 'U' else 'u'
                        } else char
                    }
                    // Medial 'u' -> 'v' if it should be a consonant
                    index > 0 && char.lowercaseChar() == 'u' -> {
                        if (isConsonantPosition(w, index)) {
                            if (char.isUpperCase()) 'V' else 'v'
                        } else char
                    }
                    else -> char
                }
                result.append(modernizedChar)
            }
            result.toString()
        }
    }

    private fun isVowel(c: Char): Boolean = "aeiouAEIOU".contains(c)

    private fun isVowelWordStart(word: String): Boolean {
        val clean = word.lowercase().filter { it.isLetter() }
        return clean.startsWith("vnto") || clean.startsWith("vp") || clean.startsWith("vs")
    }

    private fun isConsonantPosition(word: String, index: Int): Boolean {
        // Simple heuristic: if 'u' is between two vowels, it's likely a 'v' (e.g. euery, loue, haue)
        if (index <= 0 || index >= word.length - 1) return false
        val prev = word[index - 1].lowercaseChar()
        val next = word[index + 1].lowercaseChar()
        
        // common 1611 patterns: euery, loue, haue, liue, giue, graue
        val commonV = listOf("euery", "loue", "haue", "liue", "giue", "heauen", "deuill", "sauiour")
        val clean = word.lowercase().filter { it.isLetter() }
        if (commonV.any { clean.contains(it) }) {
            // Check if this specific 'u' is the one in the pattern
            // For simplicity, if the word is in the list and we are at a likely position
            return true
        }
        
        return isVowel(prev) && isVowel(next)
    }
}
