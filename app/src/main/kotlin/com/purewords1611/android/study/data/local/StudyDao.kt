package com.purewords1611.android.study.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.paging.PagingSource
import com.purewords1611.android.study.data.TestamentSection
import kotlinx.coroutines.flow.Flow

data class ChapterIndexRow(
    val book: String,
    val bookOriginal: String?,
    val chapter: Int,
    val section: TestamentSection,
    val firstVerseId: Long,
    val verseCount: Int
)

@Dao
interface VerseDao {
    @Query("SELECT COUNT(*) FROM verses")
    suspend fun count(): Int

    @Query("SELECT * FROM verses ORDER BY id ASC")
    fun observeAllVerses(): PagingSource<Int, VerseEntity>

    @Query("SELECT * FROM verses ORDER BY id ASC")
    fun observeAllVersesList(): Flow<List<VerseEntity>>

    @Query(
        """
        SELECT * FROM verses
        WHERE originalText LIKE '%' || :query || '%'
           OR modernizedText LIKE '%' || :query || '%'
           OR book LIKE '%' || :query || '%'
        ORDER BY id ASC
        """,
    )
    fun observeVersesByQuery(query: String): Flow<List<VerseEntity>>

    @Query(
        """
        SELECT * FROM verses
        JOIN verses_fts ON verses.id = verses_fts.docid
        WHERE verses_fts MATCH :query
          AND (:section IS NULL OR section = :section)
        ORDER BY (CASE WHEN verses_fts.book MATCH :query THEN 0 ELSE 1 END) ASC, id ASC
        """
    )
    fun searchVerses(query: String, section: TestamentSection? = null): PagingSource<Int, VerseEntity>

    @Query(
        """
        SELECT * FROM verses
        WHERE book = :book AND chapter = :chapter
        ORDER BY id ASC
        """
    )
    fun observeVersesByChapter(book: String, chapter: Int): Flow<List<VerseEntity>>

    @Query("SELECT * FROM verses WHERE book = :book AND chapter = :chapter ORDER BY id ASC")
    suspend fun getVersesByChapter(book: String, chapter: Int): List<VerseEntity>

    @Query("SELECT * FROM verses WHERE id = :id")
    suspend fun getVerseById(id: Long): VerseEntity?

    @Query("SELECT COUNT(*) FROM verses WHERE id < (SELECT MIN(id) FROM verses WHERE book = :book AND chapter = :chapter)")
    suspend fun getChapterOffset(book: String, chapter: Int): Int

    @Query("SELECT COUNT(*) FROM verses WHERE id < :verseId")
    suspend fun getVerseOffset(verseId: Long): Int

    @Query(
        """
        SELECT book, MAX(bookOriginal) AS bookOriginal, chapter, MAX(section) AS section, MIN(id) AS firstVerseId, COUNT(*) AS verseCount
        FROM verses
        GROUP BY book, chapter
        ORDER BY firstVerseId ASC
        """
    )
    fun observeChapterIndex(): Flow<List<ChapterIndexRow>>

    @Query(
        """
        SELECT * FROM verses
        JOIN verses_fts ON verses.id = verses_fts.docid
        WHERE verses_fts.strongsText MATCH :strongsId
        ORDER BY id ASC
        """
    )
    fun searchVersesByStrongsId(strongsId: String): PagingSource<Int, VerseEntity>

    @Query(
        """
        SELECT COUNT(*) FROM verses 
        JOIN verses_fts ON verses.id = verses_fts.docid 
        WHERE verses_fts.strongsText MATCH :strongsId
        """
    )
    suspend fun countStrongsOccurrences(strongsId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(verses: List<VerseEntity>)

    @Query("DELETE FROM verses")
    suspend fun deleteAll()
}

@Dao
interface MarginalNoteDao {
    @Query("SELECT COUNT(*) FROM marginal_notes")
    suspend fun count(): Int

    @Query("SELECT * FROM marginal_notes WHERE verseId = :verseId ORDER BY id ASC")
    fun observeByVerse(verseId: Long): Flow<List<MarginalNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(notes: List<MarginalNoteEntity>)

    @Query("DELETE FROM marginal_notes")
    suspend fun deleteAll()
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE verseId = :verseId")
    suspend fun getByVerse(verseId: Long): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE verseId = :verseId")
    suspend fun deleteByVerse(verseId: Long)
}

@Dao
interface PersonalNoteDao {
    @Query("SELECT * FROM personal_notes ORDER BY updatedAtEpochMillis DESC")
    fun observeAll(): Flow<List<PersonalNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: PersonalNoteEntity)
}

@Dao
interface HighlightDao {
    @Query("SELECT * FROM highlights ORDER BY createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<HighlightEntity>>

    @Query("SELECT * FROM highlights WHERE verseId = :verseId")
    suspend fun getByVerse(verseId: Long): HighlightEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(highlight: HighlightEntity)

    @Query("DELETE FROM highlights WHERE verseId = :verseId")
    suspend fun deleteByVerse(verseId: Long)
}

@Dao
interface ExplanationDao {
    @Query("SELECT COUNT(*) FROM explanations")
    suspend fun count(): Int

    @Query(
        """
        SELECT * FROM explanations
        WHERE verseId = :verseId AND level = :level
        ORDER BY id ASC
        """
    )
    fun observeByVerseAndLevel(verseId: Long, level: String): Flow<List<ExplanationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(explanations: List<ExplanationEntity>)

    @Query("DELETE FROM explanations")
    suspend fun deleteAll()
}

@Dao
interface ReadingPreferenceDao {
    @Query("SELECT * FROM reading_preferences WHERE id = 1")
    fun observePreferences(): Flow<ReadingPreferenceEntity?>

    @Query("SELECT * FROM reading_preferences WHERE id = 1")
    suspend fun getPreferences(): ReadingPreferenceEntity?

    @Query("UPDATE reading_preferences SET lastBook = :book, lastChapter = :chapter, lastVerseId = :verseId WHERE id = 1")
    suspend fun updateLastRead(book: String, chapter: Int, verseId: Long?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(preferences: ReadingPreferenceEntity)
}

@Dao
interface ChapterCompletionDao {
    @Query("SELECT * FROM chapter_completions ORDER BY completedAtEpochMillis DESC")
    fun observeAll(): Flow<List<ChapterCompletionEntity>>

    @Query("SELECT * FROM chapter_completions WHERE book = :book AND chapter = :chapter")
    suspend fun getCompletion(book: String, chapter: Int): ChapterCompletionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(completion: ChapterCompletionEntity)

    @Query("UPDATE chapter_completions SET lastQuestionAskedAtEpochMillis = :timestamp WHERE id = :id")
    suspend fun updateLastQuestionTime(id: Long, timestamp: Long)

    @Query("SELECT * FROM chapter_completions WHERE lastQuestionAskedAtEpochMillis IS NULL OR lastQuestionAskedAtEpochMillis < :cutoff ORDER BY completedAtEpochMillis DESC LIMIT 1")
    suspend fun getOldestPendingQuestion(cutoff: Long): ChapterCompletionEntity?
}

@Dao
interface FrontMatterDao {
    @Query("SELECT COUNT(*) FROM front_matter")
    suspend fun count(): Int

    @Query("SELECT * FROM front_matter WHERE docId = :docId")
    fun observeByDocId(docId: String): Flow<FrontMatterEntity?>

    @Query("SELECT * FROM front_matter ORDER BY docId ASC")
    fun observeAll(): Flow<List<FrontMatterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<FrontMatterEntity>)
}

@Dao
interface ChapterSummaryDao {
    @Query("SELECT * FROM chapter_summaries WHERE book = :book AND chapter = :chapter")
    fun observeByChapter(book: String, chapter: Int): Flow<ChapterSummaryEntity?>

    @Query("SELECT * FROM chapter_summaries")
    fun observeAll(): Flow<List<ChapterSummaryEntity>>

    @Query("SELECT * FROM chapter_summaries WHERE book = :book AND chapter = :chapter")
    suspend fun getByChapter(book: String, chapter: Int): ChapterSummaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(summary: ChapterSummaryEntity)
}

@Dao
interface VerseTitleDao {
    @Query("SELECT * FROM verse_titles WHERE book = :book AND chapter = :chapter")
    fun observeByChapter(book: String, chapter: Int): Flow<List<VerseTitleEntity>>

    @Query("SELECT * FROM verse_titles")
    fun observeAll(): Flow<List<VerseTitleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(titles: List<VerseTitleEntity>)

    @Query("DELETE FROM verse_titles")
    suspend fun deleteAll()
}

@Dao
interface LexiconDao {
    @Query("SELECT * FROM lexicon WHERE strongsId = :strongsId")
    suspend fun getById(strongsId: String): LexiconEntity?

    @Query("SELECT * FROM lexicon WHERE strongsId = :strongsId")
    fun observeById(strongsId: String): Flow<LexiconEntity?>

    @Query("SELECT * FROM lexicon WHERE strongsId IN (:strongsIds)")
    suspend fun getByIds(strongsIds: List<String>): List<LexiconEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<LexiconEntity>)

    @Query("DELETE FROM lexicon")
    suspend fun deleteAll()
}

@Dao
interface StudyStatsDao {
    @Query("SELECT * FROM study_stats ORDER BY date DESC")
    fun observeAll(): Flow<List<StudyStatsEntity>>

    @Query("SELECT * FROM study_stats WHERE date = :date")
    suspend fun getByDate(date: String): StudyStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stats: StudyStatsEntity)

    @Query("SELECT COUNT(DISTINCT date) FROM study_stats")
    suspend fun getTotalActiveDays(): Int

    @Query("SELECT SUM(chaptersCompleted) FROM study_stats")
    suspend fun getTotalChaptersCompleted(): Int
}

@Dao
interface MarginaliaDao {
    @Query("SELECT * FROM marginalia ORDER BY updatedAtEpochMillis DESC")
    fun observeAll(): Flow<List<MarginaliaEntity>>

    @Query("SELECT * FROM marginalia WHERE book = :book AND chapter = :chapter")
    fun observeByChapter(book: String, chapter: Int): Flow<MarginaliaEntity?>

    @Query("SELECT * FROM marginalia WHERE book = :book AND chapter = :chapter")
    suspend fun getByChapter(book: String, chapter: Int): MarginaliaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(marginalia: MarginaliaEntity)

    @Query("DELETE FROM marginalia WHERE book = :book AND chapter = :chapter")
    suspend fun deleteByChapter(book: String, chapter: Int)
}
