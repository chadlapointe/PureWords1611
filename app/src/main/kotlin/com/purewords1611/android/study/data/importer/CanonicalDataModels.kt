package com.purewords1611.android.study.data.importer

import com.purewords1611.android.study.data.TestamentSection

data class CanonicalSourceRecord(
    val sourceId: String,
    val label: String
)

data class CanonicalVerseRecord(
    val id: Long,
    val book: String,
    val bookOriginal: String?,
    val chapter: Int,
    val verse: Int,
    val section: TestamentSection,
    val canonicalOrder: Int,
    val originalText: String,
    val modernizedText: String,
    val standardText: String? = null,
    val comparativeText: String?,
    val hasItalicWords: Boolean,
    val sourceId: String,
    val sourceLocator: String,
    val checksumSha256: String
)

data class CanonicalMarginalNoteRecord(
    val id: Long,
    val verseId: Long,
    val noteType: String,
    val note: String,
    val anchorToken: String?,
    val sourceId: String,
    val sourceLocator: String,
    val checksumSha256: String
)

data class CanonicalGlossaryRecord(
    val id: String,
    val headword: String,
    val definitionShort: String,
    val sourceId: String,
    val checksumSha256: String
)

data class CanonicalExplanationRecord(
    val id: String,
    val verseId: Long,
    val level: String,
    val contentMarkdown: String,
    val sourceId: String,
    val checksumSha256: String
)

data class CanonicalSeekerTrackRecord(
    val trackId: String,
    val title: String,
    val description: String
)

data class CanonicalSeekerStepRecord(
    val stepId: String,
    val trackId: String,
    val sequence: Int,
    val title: String,
    val bodyMarkdown: String
)

data class CanonicalFrontMatterRecord(
    val docId: String,
    val title: String,
    val textOriginal: String,
    val textModernized: String,
    val sourceId: String,
    val checksumSha256: String
)
