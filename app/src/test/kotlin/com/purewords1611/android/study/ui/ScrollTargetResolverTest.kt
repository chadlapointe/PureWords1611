package com.purewords1611.android.study.ui

import com.purewords1611.android.data.Verse
import com.purewords1611.android.study.data.ReaderItem
import com.purewords1611.android.study.data.VerseText
import com.purewords1611.android.study.data.TestamentSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScrollTargetResolverTest {

    private val mockVerse1 = VerseText(
        id = 100, book = "John", bookOriginal = null, chapter = 3, verse = 16, section = TestamentSection.NEW_TESTAMENT, originalText = "For God so loved...", modernizedText = "", standardText = null, comparativeText = null, strongsText = null, hasItalicWords = false
    )
    
    private val mockVerse2 = VerseText(
        id = 101, book = "John", bookOriginal = null, chapter = 3, verse = 17, section = TestamentSection.NEW_TESTAMENT, originalText = "For God sent not...", modernizedText = "", standardText = null, comparativeText = null, strongsText = null, hasItalicWords = false
    )

    @Test
    fun `finds target after a CompositeHeader separator`() {
        val snapshot = listOf(
            ReaderItem.CompositeHeader(TestamentSection.NEW_TESTAMENT, "John", null, 3, false, "Summary", "Title", null, null),
            ReaderItem.VerseLine(mockVerse1),
            ReaderItem.VerseLine(mockVerse2)
        )
        val target = ScrollTarget(verseId = 101, absolutePosition = 2, token = 1)
        val idx = resolveScrollIndex(snapshot, target, false)
        assertEquals(2, idx)
    }

    @Test
    fun `returns null when the target is not loaded`() {
        val snapshot = listOf(
            ReaderItem.VerseLine(mockVerse1)
        )
        val target = ScrollTarget(verseId = 999, absolutePosition = 50, token = 1)
        val idx = resolveScrollIndex(snapshot, target, false)
        assertNull(idx)
    }

    @Test
    fun `returns null for an empty snapshot`() {
        val snapshot = emptyList<ReaderItem>()
        val target = ScrollTarget(verseId = 100, absolutePosition = 0, token = 1)
        val idx = resolveScrollIndex(snapshot, target, false)
        assertNull(idx)
    }

    @Test
    fun `handles a leading VerseTitle separator by locating the verse`() {
        val snapshot = listOf(
            ReaderItem.VerseTitle("For God so loved...", com.purewords1611.android.study.data.TranslationMode.KJV_1611),
            ReaderItem.VerseLine(mockVerse1)
        )
        val target = ScrollTarget(verseId = 100, absolutePosition = 0, token = 1)
        val idx = resolveScrollIndex(snapshot, target, false)
        assertEquals(1, idx)
    }
}
