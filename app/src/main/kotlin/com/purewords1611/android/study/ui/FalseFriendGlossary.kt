package com.purewords1611.android.study.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

object FalseFriendGlossary {
    // Expanded set of 1611 false friends with clearer differentiation
    private val falseFriends = mapOf(
        // Existing terms with modern vs 1611 meaning
        "conversation" to "1611: Conduct/behavior | Modern: Chat",
        "prevent" to "1611: To go before | Modern: Stop beforehand",
        "allow" to "1611: Approve | Modern: Permit",
        "careful" to "1611: Anxious | Modern: Cautious",
        "charity" to "1611: Brotherly love | Modern: Almsgiving",
        "halt" to "1611: Limp | Modern: Stop",
        "let" to "1611: Hinder | Modern: Allow",
        "nephews" to "1611: Descendants | Modern: Grandsons",
        "peculiar" to "1611: One's own | Modern: Unique",
        "quick" to "1611: Living | Modern: Fast",
        "suffer" to "1611: Permit | Modern: Endure",
        
        // Additional historical terms
        "advent" to "1611: Arrival | Modern: Christmas season",
        "answer" to "1611: Give thought | Modern: Reply",
        "beast" to "1611: Animal | Modern: Fierce creature",
        "bless" to "1611: Consecrate | Modern: Divine favor",
        "body" to "1611: Corpse | Modern: Physical form",
        "chamber" to "1611: Bedroom | Modern: Room",
        "child" to "1611: Offspring | Modern: Minor person",
        "company" to "1611: Attendance | Modern: Business",
        "deceit" to "1611: Disguise | Modern: Fraud",
        "degree" to "1611: Rank | Modern: Academic title",
        "desire" to "1611: Craving | Modern: Wish",
        "dispute" to "1611: Contend | Modern: Argue",
        "elder" to "1611: Senior | Modern: Church official",
        "end" to "1611: Purpose | Modern: Conclusion",
        "flesh" to "1611: Human nature | Modern: Meat",
        "fornication" to "1611: Immorality | Modern: Premarital sex",
        "fruit" to "1611: Result | Modern: Plant produce",
        "gift" to "1611: Charisma | Modern: Present",
        "grace" to "1611: Divine favor | Modern: Elegance",
        "heathen" to "1611: Pagan | Modern: Non-Christian",
        "holy" to "1611: Set apart | Modern: Pious",
        "knowledge" to "1611: Wisdom | Modern: Information",
        "light" to "1611: Mild | Modern: Not heavy",
        "meek" to "1611: Gentle | Modern: Timid",
        "mourn" to "1611: Lament | Modern: Mourn",
        "number" to "1611: Rank | Modern: Quantity",
        "offence" to "1611: Blasphemy | Modern: Insult",
        "old" to "1611: Mature | Modern: Aged",
        "perfect" to "1611: Complete | Modern: Flawless",
        "perfectly" to "1611: Entirely | Modern: Totally",
        "picture" to "1611: Image | Modern: Painting",
        "poor" to "1611: Needy | Modern: Destitute",
        "port" to "1611: Harbor | Modern: Port city",
        "precious" to "1611: Priceless | Modern: Valuable",
        "purchase" to "1611: Obtain | Modern: Buy",
        "reprove" to "1611: Convince | Modern: Scold",
        "righteous" to "1611: Just | Modern: Moral",
        "saint" to "1611: Holy person | Modern: Canonized",
        "salt" to "1611: Preservative | Modern: Condiment",
        "save" to "1611: Rescue | Modern: Store",
        "scripture" to "1611: Sacred text | Modern: Bible",
        "seed" to "1611: Descendants | Modern: Plant part",
        "sight" to "1611: Vision | Modern: Visual perception",
        "sin" to "1611: Transgression | Modern: Immorality",
        "tongue" to "1611: Language | Modern: Oral organ",
        "word" to "1611: Message | Modern: Vocabulary",
        "world" to "1611: Age | Modern: Planet"
    )

    fun highlightFalseFriends(text: String, highlightColor: Color = Color(0xFF673AB7)): AnnotatedString {
        return buildAnnotatedString {
            val components = text.split(Regex("(?<=\\b)|(?=\\b)"))
            components.forEach { component ->
                val cleanWord = component.lowercase().trim().filter { it.isLetter() }
                if (falseFriends.containsKey(cleanWord)) {
                    pushStringAnnotation(tag = "GLOSSARY", annotation = falseFriends[cleanWord]!!)
                    withStyle(style = SpanStyle(
                        color = highlightColor,
                        fontWeight = FontWeight.Bold,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                    )) {
                        append(component)
                    }
                    pop()
                } else {
                    append(component)
                }
            }
        }
    }

    fun getFalseFriends(text: String): Map<String, String> {
        val found = mutableMapOf<String, String>()
        val words = text.lowercase(java.util.Locale.ROOT).split(Regex("\\W+"))
        words.forEach { word ->
            val cleanWord = word.trim().filter { it.isLetter() }
            if (falseFriends.containsKey(cleanWord)) {
                found[cleanWord] = falseFriends[cleanWord]!!
            }
        }
        return found
    }
}
