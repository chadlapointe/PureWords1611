package com.purewords1611.android.study.data

enum class OrthographyMode {
    ORIGINAL_1611,
    MODERNIZED
}

enum class TranslationMode {
    KJV_1611,
    KJV_STANDARD,
    ESV
}

enum class ExplanationDepth {
    MINIMAL,
    HISTORICAL_LINGUISTIC,
    HISTORICAL_LIGHT_DOCTRINAL
}

enum class TestamentSection {
    OLD_TESTAMENT,
    APOCRYPHA,
    NEW_TESTAMENT
}

data class VerseText(
    val id: Long,
    val book: String,
    val bookOriginal: String?,
    val chapter: Int,
    val verse: Int,
    val section: TestamentSection,
    val originalText: String,
    val modernizedText: String,
    val standardText: String?,
    val comparativeText: String?,
    val strongsText: String? = null,
    val hasItalicWords: Boolean
)

sealed class ReaderItem {
    data class SectionHeader(val section: TestamentSection) : ReaderItem()
    data class BookHeader(val book: String, val bookOriginal: String?) : ReaderItem()
    data class ChapterHeader(val book: String, val chapter: Int) : ReaderItem()
    data class VerseLine(val verse: VerseText) : ReaderItem()
    data class VerseTitle(val title: String, val translation: TranslationMode) : ReaderItem()
    data class CompositeHeader(
        val section: TestamentSection? = null,
        val book: String? = null,
        val bookOriginal: String? = null,
        val chapter: Int? = null,
        val showBookHeader: Boolean = false,
        val summary1611: String? = null,
        val titleEsv: String? = null,
        val titleStandard: String? = null,
        val sectionTitle: String? = null
    ) : ReaderItem()
}

enum class StudyFont(val label: String) {
    SYSTEM("System"),
    SERIF("Serif"),
    SANS_SERIF("Sans Serif"),
    MONOSPACE("Monospace"),
    BLACKLETTER("Blackletter")
}

enum class AmbientSoundscape(val label: String, val resName: String) {
    SCRIPTORIUM("Scriptorium", "scriptorium"),
    RAIN("Gentle Rain", "rain"),
    CHORAL("Sacred Choral", "choral"),
    QUILL("Scratching Quill", "quill")
}

data class MarginalNote(
    val id: Long,
    val verseId: Long,
    val note: String
)

data class ChapterIndexEntry(
    val book: String,
    val bookOriginal: String?,
    val chapter: Int,
    val section: TestamentSection,
    val firstVerseId: Long,
    val position: Int,
    val verseCount: Int = 0
)

data class ExplanationEntry(
    val id: String,
    val verseId: Long,
    val level: ExplanationDepth,
    val contentMarkdown: String
)

data class SeekerTrackEntry(
    val trackId: String,
    val title: String,
    val description: String
)

data class SeekerStepEntry(
    val stepId: String,
    val trackId: String,
    val sequence: Int,
    val title: String,
    val bodyMarkdown: String
)

data class BookmarkItem(
    val id: Long,
    val verseId: Long,
    val createdAtEpochMillis: Long
)

data class PersonalNoteItem(
    val id: Long,
    val verseId: Long,
    val note: String,
    val updatedAtEpochMillis: Long,
    val category: String? = null
)

data class HighlightItem(
    val id: Long,
    val verseId: Long,
    val colorName: String,
    val createdAtEpochMillis: Long
)

data class FrontMatterItem(
    val docId: String,
    val title: String,
    val textOriginal: String,
    val textModernizedSpelling: String,
)

enum class RootDestination(val label: String) {
    HOME("Home"), READ("Read"), PARALLEL("Compare"), SEARCH("Search"), NOTES("Notes"), SEEKER_PATH("Path"), FRONT_MATTER("1611"), GALLERY("Gallery")
}

data class DrawingPath(
    val points: List<Pair<Float, Float>>,
    val color: Int,
    val strokeWidth: Float
)
