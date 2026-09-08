package com.purewords1611.android.study.data

object BibleCitationParser {
    private val bookMap = mapOf(
        "gen" to "Genesis", "ex" to "Exodus", "exo" to "Exodus", "lev" to "Leviticus", 
        "num" to "Numbers", "deut" to "Deuteronomy", "josh" to "Joshua", "judg" to "Judges",
        "ruth" to "Ruth", "1 sam" to "1 Samuel", "2 sam" to "2 Samuel", "1 kings" to "1 Kings",
        "2 kings" to "2 Kings", "1 chron" to "1 Chronicles", "2 chron" to "2 Chronicles",
        "ezra" to "Ezra", "neh" to "Nehemiah", "est" to "Esther", "job" to "Job",
        "ps" to "Psalms", "psa" to "Psalms", "prov" to "Proverbs", "eccl" to "Ecclesiastes",
        "song" to "Song of Solomon", "isa" to "Isaiah", "jer" to "Jeremiah", "lam" to "Lamentations",
        "ezek" to "Ezekiel", "dan" to "Daniel", "hos" to "Hosea", "joel" to "Joel",
        "amos" to "Amos", "obad" to "Obadiah", "jonah" to "Jonah", "mic" to "Micah",
        "nah" to "Nahum", "hab" to "Habakkuk", "zeph" to "Zephaniah", "hag" to "Haggai",
        "zech" to "Zechariah", "mal" to "Malachi", "matt" to "Matthew", "mark" to "Mark",
        "luke" to "Luke", "john" to "John", "acts" to "Acts", "rom" to "Romans",
        "1 cor" to "1 Corinthians", "2 cor" to "2 Corinthians", "gal" to "Galatians",
        "eph" to "Ephesians", "phil" to "Philippians", "col" to "Colossians",
        "1 thess" to "1 Thessalonians", "2 thess" to "2 Thessalonians", "1 tim" to "1 Timothy",
        "2 tim" to "2 Timothy", "titus" to "Titus", "phile" to "Philemon", "heb" to "Hebrews",
        "james" to "James", "1 pet" to "1 Peter", "2 pet" to "2 Peter", "1 john" to "1 John",
        "2 john" to "2 John", "3 john" to "3 John", "jude" to "Jude", "rev" to "Revelation"
    )

    data class Citation(val book: String, val chapter: Int, val verse: Int)

    fun parse(query: String): List<Citation> {
        val citations = mutableListOf<Citation>()
        // Improved pattern: (Number)? Book (Chapter)(:Verse)?
        // Handles: "1 Cor 13:4", "Genesis 1:1", "John 3:16", "Romans 3:23; 6:23"
        val regex = Regex("((?:\\d\\s+)?[a-zA-Z]+)\\s+(\\d+)(?::(\\d+))?")
        
        val segments = query.split(Regex("[,;]"))
        var lastBook: String? = null
        
        segments.forEach { segment ->
            val s = segment.trim()
            val match = regex.find(s)
            if (match != null) {
                val bookPart = match.groupValues[1].lowercase().trim()
                val chapter = match.groupValues[2].toIntOrNull() ?: return@forEach
                val verse = match.groupValues[3].toIntOrNull() ?: 1
                
                val fullBookName = bookMap[bookPart] ?: bookMap.values.find { it.lowercase().startsWith(bookPart) }
                if (fullBookName != null) {
                    citations.add(Citation(fullBookName, chapter, verse))
                    lastBook = fullBookName
                }
            } else {
                // Try to parse just Chapter:Verse if we have a lastBook
                val shortRegex = Regex("(\\d+)(?::(\\d+))?")
                val shortMatch = shortRegex.find(s)
                if (shortMatch != null && lastBook != null) {
                    val chapter = shortMatch.groupValues[1].toInt()
                    val verse = if (shortMatch.groupValues[2].isNotEmpty()) shortMatch.groupValues[2].toInt() else 1
                    citations.add(Citation(lastBook!!, chapter, verse))
                }
            }
        }
        return citations
    }
}
