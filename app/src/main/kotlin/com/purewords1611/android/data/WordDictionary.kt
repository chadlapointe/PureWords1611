package com.purewords1611.android.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository interface for managing KJV word dictionary.
 * Provides words for grid generation and validation.
 */
interface WordDictionary {
    suspend fun loadWords(): Set<String>
    suspend fun isValidWord(word: String): Boolean
    suspend fun getRandomWord(): String?
    suspend fun getWordsByLength(length: Int): List<String>
}

open class KjvWordDictionary(private val context: Context) : WordDictionary {
    
    companion object {
        private const val TAG = "WordDictionary"
        private const val MIN_WORD_LENGTH = 3
    }
    
    private var words: Set<String> = emptySet()
    
    override suspend fun loadWords(): Set<String> = withContext(Dispatchers.IO) {
        if (words.isEmpty()) {
            try {
                val verseRepository = VerseRepository(context)
                val verses = verseRepository.loadVerses()
                
                val allWords = mutableSetOf<String>()
                verses.forEach { verse ->
                    val verseWords = verse.text
                        .lowercase()
                        .replace(Regex("[^a-z ]"), "")
                        .split("\\s+".toRegex())
                        .filter { it.length >= MIN_WORD_LENGTH }
                    
                    allWords.addAll(verseWords)
                }
                
                words = allWords
                android.util.Log.d(TAG, "Loaded ${words.size} unique words")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to load words", e)
                words = getDefaultWords()
            }
        }
        words
    }
    
    override suspend fun isValidWord(word: String): Boolean {
        val dictionary = loadWords()
        return dictionary.contains(word.lowercase())
    }
    
    override suspend fun getRandomWord(): String? {
        val dictionary = loadWords()
        return dictionary.randomOrNull()
    }
    
    override suspend fun getWordsByLength(length: Int): List<String> {
        val dictionary = loadWords()
        return dictionary.filter { it.length == length }
    }
    
    private fun getDefaultWords(): Set<String> {
        return setOf(
            "god", "lord", "jesus", "christ", "heaven", "earth", "spirit",
            "love", "faith", "hope", "life", "peace", "word", "light",
            "truth", "way", "shepherd", "beginning", "grace", "mercy",
            "kingdom", "righteousness", "eternal", "salvation", "blessed",
            "holy", "strength", "power", "glory", "world", "soul", "heart"
        )
    }
}
