package com.purewords1611.android.study.data

import com.purewords1611.android.study.data.importer.CanonicalDataLoader
import com.purewords1611.android.study.data.importer.StudyDataImporter
import com.purewords1611.android.study.data.local.*
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

interface StudyRepository {
    val isReady: StateFlow<Boolean>
    suspend fun initializeDatabase()
    fun observeVerses(query: String, initialKey: Int? = null, section: TestamentSection? = null): Flow<PagingData<VerseText>>
    fun observeFullVerses(): Flow<List<VerseText>>
    fun observeChapterVerses(book: String, chapter: Int): Flow<List<VerseText>>
    fun observeChapterIndex(): Flow<List<ChapterIndexEntry>>
    suspend fun getChapterVerses(book: String, chapter: Int): List<VerseText>
    suspend fun getVerse(id: Long): VerseText?
    suspend fun getChapterOffset(book: String, chapter: Int): Int
    suspend fun getVerseOffset(verseId: Long): Int
    fun observeChapterSummary(book: String, chapter: Int): Flow<ChapterSummaryEntity?>
    fun observeAllChapterSummaries(): Flow<List<ChapterSummaryEntity>>
    fun observeVerseTitles(book: String, chapter: Int): Flow<List<VerseTitleEntity>>
    fun observeAllVerseTitles(): Flow<List<VerseTitleEntity>>
    fun observeMarginalNotes(verseId: Long): Flow<List<MarginalNote>>
    fun observeExplanations(verseId: Long, level: ExplanationDepth): Flow<List<ExplanationEntry>>
    fun observeExplanationDepth(): Flow<ExplanationDepth>
    fun observeSeekerTracks(): Flow<List<SeekerTrackEntry>>
    fun observeSeekerSteps(trackId: String): Flow<List<SeekerStepEntry>>
    fun observeBookmarks(): Flow<List<BookmarkItem>>
    fun observeHighlights(): Flow<List<HighlightItem>>
    fun observePersonalNotes(): Flow<List<PersonalNoteItem>>
    fun observeFrontMatter(docId: String): Flow<FrontMatterItem?>
    fun observeAllFrontMatter(): Flow<List<FrontMatterItem>>
    fun observeCompletedChapters(): Flow<Set<Pair<String, Int>>>
    fun observeChapterCompletions(): Flow<List<ChapterCompletionEntity>>
    fun observeLastReadPosition(): Flow<Pair<String, Int>?>
    fun observeLastReadVerseId(): Flow<Long?>
    fun observeSpeechRate(): Flow<Float>
    fun observeSelectedVoice(): Flow<String?>
    fun observeImportProgress(): Flow<Float>
    suspend fun addBookmark(verseId: Long)
    suspend fun removeBookmark(verseId: Long)
    suspend fun toggleBookmark(verseId: Long)
    suspend fun addHighlight(verseId: Long, colorName: String)
    suspend fun removeHighlight(verseId: Long)
    suspend fun toggleHighlight(verseId: Long, colorName: String)
    fun observeAllMarginalia(): Flow<List<MarginaliaEntity>>
    suspend fun savePersonalNote(verseId: Long, note: String, category: String? = null)
    suspend fun setExplanationDepth(level: ExplanationDepth)
    suspend fun saveLastReadPosition(book: String, chapter: Int, verseId: Long? = null)
    suspend fun saveSpeechRate(rate: Float)
    suspend fun saveSelectedVoice(voiceName: String?)
    suspend fun markChapterCompleted(book: String, chapter: Int)
    suspend fun getOldestPendingQuestion(cutoff: Long): ChapterCompletionEntity?
    suspend fun updateLastQuestionTime(id: Long, timestamp: Long)
    fun observeLexiconEntry(strongsId: String): Flow<LexiconEntity?>
    suspend fun getLexiconEntries(strongsIds: List<String>): List<LexiconEntity>
    suspend fun getStrongsOccurrenceCount(strongsId: String): Int
    fun observeStudyStats(): Flow<List<StudyStatsEntity>>
    suspend fun updateStudyStats(chaptersCompleted: Int = 0, versesRead: Int = 0, minutesSpent: Int = 0, pointsEarned: Int = 0)
    suspend fun getStreakInfo(): StreakInfo
    fun observeMarginalia(book: String, chapter: Int): Flow<List<DrawingPath>>
    suspend fun saveMarginalia(book: String, chapter: Int, paths: List<DrawingPath>)
    suspend fun seedIfEmpty()
}

@Singleton
class OfflineStudyRepository @Inject constructor(
    private val verseDao: VerseDao,
    private val marginalNoteDao: MarginalNoteDao,
    private val explanationDao: ExplanationDao,
    private val readingPreferenceDao: ReadingPreferenceDao,
    private val bookmarkDao: BookmarkDao,
    private val highlightDao: HighlightDao,
    private val personalNoteDao: PersonalNoteDao,
    private val frontMatterDao: FrontMatterDao,
    private val chapterCompletionDao: ChapterCompletionDao,
    private val chapterSummaryDao: ChapterSummaryDao,
    private val verseTitleDao: VerseTitleDao,
    private val lexiconDao: LexiconDao,
    private val studyStatsDao: StudyStatsDao,
    private val marginaliaDao: MarginaliaDao,
    private val canonicalDataLoader: CanonicalDataLoader,
    private val dataImporter: StudyDataImporter,
    private val initializer: ManualDatabaseInitializer,
    databaseStateHolder: DatabaseStateHolder,
) : StudyRepository {

    override val isReady: StateFlow<Boolean> = databaseStateHolder.isReady

    override suspend fun initializeDatabase() {
        // Handled via seedIfEmpty in ViewModel for now or inject initializer directly
    }

    override fun observeVerses(query: String, initialKey: Int?, section: TestamentSection?): Flow<PagingData<VerseText>> {
        return Pager(
            config = PagingConfig(
                pageSize = 50,
                enablePlaceholders = false,
                prefetchDistance = 20,
            ),
            initialKey = initialKey,
        ) {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) {
                verseDao.observeAllVerses()
            } else if (trimmed.matches(Regex("^[HG]\\d+$"))) {
                verseDao.searchVersesByStrongsId(trimmed)
            } else {
                // Check if it's a quoted phrase search
                val ftsQuery = if (trimmed.startsWith("\"") && (trimmed.endsWith("\""))) {
                    // Pass the quoted phrase directly to FTS
                    trimmed
                } else {
                    // Standard word-prefix search
                    trimmed.split("\\s+".toRegex()).asSequence()
                        .filter { it.isNotBlank() }
                        .joinToString(" ") { "$it*" }
                }
                
                if (ftsQuery.isBlank() || (ftsQuery == "\"\"")) {
                    verseDao.observeAllVerses()
                } else {
                    verseDao.searchVerses(ftsQuery, section)
                }
            }
        }.flow.map { pagingData -> 
            pagingData.map { it.toModel() } 
        }.flowOn(Dispatchers.IO)
    }

    override fun observeFullVerses(): Flow<List<VerseText>> {
        return verseDao.observeAllVersesList().map { entities -> 
            entities.map { it.toModel() } 
        }.flowOn(Dispatchers.IO)
    }

    override fun observeChapterVerses(book: String, chapter: Int): Flow<List<VerseText>> {
        return verseDao.observeVersesByChapter(book = book, chapter = chapter)
            .map { entities -> entities.map { it.toModel() } }
            .flowOn(Dispatchers.IO)
    }

    override fun observeChapterIndex(): Flow<List<ChapterIndexEntry>> {
        return verseDao.observeChapterIndex().map { rows ->
            var currentPos = 0
            rows.map { row ->
                val entry = ChapterIndexEntry(
                    book = row.book,
                    bookOriginal = row.bookOriginal,
                    chapter = row.chapter,
                    section = row.section,
                    firstVerseId = row.firstVerseId,
                    position = currentPos,
                    verseCount = row.verseCount,
                )
                currentPos += row.verseCount
                entry
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getChapterVerses(book: String, chapter: Int): List<VerseText> = withContext(Dispatchers.IO) {
        verseDao.getVersesByChapter(book, chapter).map { it.toModel() }
    }

    override suspend fun getVerse(id: Long): VerseText? = withContext(Dispatchers.IO) {
        verseDao.getVerseById(id)?.toModel()
    }

    override suspend fun getChapterOffset(book: String, chapter: Int): Int = withContext(Dispatchers.IO) {
        verseDao.getChapterOffset(book, chapter)
    }

    override suspend fun getVerseOffset(verseId: Long): Int = withContext(Dispatchers.IO) {
        verseDao.getVerseOffset(verseId)
    }

    override fun observeChapterSummary(book: String, chapter: Int): Flow<ChapterSummaryEntity?> {
        return chapterSummaryDao.observeByChapter(book, chapter).flowOn(Dispatchers.IO)
    }

    override fun observeAllChapterSummaries(): Flow<List<ChapterSummaryEntity>> {
        return chapterSummaryDao.observeAll().flowOn(Dispatchers.IO)
    }

    override fun observeVerseTitles(book: String, chapter: Int): Flow<List<VerseTitleEntity>> {
        return verseTitleDao.observeByChapter(book, chapter).flowOn(Dispatchers.IO)
    }

    override fun observeAllVerseTitles(): Flow<List<VerseTitleEntity>> {
        return verseTitleDao.observeAll().flowOn(Dispatchers.IO)
    }

    override fun observeMarginalNotes(verseId: Long): Flow<List<MarginalNote>> {
        return marginalNoteDao.observeByVerse(verseId).map { entities ->
            entities.map { MarginalNote(id = it.id, verseId = it.verseId, note = it.note) }
        }.flowOn(Dispatchers.IO)
    }

    override fun observeExplanations(
        verseId: Long,
        level: ExplanationDepth
    ): Flow<List<ExplanationEntry>> {
        return explanationDao.observeByVerseAndLevel(verseId = verseId, level = level.name)
            .map { entities ->
                entities.map {
                    ExplanationEntry(
                        id = it.id,
                        verseId = it.verseId,
                        level = ExplanationDepth.valueOf(it.level),
                        contentMarkdown = it.contentMarkdown
                    )
                }
            }.flowOn(Dispatchers.IO)
    }

    override fun observeExplanationDepth(): Flow<ExplanationDepth> {
        return readingPreferenceDao.observePreferences().map { preferences ->
            if (preferences == null) {
                ExplanationDepth.HISTORICAL_LINGUISTIC
            } else {
                ExplanationDepth.valueOf(preferences.explanationLevel)
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun observeSeekerTracks(): Flow<List<SeekerTrackEntry>> = flow {
        val tracks = canonicalDataLoader.loadSeekerTracks().map {
            SeekerTrackEntry(
                trackId = it.trackId,
                title = it.title,
                description = it.description
            )
        }
        emit(tracks)
    }.flowOn(Dispatchers.IO)

    override fun observeSeekerSteps(trackId: String): Flow<List<SeekerStepEntry>> = flow {
        val steps = canonicalDataLoader.loadSeekerSteps()
            .asSequence()
            .filter { it.trackId == trackId }
            .sortedBy { it.sequence }
            .map {
                SeekerStepEntry(
                    stepId = it.stepId,
                    trackId = it.trackId,
                    sequence = it.sequence,
                    title = it.title,
                    bodyMarkdown = it.bodyMarkdown,
                )
            }
            .toList()
        emit(steps)
    }.flowOn(Dispatchers.IO)

    override fun observeBookmarks(): Flow<List<BookmarkItem>> {
        return bookmarkDao.observeAll().map { entities ->
            entities.map {
                BookmarkItem(
                    id = it.id,
                    verseId = it.verseId,
                    createdAtEpochMillis = it.createdAtEpochMillis
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun observePersonalNotes(): Flow<List<PersonalNoteItem>> {
        return personalNoteDao.observeAll().map { entities ->
            entities.map {
                PersonalNoteItem(
                    id = it.id,
                    verseId = it.verseId,
                    note = it.note,
                    updatedAtEpochMillis = it.updatedAtEpochMillis,
                    category = it.category
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun observeFrontMatter(docId: String): Flow<FrontMatterItem?> {
        return frontMatterDao.observeByDocId(docId).map { it?.toModel() }.flowOn(Dispatchers.IO)
    }

    override fun observeAllFrontMatter(): Flow<List<FrontMatterItem>> {
        return frontMatterDao.observeAll().map { entities ->
            entities.map { it.toModel() }
        }.flowOn(Dispatchers.IO)
    }

    override fun observeCompletedChapters(): Flow<Set<Pair<String, Int>>> {
        return chapterCompletionDao.observeAll().map { entities ->
            entities.asSequence().map { it.book to it.chapter }.toSet()
        }.flowOn(Dispatchers.IO)
    }

    override fun observeChapterCompletions(): Flow<List<ChapterCompletionEntity>> {
        return chapterCompletionDao.observeAll().map { entities ->
            entities.sortedByDescending { it.completedAtEpochMillis }
        }.flowOn(Dispatchers.IO)
    }

    override fun observeLastReadPosition(): Flow<Pair<String, Int>?> {
        return readingPreferenceDao.observePreferences().map { prefs ->
            if ((prefs?.lastBook != null) && (prefs.lastChapter != null)) {
                prefs.lastBook to prefs.lastChapter
            } else null
        }.flowOn(Dispatchers.IO)
    }

    override fun observeLastReadVerseId(): Flow<Long?> {
        return readingPreferenceDao.observePreferences().map { it?.lastVerseId }.flowOn(Dispatchers.IO)
    }

    override fun observeSpeechRate(): Flow<Float> {
        return readingPreferenceDao.observePreferences().map { it?.speechRate ?: 1.0f }.flowOn(Dispatchers.IO)
    }

    override fun observeSelectedVoice(): Flow<String?> {
        return readingPreferenceDao.observePreferences().map { it?.selectedVoice }.flowOn(Dispatchers.IO)
    }

    override fun observeImportProgress(): Flow<Float> = dataImporter.importProgress

    override fun observeHighlights(): Flow<List<HighlightItem>> {
        return highlightDao.observeAll().map { entities ->
            entities.map {
                HighlightItem(
                    id = it.id,
                    verseId = it.verseId,
                    colorName = it.colorName,
                    createdAtEpochMillis = it.createdAtEpochMillis
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun addBookmark(verseId: Long) = withContext(Dispatchers.IO) {
        bookmarkDao.insert(
            BookmarkEntity(
                verseId = verseId,
                createdAtEpochMillis = System.currentTimeMillis()
            )
        )
    }

    override suspend fun removeBookmark(verseId: Long) = withContext(Dispatchers.IO) {
        bookmarkDao.deleteByVerse(verseId)
    }

    override suspend fun toggleBookmark(verseId: Long) = withContext(Dispatchers.IO) {
        val existing = bookmarkDao.getByVerse(verseId)
        if (existing != null) {
            bookmarkDao.deleteByVerse(verseId)
        } else {
            bookmarkDao.insert(
                BookmarkEntity(
                    verseId = verseId,
                    createdAtEpochMillis = System.currentTimeMillis()
                )
            )
        }
    }

    override suspend fun removeHighlight(verseId: Long) = withContext(Dispatchers.IO) {
        highlightDao.deleteByVerse(verseId)
    }

    override suspend fun toggleHighlight(verseId: Long, colorName: String) = withContext(Dispatchers.IO) {
        val existing = highlightDao.getByVerse(verseId)
        if (existing != null && existing.colorName == colorName) {
            highlightDao.deleteByVerse(verseId)
        } else {
            highlightDao.insert(
                HighlightEntity(
                    verseId = verseId,
                    colorName = colorName,
                    createdAtEpochMillis = System.currentTimeMillis()
                )
            )
        }
    }

    override fun observeAllMarginalia(): Flow<List<MarginaliaEntity>> {
        return marginaliaDao.observeAll().flowOn(Dispatchers.IO)
    }

    override suspend fun savePersonalNote(verseId: Long, note: String, category: String?) = withContext(Dispatchers.IO) {
        personalNoteDao.upsert(
            PersonalNoteEntity(
                verseId = verseId,
                note = note,
                updatedAtEpochMillis = System.currentTimeMillis(),
                category = category
            )
        )
    }

    override suspend fun addHighlight(verseId: Long, colorName: String) = withContext(Dispatchers.IO) {
        highlightDao.insert(
            HighlightEntity(
                verseId = verseId,
                colorName = colorName,
                createdAtEpochMillis = System.currentTimeMillis()
            )
        )
    }

    override suspend fun setExplanationDepth(level: ExplanationDepth) = withContext(Dispatchers.IO) {
        val current = readingPreferenceDao.getPreferences() ?: ReadingPreferenceEntity(explanationLevel = level.name)
        readingPreferenceDao.upsert(
            current.copy(explanationLevel = level.name)
        )
    }

    override suspend fun saveLastReadPosition(book: String, chapter: Int, verseId: Long?) = withContext(Dispatchers.IO) {
        readingPreferenceDao.updateLastRead(book, chapter, verseId)
    }

    override suspend fun saveSpeechRate(rate: Float) = withContext(Dispatchers.IO) {
        val current = readingPreferenceDao.getPreferences() ?: ReadingPreferenceEntity(explanationLevel = ExplanationDepth.HISTORICAL_LINGUISTIC.name)
        readingPreferenceDao.upsert(current.copy(speechRate = rate))
    }

    override suspend fun saveSelectedVoice(voiceName: String?) = withContext(Dispatchers.IO) {
        val current = readingPreferenceDao.getPreferences() ?: ReadingPreferenceEntity(explanationLevel = ExplanationDepth.HISTORICAL_LINGUISTIC.name)
        readingPreferenceDao.upsert(current.copy(selectedVoice = voiceName))
    }

    override suspend fun markChapterCompleted(book: String, chapter: Int) = withContext(Dispatchers.IO) {
        val existing = chapterCompletionDao.getCompletion(book, chapter)
        if (existing == null) {
            chapterCompletionDao.upsert(
                ChapterCompletionEntity(
                    book = book,
                    chapter = chapter,
                    completedAtEpochMillis = System.currentTimeMillis(),
                    masteryPoints = 100
                )
            )
            updateStudyStats(chaptersCompleted = 1, pointsEarned = 100)
        }
    }

    override suspend fun getOldestPendingQuestion(cutoff: Long): ChapterCompletionEntity? = withContext(Dispatchers.IO) {
        chapterCompletionDao.getOldestPendingQuestion(cutoff)
    }

    override suspend fun updateLastQuestionTime(id: Long, timestamp: Long) = withContext(Dispatchers.IO) {
        chapterCompletionDao.updateLastQuestionTime(id, timestamp)
    }

    override fun observeLexiconEntry(strongsId: String): Flow<LexiconEntity?> {
        return lexiconDao.observeById(strongsId).flowOn(Dispatchers.IO)
    }

    override suspend fun getLexiconEntries(strongsIds: List<String>): List<LexiconEntity> = withContext(Dispatchers.IO) {
        lexiconDao.getByIds(strongsIds)
    }

    override suspend fun getStrongsOccurrenceCount(strongsId: String): Int = withContext(Dispatchers.IO) {
        verseDao.countStrongsOccurrences(strongsId)
    }

    override fun observeStudyStats(): Flow<List<StudyStatsEntity>> {
        return studyStatsDao.observeAll().flowOn(Dispatchers.IO)
    }

    override suspend fun updateStudyStats(
        chaptersCompleted: Int,
        versesRead: Int,
        minutesSpent: Int,
        pointsEarned: Int
    ) = withContext(Dispatchers.IO) {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val existing = studyStatsDao.getByDate(today) ?: StudyStatsEntity(date = today)
        studyStatsDao.upsert(
            existing.copy(
                chaptersCompleted = existing.chaptersCompleted + chaptersCompleted,
                versesRead = existing.versesRead + versesRead,
                minutesSpent = existing.minutesSpent + minutesSpent,
                pointsEarned = existing.pointsEarned + pointsEarned
            )
        )
    }

    override suspend fun getStreakInfo(): StreakInfo = withContext(Dispatchers.IO) {
        val allStats = studyStatsDao.observeAll().firstOrNull() ?: emptyList()
        if (allStats.isEmpty()) return@withContext StreakInfo(0, 0, 0, null)

        val totalChapters = studyStatsDao.getTotalChaptersCompleted()
        
        var currentStreak = 0
        var longestStreak = 0
        var tempStreak = 0
        
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val today = sdf.parse(sdf.format(java.util.Date()))!!
        
        var lastDate: java.util.Date? = null
        
        allStats.forEach { stat ->
            val date = sdf.parse(stat.date) ?: return@forEach
            if (lastDate == null) {
                val diff = (today.time - date.time) / (1000 * 60 * 60 * 24)
                if (diff <= 1) {
                    currentStreak = 1
                    tempStreak = 1
                }
            } else {
                val diff = (lastDate.time - date.time) / (1000 * 60 * 60 * 24)
                if (diff == 1L) {
                    tempStreak++
                } else {
                    if (tempStreak > longestStreak) longestStreak = tempStreak
                    tempStreak = 1
                }
            }
            lastDate = date
        }
        if (tempStreak > longestStreak) longestStreak = tempStreak
        if (currentStreak > 0) currentStreak = tempStreak

        StreakInfo(currentStreak, longestStreak, totalChapters, allStats.firstOrNull()?.date)
    }

    override fun observeMarginalia(book: String, chapter: Int): Flow<List<DrawingPath>> {
        return marginaliaDao.observeByChapter(book, chapter).map { entity ->
            if (entity == null) emptyList()
            else {
                try {
                    val array = JSONArray(entity.drawingJson)
                    val paths = mutableListOf<DrawingPath>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val pointsArr = obj.getJSONArray("p")
                        val points = mutableListOf<Pair<Float, Float>>()
                        for (j in 0 until pointsArr.length()) {
                            val p = pointsArr.getJSONArray(j)
                            points.add(p.getDouble(0).toFloat() to p.getDouble(1).toFloat())
                        }
                        paths.add(DrawingPath(points, obj.getInt("c"), obj.getDouble("w").toFloat()))
                    }
                    paths
                } catch (_: Exception) {
                    emptyList()
                }
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun saveMarginalia(book: String, chapter: Int, paths: List<DrawingPath>) = withContext(Dispatchers.IO) {
        val array = JSONArray()
        paths.forEach { path ->
            val obj = JSONObject()
            val pointsArr = JSONArray()
            path.points.forEach { (x, y) ->
                val p = JSONArray()
                p.put(x.toDouble())
                p.put(y.toDouble())
                pointsArr.put(p)
            }
            obj.put("p", pointsArr)
            obj.put("c", path.color)
            obj.put("w", path.strokeWidth.toDouble())
            array.put(obj)
        }
        marginaliaDao.upsert(
            MarginaliaEntity(
                book = book,
                chapter = chapter,
                drawingJson = array.toString()
            )
        )
    }

    override suspend fun seedIfEmpty() {
        initializer.ensureInitialized()
        dataImporter.importIfEmpty()
    }

    private fun VerseEntity.toModel(): VerseText {
        return VerseText(
            id = id,
            book = book,
            bookOriginal = bookOriginal,
            chapter = chapter,
            verse = verse,
            section = section,
            originalText = originalText,
            modernizedText = modernizedText,
            standardText = standardText,
            comparativeText = comparativeText,
            strongsText = strongsText,
            hasItalicWords = hasItalicWords
        )
    }

    private fun FrontMatterEntity.toModel(): FrontMatterItem {
        return FrontMatterItem(
            docId = docId,
            title = title,
            textOriginal = textOriginal,
            textModernizedSpelling = textModernizedSpelling
        )
    }
}
