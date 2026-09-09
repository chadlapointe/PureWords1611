package com.purewords1611.android.study.data.importer

import com.purewords1611.android.study.data.TestamentSection
import org.junit.Assert.assertThrows
import org.junit.Test

class CanonicalDataValidatorTest {
    private val validator = CanonicalDataValidator()

    @Test
    fun `accepts valid canonical payload`() {
        validator.validate(
            sources = listOf(CanonicalSourceRecord(sourceId = "s1", label = "Source 1")),
            verses = listOf(
                CanonicalVerseRecord(
                    id = 1,
                    book = "Genesis",
                    chapter = 1,
                    verse = 1,
                    section = TestamentSection.OLD_TESTAMENT,
                    canonicalOrder = 1,
                    originalText = "In the beginning...",
                    modernizedText = "In the beginning...",
                    hasItalicWords = false,
                    sourceId = "s1",
                    sourceLocator = "Gen 1:1",
                    checksumSha256 = "abc"
                )
            ),
            notes = listOf(
                CanonicalMarginalNoteRecord(
                    id = 1,
                    verseId = 1,
                    noteType = "TRANSLATION_NOTE",
                    note = "note",
                    anchorToken = null,
                    sourceId = "s1",
                    sourceLocator = "margin",
                    checksumSha256 = "def"
                )
            ),
            glossary = listOf(
                CanonicalGlossaryRecord(
                    id = "g1",
                    headword = "heauen",
                    definitionShort = "heaven",
                    sourceId = "s1",
                    checksumSha256 = "ghi"
                )
            ),
            explanations = listOf(
                CanonicalExplanationRecord(
                    id = "e1",
                    verseId = 1,
                    level = "HISTORICAL_LINGUISTIC",
                    contentMarkdown = "Historical note",
                    sourceId = "s1",
                    checksumSha256 = "jkl"
                )
            ),
            seekerTracks = listOf(
                CanonicalSeekerTrackRecord(
                    trackId = "FULL_EVIDENCE",
                    title = "Full Evidence",
                    description = "Structured clear-evidence path"
                )
            ),
            seekerSteps = listOf(
                CanonicalSeekerStepRecord(
                    stepId = "full-1",
                    trackId = "FULL_EVIDENCE",
                    sequence = 1,
                    title = "Start",
                    bodyMarkdown = "Read the pure words."
                )
            ),
            frontMatter = listOf(
                CanonicalFrontMatterRecord(
                    docId = "TRANSLATORS_TO_THE_READER",
                    title = "Translators to the Reader",
                    textOriginal = "Original text",
                    textModernized = "Modernized text",
                    sourceId = "s1",
                    checksumSha256 = "mno"
                )
            )
        )
    }

    @Test
    fun `asserts every checksum in every bundled JSON asset is exactly 64 lowercase hex characters`() {
        val studyDir = java.io.File("src/main/assets/study").let { if (it.exists()) it else java.io.File("app/src/main/assets/study") }
        val jsonFiles = studyDir.walkTopDown().filter { it.extension == "json" }.toList()
        val hexRegex = Regex("^[0-9a-f]{64}$")
        val checksumRegex = Regex(""""checksum_sha256"\s*:\s*"([^"]+)"""")
        var checksumCount = 0

        for (file in jsonFiles) {
            val jsonText = file.readText(Charsets.UTF_8).removePrefix("\uFEFF")
            for (match in checksumRegex.findAll(jsonText)) {
                val checksum = match.groupValues[1]
                org.junit.Assert.assertTrue(
                    "Invalid checksum in ${file.name}: '$checksum'",
                    checksum.matches(hexRegex)
                )
                checksumCount++
            }
        }
        org.junit.Assert.assertTrue("Expected at least 1 checksum_sha256 field checked", checksumCount > 0)
    }

    @Test
    fun `rejects note with missing verse`() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            validator.validate(
                sources = listOf(CanonicalSourceRecord(sourceId = "s1", label = "Source 1")),
                verses = listOf(
                    CanonicalVerseRecord(
                        id = 1,
                        book = "Genesis",
                        chapter = 1,
                        verse = 1,
                        section = TestamentSection.OLD_TESTAMENT,
                        canonicalOrder = 1,
                        originalText = "In the beginning...",
                        modernizedText = "In the beginning...",
                        hasItalicWords = false,
                        sourceId = "s1",
                        sourceLocator = "Gen 1:1",
                        checksumSha256 = "abc"
                    )
                ),
                notes = listOf(
                    CanonicalMarginalNoteRecord(
                        id = 1,
                        verseId = 999,
                        noteType = "TRANSLATION_NOTE",
                        note = "note",
                        anchorToken = null,
                        sourceId = "s1",
                        sourceLocator = "margin",
                        checksumSha256 = "def"
                    )
                ),
                glossary = emptyList(),
                explanations = emptyList(),
                seekerTracks = emptyList(),
                seekerSteps = emptyList(),
                frontMatter = emptyList()
            )
        }

        check(ex.message?.contains("missing verse_id") == true)
    }

    @Test
    fun `rejects invalid explanation level`() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            validator.validate(
                sources = listOf(CanonicalSourceRecord(sourceId = "s1", label = "Source 1")),
                verses = listOf(
                    CanonicalVerseRecord(
                        id = 1,
                        book = "Genesis",
                        chapter = 1,
                        verse = 1,
                        section = TestamentSection.OLD_TESTAMENT,
                        canonicalOrder = 1,
                        originalText = "In the beginning...",
                        modernizedText = "In the beginning...",
                        hasItalicWords = false,
                        sourceId = "s1",
                        sourceLocator = "Gen 1:1",
                        checksumSha256 = "abc"
                    )
                ),
                notes = emptyList(),
                glossary = emptyList(),
                explanations = listOf(
                    CanonicalExplanationRecord(
                        id = "e1",
                        verseId = 1,
                        level = "INVALID_LEVEL",
                        contentMarkdown = "x",
                        sourceId = "s1",
                        checksumSha256 = "x"
                    )
                ),
                seekerTracks = emptyList(),
                seekerSteps = emptyList(),
                frontMatter = emptyList()
            )
        }

        check(ex.message?.contains("invalid level") == true)
    }

    @Test
    fun `rejects non contiguous seeker step sequence`() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            validator.validate(
                sources = listOf(CanonicalSourceRecord(sourceId = "s1", label = "Source 1")),
                verses = listOf(
                    CanonicalVerseRecord(
                        id = 1,
                        book = "Genesis",
                        chapter = 1,
                        verse = 1,
                        section = TestamentSection.OLD_TESTAMENT,
                        canonicalOrder = 1,
                        originalText = "In the beginning...",
                        modernizedText = "In the beginning...",
                        hasItalicWords = false,
                        sourceId = "s1",
                        sourceLocator = "Gen 1:1",
                        checksumSha256 = "abc"
                    )
                ),
                notes = emptyList(),
                glossary = emptyList(),
                explanations = emptyList(),
                seekerTracks = listOf(
                    CanonicalSeekerTrackRecord(
                        trackId = "FULL_EVIDENCE",
                        title = "Full Evidence",
                        description = "x"
                    )
                ),
                seekerSteps = listOf(
                    CanonicalSeekerStepRecord(
                        stepId = "s2",
                        trackId = "FULL_EVIDENCE",
                        sequence = 2,
                        title = "Second",
                        bodyMarkdown = "x"
                    )
                ),
                frontMatter = emptyList()
            )
        }

        check(ex.message?.contains("non-contiguous sequence") == true)
    }
}
