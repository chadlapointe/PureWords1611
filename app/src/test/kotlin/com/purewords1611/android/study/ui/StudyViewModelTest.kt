package com.purewords1611.android.study.ui

import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import androidx.paging.PagingData
import com.purewords1611.android.study.data.*
import com.purewords1611.android.study.data.local.*
import com.purewords1611.android.study.service.PdfExportManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.doReturn
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn as kDoReturn
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class StudyViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val mockAudioPlayer: AmbientSoundPlayer = mock()
    private val mockPdfManager: PdfExportManager = mock()
    private val mockContext: Context = mock {
        on { packageName } kDoReturn "com.purewords1611.android"
    }

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        doReturn(true).`when`(mockContext).bindService(any<Intent>(), any<ServiceConnection>(), any<Int>())
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `updates orthography mode`() = runTest {
        val viewModel = StudyViewModel(FakeStudyRepository(), mockAudioPlayer, mockPdfManager, mockContext)
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.setOrthographyMode(OrthographyMode.MODERNIZED)
        testScheduler.advanceUntilIdle()

        assertEquals(OrthographyMode.MODERNIZED, viewModel.uiState.value.orthographyMode)
    }

    @Test
    fun `loads marginal notes for selected verse`() = runTest {
        val viewModel = StudyViewModel(FakeStudyRepository(), mockAudioPlayer, mockPdfManager, mockContext)
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.selectVerse(1L)
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.selectedVerseNotes.isNotEmpty())
        assertEquals("Anchor note", viewModel.uiState.value.selectedVerseNotes.first())
    }

    @Test
    fun `updates explanation depth preference`() = runTest {
        val repository = FakeStudyRepository()
        val viewModel = StudyViewModel(repository, mockAudioPlayer, mockPdfManager, mockContext)
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.setExplanationDepth(ExplanationDepth.MINIMAL)
        testScheduler.advanceUntilIdle()

        assertEquals(ExplanationDepth.MINIMAL, viewModel.uiState.value.explanationDepth)
    }
}

private class FakeStudyRepository : StudyRepository {
    override val isReady: StateFlow<Boolean> = MutableStateFlow(true)
    private val explanationDepth = MutableStateFlow(ExplanationDepth.HISTORICAL_LINGUISTIC)

    private val verses = listOf(
        VerseText(
            id = 1L,
            book = "Psalmes",
            bookOriginal = null,
            chapter = 12,
            verse = 6,
            section = TestamentSection.OLD_TESTAMENT,
            originalText = "Original text",
            modernizedText = "Modern text",
            standardText = null,
            comparativeText = null,
            hasItalicWords = false
        )
    )

    override fun observeVerses(query: String, initialKey: Int?, section: TestamentSection?): Flow<PagingData<VerseText>> {
        return flowOf(PagingData.from(verses))
    }

    override fun observeFullVerses(): Flow<List<VerseText>> = flowOf(verses)

    override fun observeChapterVerses(book: String, chapter: Int): Flow<List<VerseText>> {
        return flowOf(verses.filter { it.book == book && it.chapter == chapter })
    }

    override fun observeChapterIndex(): Flow<List<ChapterIndexEntry>> {
        return flowOf(
            listOf(
                ChapterIndexEntry(
                    book = "Psalmes",
                    bookOriginal = null,
                    chapter = 12,
                    section = TestamentSection.OLD_TESTAMENT,
                    firstVerseId = 1L,
                    position = 0,
                    verseCount = 1
                )
            )
        )
    }

    override suspend fun initializeDatabase() = Unit
    override suspend fun getChapterVerses(book: String, chapter: Int): List<VerseText> = verses
    override suspend fun getVerse(id: Long): VerseText? = verses.find { it.id == id }
    override suspend fun getChapterOffset(book: String, chapter: Int): Int = 0
    override suspend fun getVerseOffset(verseId: Long): Int = 0

    override fun observeChapterSummary(book: String, chapter: Int): Flow<ChapterSummaryEntity?> = flowOf(null)
    override fun observeAllChapterSummaries(): Flow<List<ChapterSummaryEntity>> = flowOf(emptyList())
    override fun observeVerseTitles(book: String, chapter: Int): Flow<List<VerseTitleEntity>> = flowOf(emptyList())
    override fun observeAllVerseTitles(): Flow<List<VerseTitleEntity>> = flowOf(emptyList())

    override fun observeMarginalNotes(verseId: Long): Flow<List<MarginalNote>> {
        return if (verseId == 1L) {
            flowOf(listOf(MarginalNote(id = 1L, verseId = 1L, note = "Anchor note")))
        } else {
            flowOf(emptyList())
        }
    }

    override fun observeExplanations(
        verseId: Long,
        level: ExplanationDepth
    ): Flow<List<ExplanationEntry>> {
        return if (verseId == 1L) {
            flowOf(
                listOf(
                    ExplanationEntry(
                        id = "e1",
                        verseId = 1L,
                        level = level,
                        contentMarkdown = "Explanation at ${level.name}"
                    )
                )
            )
        } else {
            flowOf(emptyList())
        }
    }

    override fun observeExplanationDepth(): Flow<ExplanationDepth> = explanationDepth

    override suspend fun setExplanationDepth(level: ExplanationDepth) {
        explanationDepth.value = level
    }

    override fun observeSeekerTracks(): Flow<List<SeekerTrackEntry>> {
        return flowOf(
            listOf(
                SeekerTrackEntry(
                    trackId = "FULL_EVIDENCE",
                    title = "Full",
                    description = "full path"
                )
            )
        )
    }

    override fun observeSeekerSteps(trackId: String): Flow<List<SeekerStepEntry>> {
        return flowOf(
            listOf(
                SeekerStepEntry(
                    stepId = "s1",
                    trackId = trackId,
                    sequence = 1,
                    title = "Start",
                    bodyMarkdown = "Read"
                )
            )
        )
    }

    override fun observeBookmarks(): Flow<List<BookmarkItem>> = flowOf(emptyList())
    override fun observeHighlights(): Flow<List<HighlightItem>> = flowOf(emptyList())
    override fun observePersonalNotes(): Flow<List<PersonalNoteItem>> = flowOf(emptyList())
    override fun observeFrontMatter(docId: String): Flow<FrontMatterItem?> = flowOf(null)
    override fun observeAllFrontMatter(): Flow<List<FrontMatterItem>> = flowOf(emptyList())
    override fun observeCompletedChapters(): Flow<Set<Pair<String, Int>>> = flowOf(emptySet())
    override fun observeChapterCompletions(): Flow<List<ChapterCompletionEntity>> = flowOf(emptyList())
    override fun observeLastReadPosition(): Flow<Pair<String, Int>?> = flowOf(null)
    override fun observeLastReadVerseId(): Flow<Long?> = flowOf(null)
    override fun observeSpeechRate(): Flow<Float> = flowOf(1.0f)
    override fun observeSelectedVoice(): Flow<String?> = flowOf(null)
    override fun observeImportProgress(): Flow<Float> = flowOf(1.0f)

    override suspend fun addBookmark(verseId: Long) = Unit
    override suspend fun removeBookmark(verseId: Long) = Unit
    override suspend fun toggleBookmark(verseId: Long) = Unit
    override suspend fun addHighlight(verseId: Long, colorName: String) = Unit
    override suspend fun removeHighlight(verseId: Long) = Unit
    override suspend fun toggleHighlight(verseId: Long, colorName: String) = Unit
    override fun observeAllMarginalia(): Flow<List<MarginaliaEntity>> = flowOf(emptyList())
    override suspend fun savePersonalNote(verseId: Long, note: String, category: String?) = Unit
    override suspend fun saveLastReadPosition(book: String, chapter: Int, verseId: Long?) = Unit
    override suspend fun saveSpeechRate(rate: Float) = Unit
    override suspend fun saveSelectedVoice(voiceName: String?) = Unit
    override suspend fun markChapterCompleted(book: String, chapter: Int) = Unit
    override suspend fun getOldestPendingQuestion(cutoff: Long): ChapterCompletionEntity? = null
    override suspend fun updateLastQuestionTime(id: Long, timestamp: Long) = Unit
    override fun observeLexiconEntry(strongsId: String): Flow<LexiconEntity?> = flowOf(null)
    override suspend fun getLexiconEntries(strongsIds: List<String>): List<LexiconEntity> = emptyList()
    override suspend fun getStrongsOccurrenceCount(strongsId: String): Int = 0
    override fun observeStudyStats(): Flow<List<StudyStatsEntity>> = flowOf(emptyList())
    override suspend fun updateStudyStats(chaptersCompleted: Int, versesRead: Int, minutesSpent: Int, pointsEarned: Int) = Unit
    override suspend fun getStreakInfo(): StreakInfo = StreakInfo(0, 0, 0, null)
    override fun observeMarginalia(book: String, chapter: Int): Flow<List<DrawingPath>> = flowOf(emptyList())
    override suspend fun saveMarginalia(book: String, chapter: Int, paths: List<DrawingPath>) = Unit
    override suspend fun seedIfEmpty() = Unit
}
