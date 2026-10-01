package com.purewords1611.android.study.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FalseFriendGlossaryTest {

    @Test
    fun testNephewsCorrectDefinition() {
        val verseText = "If any widow have children or nephews, let them learn first to shew piety at home."
        val result = FalseFriendGlossary.getFalseFriends(verseText)

        assertTrue(result.containsKey("nephews"))
        assertEquals("1611: Grandsons or descendants | Modern: Sons of a sibling", result["nephews"])
        // Verify 'let' is ignored
        assertFalse(result.containsKey("let"))
    }

    @Test
    fun testPortCorrectDefinition() {
        val verseText = "And I went out by night by the gate of the valley, even before the dragon well, and to the dung port."
        val result = FalseFriendGlossary.getFalseFriends(verseText)

        assertTrue(result.containsKey("port"))
        assertEquals("1611: Gate or doorway | Modern: Harbor or port city", result["port"])
    }

    @Test
    fun testRemovedContextDependentWords() {
        val verseText = "No man knoweth the Son, but the Father; neither knoweth any man the Father, save the Son. Suffer it to be so."
        val result = FalseFriendGlossary.getFalseFriends(verseText)

        assertFalse(result.containsKey("save"))
        assertFalse(result.containsKey("suffer"))
    }

    @Test
    fun testPreventAndConversation() {
        val verseText = "We which are alive and remain unto the coming of the Lord shall not prevent them which are asleep. Be ye holy in all manner of conversation."
        val result = FalseFriendGlossary.getFalseFriends(verseText)

        assertTrue(result.containsKey("prevent"))
        assertEquals("1611: Go before or precede | Modern: Stop or hinder beforehand", result["prevent"])

        assertTrue(result.containsKey("conversation"))
        assertEquals("1611: Manner of life or conduct | Modern: Verbal chat or dialogue", result["conversation"])
    }

    @Test
    fun testHighlightingProducesAnnotations() {
        val text = "We shall not prevent them which are asleep."
        val annotated = FalseFriendGlossary.highlightFalseFriends(text)

        val annotations = annotated.getStringAnnotations(tag = "GLOSSARY", start = 0, end = text.length)
        assertTrue(annotations.isNotEmpty())
        assertEquals("1611: Go before or precede | Modern: Stop or hinder beforehand", annotations.first().item)
    }
}
