package com.purewords1611.android.study.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey
import com.purewords1611.android.study.data.TestamentSection

@Entity(
    tableName = "verses",
    indices = [Index(value = ["book", "chapter", "verse"], unique = true)],
)
data class VerseEntity(
    @PrimaryKey val id: Long,
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
    val strongsText: String? = null,
    val hasItalicWords: Boolean,
    val sourceId: String,
    val sourceLocator: String,
    val checksumSha256: String
)

@Entity(
    tableName = "marginal_notes",
    foreignKeys = [
        ForeignKey(
            entity = VerseEntity::class,
            parentColumns = ["id"],
            childColumns = ["verseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("verseId")]
)
data class MarginalNoteEntity(
    @PrimaryKey val id: Long,
    val verseId: Long,
    val noteType: String,
    val note: String,
    val anchorToken: String?,
    val sourceId: String,
    val sourceLocator: String,
    val checksumSha256: String
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val verseId: Long,
    val createdAtEpochMillis: Long
)

@Entity(tableName = "highlights")
data class HighlightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val verseId: Long,
    val colorName: String,
    val createdAtEpochMillis: Long
)

@Entity(tableName = "personal_notes")
data class PersonalNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val verseId: Long,
    val note: String,
    val updatedAtEpochMillis: Long,
    val category: String? = null, // e.g. "Historical", "Theological", "Personal"
)

@Entity(
    tableName = "explanations",
    foreignKeys = [
        ForeignKey(
            entity = VerseEntity::class,
            parentColumns = ["id"],
            childColumns = ["verseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("verseId"), Index("level")]
)
data class ExplanationEntity(
    @PrimaryKey val id: String,
    val verseId: Long,
    val level: String,
    val contentMarkdown: String,
    val sourceId: String,
    val checksumSha256: String
)

@Entity(tableName = "reading_preferences")
data class ReadingPreferenceEntity(
    @PrimaryKey val id: Int = 1,
    val explanationLevel: String,
    val contentVersion: Int = 0,
    val lastBook: String? = null,
    val lastChapter: Int? = null,
    val lastVerseId: Long? = null,
    val speechRate: Float = 1.0f,
    val selectedVoice: String? = null,
)

@Entity(
    tableName = "chapter_completions",
    indices = [Index(value = ["book", "chapter"], unique = true)]
)
data class ChapterCompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val book: String,
    val chapter: Int,
    val completedAtEpochMillis: Long,
    val lastQuestionAskedAtEpochMillis: Long? = null,
    val masteryPoints: Int = 0,
)

@Fts4(contentEntity = VerseEntity::class)
@Entity(tableName = "verses_fts")
data class VerseFtsEntity(
    val book: String,
    val originalText: String,
    val modernizedText: String,
    val standardText: String?,
    val comparativeText: String?,
    val strongsText: String?
)

@Entity(tableName = "front_matter")
data class FrontMatterEntity(
    @PrimaryKey val docId: String,
    val title: String,
    val textOriginal: String,
    val textModernizedSpelling: String,
    val sourceId: String,
    val checksumSha256: String
)

@Entity(
    tableName = "chapter_summaries",
    indices = [Index(value = ["book", "chapter"], unique = true)]
)
data class ChapterSummaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val book: String,
    val chapter: Int,
    val summary1611: String?,
    val titleEsv: String?,
    val titleStandard: String? = null,
    val sectionTitle: String? = null
)

@Entity(
    tableName = "verse_titles",
    indices = [Index(value = ["book", "chapter", "verse", "translation"], unique = true)]
)
data class VerseTitleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val book: String,
    val chapter: Int,
    val verse: Int,
    val title: String,
    val translation: String // "ESV", "KJV_STANDARD", etc.
)

@Entity(tableName = "lexicon")
data class LexiconEntity(
    @PrimaryKey val strongsId: String, // e.g., "G1234", "H5678"
    val word: String,
    val transliteration: String?,
    val pronunciation: String?,
    val definition: String,
    val info: String?
)
