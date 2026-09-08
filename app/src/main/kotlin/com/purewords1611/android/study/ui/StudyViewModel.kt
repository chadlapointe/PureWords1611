package com.purewords1611.android.study.ui

import androidx.lifecycle.*
import com.purewords1611.android.study.data.*
import com.purewords1611.android.study.data.local.ChapterCompletionEntity
import com.purewords1611.android.study.data.local.VerseTitleEntity
import com.purewords1611.android.study.data.local.LexiconEntity
import com.purewords1611.android.study.data.local.StreakInfo
import com.purewords1611.android.study.service.PdfExportManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.*
import android.os.IBinder
import com.purewords1611.android.study.service.BibleAudioService
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import androidx.paging.*
import kotlin.time.Duration.Companion.milliseconds

data class StudyUiState(
    val orthographyMode: OrthographyMode = OrthographyMode.ORIGINAL_1611,
    val translationMode: TranslationMode = TranslationMode.KJV_1611,
    val query: String = "",
    val activeChapter: ChapterIndexEntry? = null,
    val explanationDepth: ExplanationDepth = ExplanationDepth.HISTORICAL_LINGUISTIC,
    val selectedVerseId: Long? = null,
    val selectedVerseNotes: List<String> = emptyList(),
    val selectedVerseExplanations: List<ExplanationEntry> = emptyList(),
    val selectedVerseDisplayText: String = "",
    val lastReadPosition: Pair<String, Int>? = null,
    val lastReadVerseId: Long? = null,
    val highlightedVerseId: Long? = null,
    val isReadingChapter: Boolean = false,
    val scrollToVerseId: Long? = null,
    val scrollToIndex: Int? = null,
    val importProgress: Float = 1.0f,
    val fontSize: Float = 18f,
    val themeMode: StudyThemeMode = StudyThemeMode.LIGHT,
    val currentDestination: RootDestination = RootDestination.HOME,
    val speechRate: Float = 1.0f,
    val availableVoices: List<String> = emptyList(),
    val selectedVoice: String? = null,
    val completedChapters: Set<Pair<String, Int>> = emptySet(),
    val chapterHistory: List<ChapterCompletionEntity> = emptyList(),
    val isParallelAudioEnabled: Boolean = false,
    val isScriptoriumEnabled: Boolean = false,
    val isDatabaseReady: Boolean = false,
    val selectedFont: StudyFont = StudyFont.SYSTEM,
    val activeSeekerTrackId: String? = null,
    val selectedFrontMatter: FrontMatterItem? = null,
    val selectedVerseGlossary: Map<String, String> = emptyMap(),
    val searchFilterTestament: TestamentSection? = null,
    val parsedCitations: List<VerseText> = emptyList(),
    val isOrthographyModernized: Boolean = false,
    val isLexiconEnabled: Boolean = false,
    val selectedStrongsId: String? = null,
    val selectedLexiconEntry: LexiconEntity? = null,
    val selectedStrongsOccurrenceCount: Int = 0,
    val selectedVisualId: String? = null,
    val selectedSoundscape: AmbientSoundscape = AmbientSoundscape.SCRIPTORIUM,
    val streakInfo: StreakInfo = StreakInfo(0, 0, 0, null),
    val dailyReadingLesson: String? = null,
    val isVisualMetadataVisible: Boolean = false,
    val isPencilEnabled: Boolean = false,
    val activeMarginalia: List<DrawingPath> = emptyList(),
    val selectedPhrase: String? = null,
    val comparisonTranslation: TranslationMode = TranslationMode.ESV,
    val isFacsimileMode: Boolean = false,
)

enum class StudyThemeMode { LIGHT, DARK, SEPIA }

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class StudyViewModel @Inject constructor(
    private val repository: StudyRepository,
    private val ambientSoundPlayer: AmbientSoundPlayer,
    private val pdfExportManager: PdfExportManager,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {
    private val orthographyMode = MutableStateFlow(OrthographyMode.ORIGINAL_1611)
    private val translationMode = MutableStateFlow(TranslationMode.KJV_1611)
    private val query = MutableStateFlow("")
    private val selectedVerseId = MutableStateFlow<Long?>(null)
    private val activeChapter = MutableStateFlow<ChapterIndexEntry?>(null)
    private val activeSeekerTrackId = MutableStateFlow<String?>(null)
    private val highlightedVerseId = MutableStateFlow<Long?>(null)
    private val isReadingChapter = MutableStateFlow(value = false)
    private val scrollToVerseId = MutableStateFlow<Long?>(value = null)
    private val scrollToIndex = MutableStateFlow<Int?>(value = null)
    private val selectedFrontMatterDocId = MutableStateFlow<String?>(null)
    private val fontSize = MutableStateFlow(18f)
    private val themeMode = MutableStateFlow(StudyThemeMode.LIGHT)
    private val selectedFont = MutableStateFlow(StudyFont.SYSTEM)
    private val currentDestination = MutableStateFlow(RootDestination.HOME)
    private val isParallelAudioEnabled = MutableStateFlow(value = false)
    private val isScriptoriumEnabled = MutableStateFlow(value = false)
    private val selectedSoundscape = MutableStateFlow(AmbientSoundscape.SCRIPTORIUM)
    private val isOrthographyModernized = MutableStateFlow(value = false)
    private val isLexiconEnabled = MutableStateFlow(value = false)
    private val isPencilEnabled = MutableStateFlow(value = false)
    private val comparisonTranslation = MutableStateFlow(TranslationMode.ESV)
    private val isFacsimileMode = MutableStateFlow(false)
    private val selectedPhrase = MutableStateFlow<String?>(null)
    private val selectedStrongsId = MutableStateFlow<String?>(null)
    private val selectedVisualId = MutableStateFlow<String?>(null)
    private val isVisualMetadataVisible = MutableStateFlow(false)
    private val searchFilterTestament = MutableStateFlow<TestamentSection?>(null)
    private val streakInfo = MutableStateFlow(StreakInfo(0, 0, 0, null))
    private val pagingInitialKey = MutableStateFlow<Int?>(null)
    private var lastRecordedPosition: Int = 0

    private var audioService: BibleAudioService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BibleAudioService.AudioBinder
            audioService = binder.getService().also { s ->
                isBound = true
                viewModelScope.launch { s.isPlaying.collect { isReadingChapter.value = it } }
                viewModelScope.launch { s.currentVerseId.collect { highlightedVerseId.value = it } }
                viewModelScope.launch {
                    repository.observeSpeechRate().collect { s.setSpeechRate(it) }
                }
                viewModelScope.launch {
                    repository.observeSelectedVoice().collect { it?.let { v -> s.setVoice(v) } }
                }
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) { audioService = null; isBound = false }
    }

    private fun <T> Flow<Flow<T>>.flattenByLatest(): Flow<T> = flatMapLatest { it }

    val pagingDataFlow: Flow<PagingData<ReaderItem>> = repository.isReady.flatMapLatest { ready ->
        if (!ready) flowOf(PagingData.empty())
        else {
            combine(
                query.debounce(300.milliseconds),
                pagingInitialKey,
                allVerseTitles,
                translationMode,
                searchFilterTestament,
            ) { q: String, key: Int?, titles: List<VerseTitleEntity>, tMode: TranslationMode, section: TestamentSection? ->
                
                fun isBookFirstInSection(book: String): Boolean {
                    return (book == "Genesis") || (book == "1 Esdras") || (book == "Matthew")
                }

                repository.observeVerses(q, key, section).map { pagingData: PagingData<VerseText> ->
                    pagingData.map { ReaderItem.VerseLine(it) as ReaderItem }
                        .insertSeparators { b, a ->
                            if (q.isNotBlank()) return@insertSeparators null
                            val av = (a as? ReaderItem.VerseLine)?.verse ?: return@insertSeparators null
                            val bv = (b as? ReaderItem.VerseLine)?.verse

                            // 1. Check for Book/Chapter Separators
                            val isSectionStart = (bv == null && (av.chapter == 1 && av.verse == 1 && isBookFirstInSection(av.book))) || (bv != null && av.section != bv.section)
                            val isBookStart = (bv == null && av.chapter == 1 && av.verse == 1) || (bv != null && av.book != bv.book)
                            val isChapterStart = (bv == null && av.verse == 1) || (bv != null && (av.chapter != bv.chapter || av.book != bv.book))

                            val separator = if (isChapterStart) {
                                ReaderItem.CompositeHeader(
                                    section = if (isSectionStart) av.section else null,
                                    book = av.book,
                                    bookOriginal = av.bookOriginal,
                                    chapter = av.chapter,
                                    showBookHeader = isBookStart,
                                )
                            } else if (bv == null) {
                                // We jumped to a mid-chapter verse. 
                                // Show a small chapter indicator or nothing?
                                // For now, let's show the full header if it's the very top of our view 
                                // so the user knows where they are.
                                ReaderItem.CompositeHeader(
                                    chapter = av.chapter,
                                    book = av.book,
                                    showBookHeader = false,
                                )
                            } else null
    
                            // 2. Check for Verse Titles
                            val titleEntity = titles.find { 
                                it.book == av.book && 
                                it.chapter == av.chapter && 
                                it.verse == av.verse && 
                                it.translation == tMode.name 
                            }
                            
                            separator ?: titleEntity?.let { 
                                ReaderItem.VerseTitle(it.title, tMode)
                            }
                        }
                }
            }.flattenByLatest()
        }
    }.cachedIn(viewModelScope)

    private val selectedVerseDetails = repository.isReady.flatMapLatest { ready ->
        if (!ready) flowOf(Quadruple(emptyList(), emptyList(), "", emptyMap()))
        else selectedVerseId.flatMapLatest { id ->
            if (id == null) flowOf(Quadruple(emptyList(), emptyList(), "", emptyMap()))
            else {
                val verseFlow = flow { emit(repository.getVerse(id)) }
                combine(
                    repository.observeMarginalNotes(id),
                    repository.observeExplanationDepth().flatMapLatest { repository.observeExplanations(id, it) },
                    verseFlow,
                ) { n, e, v ->
                    val text = v?.let { "${it.book} ${it.chapter}:${it.verse}" } ?: "Verse $id"
                    val glossary = v?.let { FalseFriendGlossary.getFalseFriends(it.originalText) } ?: emptyMap()
                    Quadruple(n.map { it.note }, e, text, glossary)
                }
            }
        }
    }

    data class Quadruple<out A, out B, out C, out D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
    )

    val chapterIndex = repository.observeChapterIndex()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    
    private val chapterIndexMap = chapterIndex.map { list ->
        list.associateBy { "${it.book}_${it.chapter}" }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
    
    val allFrontMatter = repository.observeAllFrontMatter()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val bookmarks = repository.observeBookmarks()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        
    val personalNotes = repository.observePersonalNotes()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        
    val highlights = repository.observeHighlights()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val seekerTracks = repository.observeSeekerTracks()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val chapterSummaries = repository.observeAllChapterSummaries()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val allVerseTitles = repository.observeAllVerseTitles()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val seekerSteps = activeSeekerTrackId.flatMapLatest { 
        it?.let { repository.observeSeekerSteps(it) } ?: flowOf(emptyList()) 
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activeMarginalia = activeChapter.flatMapLatest { ch ->
        if (ch == null) flowOf(emptyList())
        else repository.observeMarginalia(ch.book, ch.chapter)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val selectedLexiconEntry = selectedStrongsId.flatMapLatest { id ->
        if (id == null) flowOf(null)
        else repository.observeLexiconEntry(id)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val selectedStrongsOccurrenceCount = selectedStrongsId.flatMapLatest { id ->
        if (id == null) flowOf(0)
        else flow { emit(repository.getStrongsOccurrenceCount(id)) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val selectedFrontMatter = selectedFrontMatterDocId.flatMapLatest { 
        it?.let { repository.observeFrontMatter(it) } ?: flowOf(null) 
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val parsedCitations = query.debounce(500.milliseconds).flatMapLatest { q ->
        if (q.isBlank()) flowOf(emptyList())
        else flow {
            val citations = BibleCitationParser.parse(q)
            val verses = citations.mapNotNull { 
                repository.getChapterVerses(it.book, it.chapter).find { v -> v.verse == it.verse }
            }
            emit(verses)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<StudyUiState> = repository.isReady.flatMapLatest { ready ->
        if (!ready) {
            flowOf(StudyUiState(isDatabaseReady = false))
        } else {
            combine(
                orthographyMode, translationMode, query, activeChapter,
                repository.observeExplanationDepth(),
                repository.observeLastReadPosition(),
                repository.observeLastReadVerseId(),
                repository.observeImportProgress(),
                fontSize,
                themeMode,
                selectedFont,
                selectedVerseId,
                selectedVerseDetails,
                highlightedVerseId,
                isReadingChapter,
                scrollToVerseId,
                currentDestination,
                activeSeekerTrackId,
                selectedFrontMatter,
                repository.observeCompletedChapters(),
                repository.observeChapterCompletions(),
                isParallelAudioEnabled,
                isScriptoriumEnabled,
                selectedSoundscape,
                isOrthographyModernized,
                isLexiconEnabled,
                isPencilEnabled,
                selectedStrongsId,
                selectedLexiconEntry,
                selectedStrongsOccurrenceCount,
                selectedVisualId,
                isVisualMetadataVisible,
                searchFilterTestament,
                parsedCitations,
                streakInfo,
                activeMarginalia,
                selectedPhrase,
                comparisonTranslation,
                isFacsimileMode,
            ) { args ->
                StudyUiState(
                    orthographyMode = args[0] as OrthographyMode,
                    translationMode = args[1] as TranslationMode,
                    query = args[2] as String,
                    activeChapter = args[3] as ChapterIndexEntry?,
                    explanationDepth = args[4] as ExplanationDepth,
                    lastReadPosition = args[5] as Pair<String, Int>?,
                    lastReadVerseId = args[6] as Long?,
                    importProgress = args[7] as Float, fontSize = args[8] as Float, themeMode = args[9] as StudyThemeMode,
                    selectedFont = args[10] as StudyFont,
                    selectedVerseId = args[11] as Long?,
                    selectedVerseNotes = (args[12] as Quadruple<List<String>, List<ExplanationEntry>, String, Map<String, String>>).first,
                    selectedVerseExplanations = (args[12] as Quadruple<List<String>, List<ExplanationEntry>, String, Map<String, String>>).second,
                    selectedVerseDisplayText = (args[12] as Quadruple<List<String>, List<ExplanationEntry>, String, Map<String, String>>).third,
                    selectedVerseGlossary = (args[12] as Quadruple<List<String>, List<ExplanationEntry>, String, Map<String, String>>).fourth,
                    highlightedVerseId = args[13] as Long?,
                    isReadingChapter = args[14] as Boolean,
                    scrollToVerseId = args[15] as Long?,
                    currentDestination = args[16] as RootDestination,
                    activeSeekerTrackId = args[17] as String?,
                    selectedFrontMatter = args[18] as FrontMatterItem?,
                    completedChapters = args[19] as Set<Pair<String, Int>>,
                    chapterHistory = args[20] as List<ChapterCompletionEntity>,
                    isParallelAudioEnabled = args[21] as Boolean,
                    isScriptoriumEnabled = args[22] as Boolean,
                    selectedSoundscape = args[23] as AmbientSoundscape,
                    isOrthographyModernized = args[24] as Boolean,
                    isLexiconEnabled = args[25] as Boolean,
                    isPencilEnabled = args[26] as Boolean,
                    selectedStrongsId = args[27] as String?,
                    selectedLexiconEntry = args[28] as LexiconEntity?,
                    selectedStrongsOccurrenceCount = args[29] as Int,
                    selectedVisualId = args[30] as String?,
                    isVisualMetadataVisible = args[31] as Boolean,
                    searchFilterTestament = args[32] as TestamentSection?,
                    parsedCitations = args[33] as List<VerseText>,
                    streakInfo = args[34] as StreakInfo,
                    activeMarginalia = args[35] as List<DrawingPath>,
                    selectedPhrase = args[36] as String?,
                    comparisonTranslation = args[37] as TranslationMode,
                    isFacsimileMode = args[38] as Boolean,
                    dailyReadingLesson = calculateDailyLesson(),
                    speechRate = audioService?.getSpeechRate() ?: 1.0f,
                    availableVoices = audioService?.getAvailableVoices() ?: emptyList(),
                    selectedVoice = audioService?.getSelectedVoice(),
                    isDatabaseReady = true,
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StudyUiState())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.seedIfEmpty()
            refreshStats()
        }
        viewModelScope.launch {
            repository.isReady.first { isReady -> isReady }
            chapterIndex.first { it.isNotEmpty() }.firstOrNull()?.let { 
                if (activeChapter.value == null) activeChapter.value = it 
            }
        }
        context.bindService(Intent(context, BibleAudioService::class.java), connection, Context.BIND_AUTO_CREATE)
    }

    override fun onCleared() { super.onCleared(); if (isBound) context.unbindService(connection) }
    
    fun toggleFacsimileMode() {
        isFacsimileMode.value = !isFacsimileMode.value
    }
    fun updateQuery(v: String) { 
        query.value = v 
        if (v.isNotBlank() && currentDestination.value != RootDestination.READ) {
            currentDestination.value = RootDestination.SEARCH
        }
    }
    fun setOrthographyMode(m: OrthographyMode) { 
        pagingInitialKey.value = lastRecordedPosition
        orthographyMode.value = m 
        audioService?.onVersionChanged(m, translationMode.value)
    }
    fun setTranslationMode(t: TranslationMode) {
        pagingInitialKey.value = lastRecordedPosition
        translationMode.value = t
        audioService?.onVersionChanged(orthographyMode.value, t)
    }
    fun toggleTranslationMode() { 
        pagingInitialKey.value = lastRecordedPosition
        translationMode.value = when (translationMode.value) {
            TranslationMode.KJV_1611 -> TranslationMode.KJV_STANDARD
            TranslationMode.KJV_STANDARD -> TranslationMode.ESV
            TranslationMode.ESV -> TranslationMode.KJV_1611
        }
        audioService?.onVersionChanged(orthographyMode.value, translationMode.value)
    }
    fun toggleParallelAudio() {
        val newVal = !isParallelAudioEnabled.value
        isParallelAudioEnabled.value = newVal
        audioService?.setParallelMode(newVal)
    }
    fun toggleScriptorium() {
        val newVal = !isScriptoriumEnabled.value
        isScriptoriumEnabled.value = newVal
        ambientSoundPlayer.toggleScriptoriumMode(newVal, selectedSoundscape.value)
    }
    fun setSoundscape(s: AmbientSoundscape) {
        selectedSoundscape.value = s
        ambientSoundPlayer.setSoundscape(s)
    }
    fun selectVerse(id: Long) { 
        android.util.Log.d("StudyViewModel", "selectVerse: $id")
        selectedVerseId.value = id 
    }
    fun clearSelectedVerse() { 
        selectedVerseId.value = null
        selectedPhrase.value = null
    }
    fun selectChapter(b: String, c: Int, vId: Long? = null, verseNumber: Int? = null) { 
        viewModelScope.launch { 
            val ch = chapterIndexMap.value["${b}_$c"]
            var targetVerseNumber = verseNumber
            
            if (targetVerseNumber == null && vId != null) {
                // Resolve vId to verse number
                repository.getVerse(vId)?.let { targetVerseNumber = it.verse }
            }

            if (ch != null) {
                val vn = targetVerseNumber ?: 1
                pagingInitialKey.value = ch.position + (vn - 1).coerceAtLeast(0)
                scrollToIndex.value = if (vn > 1) 1 else 0
                activeChapter.value = ch
            } else {
                val vn = targetVerseNumber ?: 1
                val offset = repository.getChapterOffset(b, c)
                pagingInitialKey.value = offset + (vn - 1).coerceAtLeast(0)
                scrollToIndex.value = if (vn > 1) 1 else 0
                activeChapter.value = chapterIndexMap.value["${b}_$c"]
            }
        } 
    }
    fun onScrollToVerseHandled() { 
        scrollToVerseId.value = null
        scrollToIndex.value = null
    }
    fun updateActivePositionFromScroll(b: String, c: Int, vId: Long?, verseNumber: Int? = null) { 
        if ((activeChapter.value?.book != b) || (activeChapter.value?.chapter != c)) { 
            activeChapter.value = chapterIndexMap.value["${b}_$c"]
        }
        
        val ch = chapterIndexMap.value["${b}_$c"]
        if (ch != null && verseNumber != null) {
            lastRecordedPosition = ch.position + (verseNumber - 1)
        } else if (ch != null) {
            lastRecordedPosition = ch.position
        } else if (vId != null) {
            viewModelScope.launch {
                lastRecordedPosition = repository.getVerseOffset(vId)
            }
        }

        if (uiState.value.lastReadVerseId != vId) {
            viewModelScope.launch { repository.saveLastReadPosition(b, c, vId) }
        }
    }
    fun setExplanationDepth(d: ExplanationDepth) { viewModelScope.launch { repository.setExplanationDepth(d) } }
    fun selectSeekerTrack(id: String) { activeSeekerTrackId.value = id }
    fun addBookmarkForSelectedVerse() { selectedVerseId.value?.let { viewModelScope.launch { repository.addBookmark(it) } } }
    fun savePersonalNoteForSelectedVerse(n: String, c: String? = null) { n.trim().takeIf { it.isNotBlank() }?.let { note -> selectedVerseId.value?.let { viewModelScope.launch { repository.savePersonalNote(it, note, c) } } } }
    fun addHighlightForSelectedVerse(c: String = "yellow") { selectedVerseId.value?.let { viewModelScope.launch { repository.addHighlight(it, c) } } }
    fun readFullChapter(startId: Long? = null) { 
        viewModelScope.launch { 
            val ch = activeChapter.value ?: return@launch
            val v = repository.getChapterVerses(ch.book, ch.chapter)
            if (v.isEmpty()) return@launch
            val actualStartId = startId ?: uiState.value.lastReadVerseId.takeIf { lastId -> v.any { it.id == lastId } }
            audioService?.playQueue(v, actualStartId, orthographyMode.value, translationMode.value) 
        } 
    }
    fun speakSelectedVerse() { selectedVerseId.value?.let { id -> viewModelScope.launch { repository.getVerse(id)?.let { audioService?.playQueue(listOf(it), it.id, orthographyMode.value, translationMode.value) } } } }
    fun stopReading() { context.startService(Intent(context, BibleAudioService::class.java).apply { action = BibleAudioService.ACTION_PAUSE }) }
    fun setSpeechRate(r: Float) { 
        audioService?.setSpeechRate(r) 
        viewModelScope.launch { repository.saveSpeechRate(r) }
    }
    fun setVoice(v: String) { 
        audioService?.setVoice(v) 
        viewModelScope.launch { repository.saveSelectedVoice(v) }
    }
    fun setFontSize(s: Float) { fontSize.value = s }
    fun setSelectedFont(f: StudyFont) { selectedFont.value = f }
    fun setThemeMode(m: StudyThemeMode) { themeMode.value = m }
    fun setDestination(d: RootDestination) { 
        android.util.Log.d("StudyViewModel", "setDestination: $d")
        currentDestination.value = d 
    }
    fun selectFrontMatter(id: String?) { selectedFrontMatterDocId.value = id }
    fun setSearchFilterTestament(s: TestamentSection?) { searchFilterTestament.value = s }
    fun toggleOrthographyModernization() { isOrthographyModernized.value = !isOrthographyModernized.value }
    fun toggleLexicon() { isLexiconEnabled.value = !isLexiconEnabled.value }
    fun togglePencil() { 
        val newVal = !isPencilEnabled.value
        android.util.Log.d("StudyViewModel", "togglePencil: $newVal")
        isPencilEnabled.value = newVal 
    }
    fun selectPhrase(p: String?) {
        selectedPhrase.value = p
    }
    fun setComparisonTranslation(t: TranslationMode) {
        comparisonTranslation.value = t
    }
    fun selectStrongs(id: String?) { selectedStrongsId.value = id }
    fun selectVisual(id: String?) {
        selectedVisualId.value = id
        if (id == null) isVisualMetadataVisible.value = false
    }
    fun toggleVisualMetadata() { isVisualMetadataVisible.value = !isVisualMetadataVisible.value }

    fun saveMarginalia(paths: List<DrawingPath>) {
        viewModelScope.launch {
            activeChapter.value?.let { ch ->
                repository.saveMarginalia(ch.book, ch.chapter, paths)
            }
        }
    }

    fun exportChapterToPdf() {
        viewModelScope.launch {
            val ch = activeChapter.value ?: return@launch
            val verses = repository.getChapterVerses(ch.book, ch.chapter)
            val marginalia = activeMarginalia.value
            val file = pdfExportManager.exportChapter(ch.book, ch.chapter, verses, marginalia)
            if (file != null) {
                // Share the file
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Export 1611 Chapter"))
            }
        }
    }

    fun refreshStats() {
        viewModelScope.launch {
            streakInfo.value = repository.getStreakInfo()
        }
    }

    private fun calculateDailyLesson(): String {
        val calendar = java.util.Calendar.getInstance()
        val month = calendar[java.util.Calendar.MONTH] + 1
        val day = calendar[java.util.Calendar.DAY_OF_MONTH]
        
        // Very simplified placeholder for 1611 Calendar of Lessons
        // Real implementation would load from JSON
        return when(month) {
            8 -> when(day) {
                26 -> "Dan 10, Acts 24"
                27 -> "Dan 11, Acts 25"
                else -> "Psalms for the Day"
            }
            else -> "Scripture for the Day"
        }
    }
}
