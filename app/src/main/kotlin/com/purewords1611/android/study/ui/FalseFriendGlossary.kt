package com.purewords1611.android.study.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import java.util.Locale

object FalseFriendGlossary {
    // 1611 KJV false friends with accurate historical meanings vs modern usage
    // Note: Contronyms (words that meant two opposite things, like 'let' meaning 'allow' AND 'hinder')
    // and highly context-dependent words (like 'save' or 'suffer') have been removed to prevent
    // wildly incorrect highlighting in common contexts.
    private val falseFriends = mapOf(
        "allow" to "1611: Approve or commend | Modern: Permit",
        "allows" to "1611: Approves or commends | Modern: Permits",
        "allowed" to "1611: Approved or commended | Modern: Permitted",
        "alloweth" to "1611: Approves or commends | Modern: Permits",
        "artillery" to "1611: Bows, arrows, or missile weapons | Modern: Heavy mounted guns or cannons",
        "bottle" to "1611: Leather wineskin or pouch | Modern: Glass or plastic container",
        "bottles" to "1611: Leather wineskins or pouches | Modern: Glass or plastic containers",
        "careful" to "1611: Full of anxiety or worry | Modern: Cautious or taking care",
        "charity" to "1611: Benevolent Christian love | Modern: Almsgiving or organization",
        "closet" to "1611: Private inner chamber | Modern: Storage wardrobe for clothes",
        "conversation" to "1611: Manner of life or conduct | Modern: Verbal chat or dialogue",
        "corn" to "1611: Grain in general (wheat, barley) | Modern: Maize or corn on the cob",
        "cunning" to "1611: Skillful or expert | Modern: Sly or deceitful",
        "curious" to "1611: Skillfully wrought or intricate | Modern: Inquisitive or strange",
        "discover" to "1611: Uncover, lay bare, or reveal | Modern: Find something unknown",
        "discovereth" to "1611: Uncovers, lays bare, or reveals | Modern: Finds something unknown",
        "doctor" to "1611: Teacher or scholar of religious law | Modern: Medical physician",
        "doctors" to "1611: Teachers or scholars of religious law | Modern: Medical physicians",
        "halt" to "1611: Limp or lame | Modern: Stop moving",
        "honest" to "1611: Honorable, respectable, or decent | Modern: Truthful or non-deceitful",
        "instantly" to "1611: Urgently or earnestly | Modern: Immediately or without delay",
        "leasing" to "1611: Lying or falsehood | Modern: Renting property under contract",
        "lust" to "1611: Strong desire or pleasure in general | Modern: Sexual desire",
        "meat" to "1611: Any solid food in general | Modern: Animal flesh",
        "meats" to "1611: Any solid foods in general | Modern: Animal flesh",
        "naughty" to "1611: Worthless, corrupt, or bad | Modern: Disobedient or mischievous",
        "nephew" to "1611: Grandson or descendant | Modern: Son of a sibling",
        "nephews" to "1611: Grandsons or descendants | Modern: Sons of a sibling",
        "offence" to "1611: Stumbling block or cause for falling | Modern: Insult or transgression",
        "offences" to "1611: Stumbling blocks or causes for falling | Modern: Insults or transgressions",
        "passion" to "1611: Suffering or endurance of pain | Modern: Intense emotion or enthusiasm",
        "peculiar" to "1611: One's own special possession | Modern: Strange, odd, or unusual",
        "perfect" to "1611: Complete, mature, or fully developed | Modern: Flawless or without error",
        "picture" to "1611: Carved image, relief, or representation | Modern: Photograph or painting",
        "pictures" to "1611: Carved images, reliefs, or representations | Modern: Photographs or paintings",
        "port" to "1611: Gate or doorway | Modern: Harbor or port city",
        "prevent" to "1611: Go before or precede | Modern: Stop or hinder beforehand",
        "prevented" to "1611: Went before or preceded | Modern: Stopped or hindered beforehand",
        "preventeth" to "1611: Goes before or precedes | Modern: Stops or hinders beforehand",
        "provoke" to "1611: Stir up, motivate, or inspire | Modern: Annoy or irritate",
        "publican" to "1611: Tax collector for Rome | Modern: Owner or manager of a pub",
        "publicans" to "1611: Tax collectors for Rome | Modern: Owners or managers of pubs",
        "purchase" to "1611: Acquire, obtain, or gain | Modern: Buy with money",
        "quick" to "1611: Living or alive | Modern: Fast or swift",
        "quicken" to "1611: Make alive or impart life | Modern: Speed up or accelerate",
        "quickened" to "1611: Made alive or imparted life | Modern: Sped up or accelerated",
        "reprove" to "1611: Convict, expose, or refute | Modern: Scold or reprimand",
        "scrip" to "1611: Wallet, satchel, or small bag | Modern: Paper certificate or token",
        "strange" to "1611: Foreign or alien | Modern: Odd or unusual",
        "vex" to "1611: Afflict, harass, or oppress | Modern: Annoy or bother",
        "vexed" to "1611: Afflicted, harassed, or oppressed | Modern: Annoyed or bothered",
        "virtue" to "1611: Power, strength, or efficacy | Modern: Moral goodness or chastity",
        "wist" to "1611: Knew or understood | Modern: Obsolete (past tense of wit)",
        "wot" to "1611: Know or aware of | Modern: Obsolete (present tense of wit)"
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
                        textDecoration = TextDecoration.Underline
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
        val words = text.lowercase(Locale.ROOT).split(Regex("\\W+"))
        words.forEach { word ->
            val cleanWord = word.trim().filter { it.isLetter() }
            if (falseFriends.containsKey(cleanWord)) {
                found[cleanWord] = falseFriends[cleanWord]!!
            }
        }
        return found
    }
}
