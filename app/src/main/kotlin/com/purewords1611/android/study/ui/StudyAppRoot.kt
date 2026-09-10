package com.purewords1611.android.study.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.*
import androidx.paging.LoadState
import com.purewords1611.android.analytics.AnalyticsManager
import com.purewords1611.android.study.data.*
import com.purewords1611.android.study.data.local.ChapterSummaryEntity
import com.purewords1611.android.study.data.local.LexiconEntity
import com.purewords1611.android.R
import com.purewords1611.android.study.ui.components.MarginaliaDrawingView
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds
import android.content.Intent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyAppRoot(
    analyticsManager: AnalyticsManager,
    viewModel: StudyViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val chapterIndex by viewModel.chapterIndex.collectAsState()
    val allFrontMatter by viewModel.allFrontMatter.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val personalNotes by viewModel.personalNotes.collectAsState()
    val highlights by viewModel.highlights.collectAsState()
    val seekerTracks by viewModel.seekerTracks.collectAsState()
    val chapterSummaries by viewModel.chapterSummaries.collectAsState()
    val seekerSteps by viewModel.seekerSteps.collectAsState()
    val selectedFrontMatter by viewModel.selectedFrontMatter.collectAsState()

    val pagingItems = viewModel.pagingDataFlow.collectAsLazyPagingItems()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }

    val themeColors = when (uiState.themeMode) {
        StudyThemeMode.LIGHT -> lightColorScheme()
        StudyThemeMode.DARK -> darkColorScheme(surface = Color(0xFF121212), onSurface = Color.White, background = Color.Black, primaryContainer = Color(0xFF1E1E1E), onPrimaryContainer = Color.White)
        StudyThemeMode.SEPIA -> lightColorScheme(surface = Color(0xFFF4ECD8), onSurface = Color(0xFF5B4636), background = Color(0xFFE8DDC0), primaryContainer = Color(0xFFDED3B9), onPrimaryContainer = Color(0xFF5B4636))
    }

    MaterialTheme(colorScheme = themeColors) {
        if (!uiState.isDatabaseReady) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.bible_title_1611),
                        contentDescription = "KJV 1611 Title Page",
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .aspectRatio(0.65f)
                            .clip(MaterialTheme.shapes.medium)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium),
                        contentScale = ContentScale.FillWidth
                    )
                    Spacer(Modifier.height(48.dp))
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Establishing the Pure Words...",
                        style = MaterialTheme.typography.labelLarge.copy(
                            letterSpacing = 2.sp,
                            fontFamily = FontFamily.Serif,
                            fontStyle = FontStyle.Italic
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
            return@MaterialTheme
        }
        var showSettings by remember { mutableStateOf(false) }
        var showBibleSelector by remember { mutableStateOf(false) }
        var showStudyHub by remember { mutableStateOf(false) }

        LaunchedEffect(uiState.selectedVerseId) { if (uiState.selectedVerseId != null) showStudyHub = true }
        LaunchedEffect(uiState.currentDestination) { analyticsManager.trackScreenView("Study_${uiState.currentDestination.name}") }

        val textFont = getFontFamily(uiState.selectedFont, uiState.translationMode)
        val metadataFont = if (uiState.translationMode == TranslationMode.KJV_1611) FontFamily.Serif else textFont

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Spacer(Modifier.height(16.dp))
                    Text("Pure Words 1611", Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
                    HorizontalDivider()
                    
                    Text("Study Tools", Modifier.padding(16.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    NavigationDrawerItem(
                        label = { Text("My Notes & Highlights") },
                        selected = uiState.currentDestination == RootDestination.NOTES,
                        onClick = { viewModel.setDestination(RootDestination.NOTES); scope.launch { drawerState.close() } },
                        icon = { Icon(Icons.Default.Edit, null) }
                    )
                    NavigationDrawerItem(
                        label = { Text("Seeker's Path") },
                        selected = uiState.currentDestination == RootDestination.SEEKER_PATH,
                        onClick = { viewModel.setDestination(RootDestination.SEEKER_PATH); scope.launch { drawerState.close() } },
                        icon = { Icon(Icons.Default.Info, null) }
                    )
                    NavigationDrawerItem(
                        label = { Text("1611 Visual Gallery") },
                        selected = uiState.currentDestination == RootDestination.GALLERY,
                        onClick = { viewModel.setDestination(RootDestination.GALLERY); scope.launch { drawerState.close() } },
                        icon = { Icon(Icons.Default.LocationOn, null) }
                    )

                    HorizontalDivider()
                    Text("1611 Front Matter", Modifier.padding(16.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    allFrontMatter.forEach { item ->
                        NavigationDrawerItem(
                            label = { Text(item.title, fontFamily = metadataFont) }, 
                            selected = (uiState.currentDestination == RootDestination.FRONT_MATTER) && (selectedFrontMatter?.docId == item.docId), 
                            onClick = { viewModel.selectFrontMatter(item.docId); viewModel.setDestination(RootDestination.FRONT_MATTER); scope.launch { drawerState.close() } }, 
                            icon = { Icon(Icons.Default.Info, null) }
                        )
                    }
                    
                    Spacer(Modifier.weight(1f))
                    HorizontalDivider()
                    NavigationDrawerItem(label = { Text("App Settings") }, selected = false, onClick = { showSettings = true; scope.launch { drawerState.close() } }, icon = { Icon(Icons.Default.Settings, null) })
                }
            }
        ) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    Surface(
                        tonalElevation = 3.dp,
                        shadowElevation = 4.dp
                    ) {
                        Column {
                            TopAppBar(
                                navigationIcon = { IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, "Menu") } },
                                title = { 
                                    if (uiState.currentDestination != RootDestination.READ && uiState.currentDestination != RootDestination.PARALLEL) {
                                        Text(uiState.currentDestination.label)
                                    }
                                },
                                actions = {
                                    if (uiState.currentDestination == RootDestination.READ) {
                                        IconButton(onClick = viewModel::togglePencil) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Pencil",
                                                tint = if (uiState.isPencilEnabled) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                            )
                                        }
                                        IconButton(onClick = viewModel::toggleFacsimileMode) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = "Facsimile Flashback",
                                                tint = if (uiState.isFacsimileMode) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                            )
                                        }
                                    }
                                    VersionToggle(
                                        mode = uiState.translationMode,
                                        onToggle = viewModel::toggleTranslationMode
                                    )
                                }
                            )

                            if (uiState.currentDestination == RootDestination.READ || uiState.currentDestination == RootDestination.PARALLEL) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { showBibleSelector = true }
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val title = uiState.activeChapter?.let { ch ->
                                                val b = if (uiState.translationMode == TranslationMode.KJV_1611 && uiState.orthographyMode == OrthographyMode.ORIGINAL_1611) {
                                                    ch.bookOriginal ?: ch.book
                                                } else {
                                                    ch.book
                                                }
                                                val c = if (uiState.translationMode == TranslationMode.KJV_1611 && uiState.orthographyMode == OrthographyMode.ORIGINAL_1611) {
                                                    toRomanNumeral(ch.chapter)
                                                } else {
                                                    ch.chapter.toString()
                                                }
                                                "$b $c"
                                            } ?: "Select Chapter"
                                            
                                            Text(
                                                text = title, 
                                                maxLines = 3, 
                                                fontFamily = metadataFont,
                                                fontWeight = FontWeight.ExtraBold,
                                                style = MaterialTheme.typography.titleMedium,
                                                modifier = Modifier.weight(1f),
                                                lineHeight = 22.sp
                                            )
                                            
                                            Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.padding(start = 8.dp))
                                        }
                                        
                                        if (uiState.currentDestination == RootDestination.READ) {
                                            Spacer(Modifier.width(16.dp))
                                            VerticalDivider(modifier = Modifier.height(32.dp))
                                            Spacer(Modifier.width(8.dp))
                                            if (uiState.isReadingChapter) {
                                                IconButton(onClick = viewModel::stopReading) { 
                                                    Icon(Icons.Default.Clear, "Stop", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.error) 
                                                }
                                            } else {
                                                IconButton(onClick = { viewModel.readFullChapter() }) { 
                                                    Icon(Icons.Default.PlayArrow, "Read", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary) 
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                bottomBar = {
                    NavigationBar {
                        val mainDestinations = listOf(
                            RootDestination.HOME,
                            RootDestination.READ,
                            RootDestination.PARALLEL,
                            RootDestination.SEARCH
                        )
                        mainDestinations.forEach { item ->
                            NavigationBarItem(
                                selected = uiState.currentDestination == item,
                                onClick = { 
                                    viewModel.setDestination(item)
                                    viewModel.selectFrontMatter(null) 
                                },
                                label = { Text(item.label) },
                                icon = { 
                                    Icon(
                                        imageVector = when(item) { 
                                            RootDestination.HOME -> Icons.Default.Home
                                            RootDestination.READ -> Icons.AutoMirrored.Filled.List
                                            RootDestination.PARALLEL -> Icons.Default.ThumbUp
                                            RootDestination.SEARCH -> Icons.Default.Search
                                            else -> Icons.Default.Info 
                                        }, 
                                        contentDescription = null
                                    ) 
                                }
                            )
                        }
                    }
                }
            ) { p ->
                Box(Modifier.padding(p)) {
                    when (uiState.currentDestination) {
                        RootDestination.HOME -> HomeScreen(uiState, allFrontMatter, bookmarks, personalNotes, chapterIndex, onNavigateToRead = { b, c, vId -> viewModel.selectChapter(b, c, vId); viewModel.setDestination(RootDestination.READ) }, onNavigateToFrontMatter = { id -> viewModel.selectFrontMatter(id); viewModel.setDestination(RootDestination.FRONT_MATTER) })
                        RootDestination.READ -> {
                            val summaryMap = remember(chapterSummaries) {
                                chapterSummaries.associateBy { "${it.book}_${it.chapter}" }
                            }
                            ReadScreen(
                                state = uiState, 
                                chapterSummariesMap = summaryMap, 
                                pagingItems = pagingItems, 
                                onVerseSelected = viewModel::selectVerse, 
                                onVerseRangeSelected = { start, end, spanned, text -> viewModel.selectVerseRange(start, end, spanned, text) },
                                onStrongsClick = viewModel::selectStrongs,
                                onGlossaryClick = { definition ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = definition,
                                            duration = SnackbarDuration.Long
                                        )
                                    }
                                },
                                onWordClick = viewModel::selectPhrase,
                                onChapterSelected = viewModel::selectChapter, 
                                onScrollHandled = viewModel::onScrollToVerseHandled, 
                                onUpdateActivePosition = viewModel::updateActivePositionFromScroll,
                                onSaveMarginalia = viewModel::saveMarginalia
                            )
                        }
                        RootDestination.PARALLEL -> {
                            ParallelScreen(
                                state = uiState, 
                                pagingItems = pagingItems, 
                                onVerseSelected = viewModel::selectVerse, 
                                onUpdateActivePosition = viewModel::updateActivePositionFromScroll,
                                onToggleLeft = viewModel::setTranslationMode,
                                onToggleRight = viewModel::setComparisonTranslation
                            )
                        }
                        RootDestination.SEARCH -> {
                            SearchScreen(
                                state = uiState, 
                                pagingItems = pagingItems, 
                                onQueryChange = viewModel::updateQuery,
                                onSectionFilterChange = viewModel::setSearchFilterTestament,
                                onVerseSelected = { b, c, id -> 
                                    viewModel.selectChapter(b, c, id)
                                    viewModel.setDestination(RootDestination.READ) 
                                }
                            )
                        }
                        RootDestination.NOTES -> AnnotationsScreen(
                            highlights = highlights,
                            bookmarks = bookmarks,
                            marginalia = uiState.marginaliaList,
                            personalNotes = personalNotes,
                            onVerseClick = { vId ->
                                viewModel.selectVerse(vId)
                                viewModel.setDestination(RootDestination.READ)
                            },
                            onChapterClick = { book, ch ->
                                viewModel.selectChapter(book, ch)
                                viewModel.setDestination(RootDestination.READ)
                            },
                            onRemoveHighlight = { vId -> viewModel.removeHighlight(vId) },
                            onRemoveBookmark = { vId -> viewModel.removeBookmark(vId) }
                        )
                        RootDestination.SEEKER_PATH -> SeekerPathScreen(seekerTracks, uiState.activeSeekerTrackId, seekerSteps, onS = viewModel::selectSeekerTrack)
                        RootDestination.FRONT_MATTER -> FrontMatterScreen(selectedFrontMatter, uiState.orthographyMode, uiState.fontSize, uiState.selectedFont, uiState.translationMode)
                        RootDestination.GALLERY -> {
                            if (uiState.selectedVisualId != null) {
                                VisualDetailScreen(
                                    id = uiState.selectedVisualId!!,
                                    metadataFont = metadataFont,
                                    showMetadata = uiState.isVisualMetadataVisible,
                                    onToggleMetadata = viewModel::toggleVisualMetadata,
                                    onBack = { viewModel.selectVisual(null) }
                                )
                            } else {
                                GalleryScreen(metadataFont) { viewModel.selectVisual(it) }
                            }
                        }
                    }

                    if (uiState.isFacsimileMode) {
                        FacsimileOverlay(uiState) { viewModel.toggleFacsimileMode() }
                    }
                }

                if (showBibleSelector) BibleSelectorDialog(
                    chapters = chapterIndex,
                    completedChapters = uiState.completedChapters,
                    orthographyMode = uiState.orthographyMode,
                    translationMode = uiState.translationMode,
                    selectedFont = uiState.selectedFont,
                    initialBook = uiState.activeChapter?.book,
                    onChapterSelected = { b, c, v -> viewModel.selectChapter(b, c, verseNumber = v) },
                ) {
                    showBibleSelector = false
                }
                if (showStudyHub) ModalBottomSheet(
                    onDismissRequest = {
                        showStudyHub = false
                        viewModel.clearSelectedVerse()
                    },
                ) {
                    StudyHubSheet(
                        s = uiState,
                        onSp = viewModel::speakSelectedVerse,
                        onR = {
                            if (uiState.selectedVerseId != null) {
                                viewModel.readFullChapter(uiState.selectedVerseId)
                                showStudyHub = false
                            }
                        },
                        onRes = {
                            uiState.activeChapter?.let {
                                viewModel.selectChapter(it.book, it.chapter, null)
                                viewModel.readFullChapter(null)
                            }
                            showStudyHub = false
                        },
                        onTPA = viewModel::toggleParallelAudio,
                        onB = { viewModel.addBookmarkForSelectedVerse() },
                        onH = { colorName -> viewModel.addHighlightForSelectedVerse(colorName) },
                        onN = { note, tag -> viewModel.savePersonalNoteForSelectedVerse(note, tag) },
                        onExport = viewModel::exportChapterToPdf,
                        onSh = { t ->
                            context.startActivity(
                                Intent.createChooser(
                                    Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, t)
                                        type = "text/plain"
                                    },
                                    null,
                                ),
                            )
                        },
                        onCl = {
                            showStudyHub = false
                            viewModel.clearSelectedVerse()
                        },
                    )
                }
                if (showSettings) ModalBottomSheet(onDismissRequest = { showSettings = false }) {
                    StudySettingsSheet(
                        state = uiState,
                        onQ = viewModel::updateQuery,
                        onO = viewModel::setOrthographyMode,
                        onE = viewModel::setExplanationDepth,
                        onSR = viewModel::setSpeechRate,
                        onV = viewModel::setVoice,
                        onFS = viewModel::setFontSize,
                        onF = viewModel::setSelectedFont,
                        onSnd = viewModel::setSoundscape,
                        onTM = viewModel::setThemeMode,
                        onTS = viewModel::toggleScriptorium,
                        onTOM = viewModel::toggleOrthographyModernization,
                        onTL = viewModel::toggleLexicon,
                        onClose = { showSettings = false },
                    )
                }

                if (uiState.selectedLexiconEntry != null) {
                    LexiconEntrySheet(
                        entry = uiState.selectedLexiconEntry,
                        occurrenceCount = uiState.selectedStrongsOccurrenceCount,
                        onSearch = { id -> 
                            viewModel.updateQuery(id)
                            viewModel.setDestination(RootDestination.SEARCH)
                        },
                        onClose = { viewModel.selectStrongs(null) }
                    )
                }
            }
        }
    }
}

@Composable
private fun VersionToggle(mode: TranslationMode, onToggle: () -> Unit) {
    val ff = if (mode == TranslationMode.KJV_1611) FontFamily.Serif else FontFamily.Default
    val label = when (mode) {
        TranslationMode.KJV_1611 -> "1611 KJV"
        TranslationMode.KJV_STANDARD -> "Standard KJV"
        TranslationMode.ESV -> "Modern ESV"
    }
    
    Surface(
        onClick = onToggle,
        shape = MaterialTheme.shapes.medium,
        color = when (mode) {
            TranslationMode.KJV_1611 -> MaterialTheme.colorScheme.primary
            TranslationMode.KJV_STANDARD -> MaterialTheme.colorScheme.tertiary
            TranslationMode.ESV -> MaterialTheme.colorScheme.secondary
        },
        contentColor = when (mode) {
            TranslationMode.KJV_1611 -> MaterialTheme.colorScheme.onPrimary
            TranslationMode.KJV_STANDARD -> MaterialTheme.colorScheme.onTertiary
            TranslationMode.ESV -> MaterialTheme.colorScheme.onSecondary
        },
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = ff
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    state: StudyUiState,
    allFrontMatter: List<FrontMatterItem>,
    bookmarks: List<BookmarkItem>,
    personalNotes: List<PersonalNoteItem>,
    chapterIndex: List<ChapterIndexEntry>,
    onNavigateToRead: (String, Int, Long?) -> Unit,
    onNavigateToFrontMatter: (String) -> Unit
) {
    val textFont = getFontFamily(state.selectedFont, state.translationMode)
    val metadataFont = if (state.translationMode == TranslationMode.KJV_1611) FontFamily.Serif else textFont
    
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        val g = remember {
            when (java.util.Calendar.getInstance()[java.util.Calendar.HOUR_OF_DAY]) {
                in 0..11 -> "Good Morning"
                in 12..16 -> "Good Afternoon"
                else -> "Good Evening"
            }
        }
        Text(g, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Light)
        
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(
                modifier = Modifier.weight(1.5f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                onClick = {
                    val b = state.lastReadPosition?.first ?: "Genesis"
                    val c = state.lastReadPosition?.second ?: 1
                    onNavigateToRead(b, c, state.lastReadVerseId)
                }
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("CONTINUE READING", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
                    val ch = state.activeChapter ?: chapterIndex.find { it.book == (state.lastReadPosition?.first ?: "Genesis") && it.chapter == (state.lastReadPosition?.second ?: 1) }
                    val bookLabel = if (state.translationMode == TranslationMode.KJV_1611 && state.orthographyMode == OrthographyMode.ORIGINAL_1611) {
                        ch?.bookOriginal ?: (state.lastReadPosition?.first ?: "Genesis")
                    } else {
                        state.lastReadPosition?.first ?: "Genesis"
                    }
                    val chapterLabel = if (state.translationMode == TranslationMode.KJV_1611 && state.orthographyMode == OrthographyMode.ORIGINAL_1611) {
                        toRomanNumeral(state.lastReadPosition?.second ?: 1)
                    } else {
                        (state.lastReadPosition?.second ?: 1).toString()
                    }
                    
                    Text("$bookLabel $chapterLabel", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, fontFamily = metadataFont)
                }
            }
            
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
            ) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("STREAK", style = MaterialTheme.typography.labelSmall)
                    Text(state.streakInfo.currentStreak.toString(), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    Text("DAYS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (state.dailyReadingLesson != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.1f))
            ) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("DAILY 1611 LESSON", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                        Text(state.dailyReadingLesson, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, fontFamily = metadataFont)
                    }
                    Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.tertiary)
                }
            }
        }

        Text("1611 Exploration", style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(allFrontMatter) { item ->
                Card(onClick = { onNavigateToFrontMatter(item.docId) }, modifier = Modifier.width(220.dp)) {
                    Column(Modifier.padding(16.dp)) { 
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(12.dp))
                        Text(item.title, style = MaterialTheme.typography.titleSmall.copy(fontFamily = metadataFont), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("Historical Preface", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary) 
                    }
                }
            }
        }
        Text("Study Progress", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) { Column(Modifier.padding(16.dp)) { Text(bookmarks.size.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("Bookmarks", style = MaterialTheme.typography.labelSmall) } }
            Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) { Column(Modifier.padding(16.dp)) { Text(personalNotes.size.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("Personal Notes", style = MaterialTheme.typography.labelSmall) } }
        }
        Text("Study History", style = MaterialTheme.typography.titleMedium)
        if (state.chapterHistory.isEmpty()) {
            Text("Complete chapters to see your history.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.chapterHistory.take(5).forEach { completion ->
                    val date = java.text.DateFormat.getDateInstance().format(java.util.Date(completion.completedAtEpochMillis))
                    Card(
                        onClick = { onNavigateToRead(completion.book, completion.chapter, null) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("${completion.book} ${completion.chapter}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text(date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(64.dp))
    }
}

@Composable
private fun FrontMatterScreen(item: FrontMatterItem?, mode: OrthographyMode, fontSize: Float, font: StudyFont, translation: TranslationMode) {
    if (item == null) { Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Select a document") }; return }
    val ff = getFontFamily(font, translation)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Text(item.title, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontFamily = ff), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(32.dp))
        val t = if (mode == OrthographyMode.ORIGINAL_1611) item.textOriginal else item.textModernizedSpelling
        Text(t, style = MaterialTheme.typography.bodyLarge.copy(fontSize = fontSize.sp, lineHeight = (fontSize * 1.6).sp, fontFamily = ff))
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun StudySettingsSheet(
    state: StudyUiState,
    onQ: (String) -> Unit,
    onO: (OrthographyMode) -> Unit,
    onE: (ExplanationDepth) -> Unit,
    onSR: (Float) -> Unit,
    onV: (String) -> Unit,
    onFS: (Float) -> Unit,
    onF: (StudyFont) -> Unit,
    onSnd: (AmbientSoundscape) -> Unit,
    onTM: (StudyThemeMode) -> Unit,
    onTS: () -> Unit,
    onTOM: () -> Unit,
    onTL: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Study Preferences", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        OutlinedTextField(
            value = state.query,
            onValueChange = onQ,
            label = { Text("Search") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, null) }
        )
        OrthographyToggle(state.orthographyMode, onO)
        ExplanationDepthToggle(state.explanationDepth, onE)
        
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(12.dp))
            Text("Immersion", style = MaterialTheme.typography.titleMedium)
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Scriptorium Mode", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text("Enable ambient background sounds and candlelight flicker", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = state.isScriptoriumEnabled, onCheckedChange = { onTS() })
                }
                
                if (state.isScriptoriumEnabled) {
                    Spacer(Modifier.height(16.dp))
                    Text("Atmosphere", style = MaterialTheme.typography.labelSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        items(AmbientSoundscape.entries) { snd ->
                            FilterChip(
                                selected = state.selectedSoundscape == snd,
                                onClick = { onSnd(snd) },
                                label = { Text(snd.label) }
                            )
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Modernize 1611 Spelling", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text("Swaps u/v and i/j for readability while keeping 1611 wording.", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = state.isOrthographyModernized, onCheckedChange = { onTOM() })
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.1f))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Strong's Lexicon", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text("Enable clickable words to see Strong's definitions.", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = state.isLexiconEnabled, onCheckedChange = { onTL() })
            }
        }
        
        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Build, null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(12.dp))
            Text("Appearance", style = MaterialTheme.typography.titleMedium)
        }
        
        ThemeToggle(state.themeMode, onTM)
        FontSelection(state.selectedFont, onF)
        Column {
            Text("Font Size: ${state.fontSize.toInt()}sp", style = MaterialTheme.typography.labelSmall)
            Slider(value = state.fontSize, onValueChange = onFS, valueRange = 12f..32f)
        }
        
        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(12.dp))
            Text("Audio Settings", style = MaterialTheme.typography.titleMedium)
        }
        
        Column {
            Text("Speech Rate: ${"%.2f".format(state.speechRate)}x", style = MaterialTheme.typography.labelSmall)
            Slider(value = state.speechRate, onValueChange = onSR, valueRange = 0.5f..2.0f)
        }
        if (state.availableVoices.isNotEmpty()) {
            Text("Voice Selection", style = MaterialTheme.typography.labelSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.availableVoices) { v ->
                    FilterChip(
                        selected = state.selectedVoice == v,
                        onClick = { onV(v) },
                        label = { Text(v.substringAfter("-").take(10)) }
                    )
                }
            }
        }
        Button(onClick = onClose, modifier = Modifier.align(Alignment.End)) {
            Text("Done")
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun FontSelection(current: StudyFont, onF: (StudyFont) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Font", style = MaterialTheme.typography.labelSmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(StudyFont.entries) { f ->
                FilterChip(
                    selected = current == f,
                    onClick = { onF(f) },
                    label = { Text(f.label) }
                )
            }
        }
    }
}

@Composable private fun ThemeToggle(c: StudyThemeMode, onM: (StudyThemeMode) -> Unit) { Row(Modifier, Arrangement.spacedBy(8.dp)) { StudyThemeMode.entries.forEach { m -> FilterChip(c == m, { onM(m) }, label = { Text(m.name.lowercase().replaceFirstChar { it.uppercase() }) }) } } }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReadScreen(
    state: StudyUiState,
    chapterSummariesMap: Map<String, ChapterSummaryEntity>,
    pagingItems: LazyPagingItems<ReaderItem>,
    onVerseSelected: (Long) -> Unit,
    onVerseRangeSelected: (Long, Long, List<Long>, String) -> Unit,
    onStrongsClick: (String) -> Unit,
    onGlossaryClick: (String) -> Unit,
    onWordClick: (String?) -> Unit,
    onChapterSelected: (String, Int) -> Unit,
    onScrollHandled: () -> Unit,
    onUpdateActivePosition: (String, Int, Long?, Int?) -> Unit,
    onSaveMarginalia: (List<DrawingPath>) -> Unit
) {
    val listState = rememberLazyListState()
    
    val scriptoriumAlpha by animateFloatAsState(
        targetValue = if (state.isScriptoriumEnabled) 0.05f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "candlelight"
    )

    // Auto-scroll to highlighted verse if reading (Audio Auto-Centering)
    LaunchedEffect(state.highlightedVerseId) {
        if (state.isReadingChapter) {
            state.highlightedVerseId?.let { id ->
                for (i in 0 until pagingItems.itemCount) {
                    val itm = pagingItems[i]
                    if ((itm is ReaderItem.VerseLine) && (itm.verse.id == id)) {
                        val layoutInfo = listState.layoutInfo
                        val visibleItems = layoutInfo.visibleItemsInfo
                        val isVisible = visibleItems.any { it.index == i }
                        
                        if (!isVisible) {
                            // Center the item in the viewport
                            val viewportHeight = layoutInfo.viewportSize.height
                            listState.animateScrollToItem(i, -viewportHeight / 3)
                        }
                        break
                    }
                }
            }
        }
    }

    LaunchedEffect(state.scrollToVerseId, state.scrollToIndex, pagingItems.itemCount, pagingItems.loadState.refresh) { 
        state.scrollToIndex?.let { index ->
            if (pagingItems.itemCount > 0 && index in 0 until pagingItems.itemCount && pagingItems.loadState.refresh is LoadState.NotLoading) {
                // Wait for data and UI to settle
                kotlinx.coroutines.delay(100.milliseconds)
                listState.scrollToItem(index, 0)
                // Second pass to ensure precision after any mid-frame layout shifts
                kotlinx.coroutines.delay(50.milliseconds)
                listState.scrollToItem(index, 0)
                onScrollHandled()
            }
        } ?: state.scrollToVerseId?.let { id -> 
            if (pagingItems.itemCount > 0 && pagingItems.loadState.refresh is LoadState.NotLoading) {
                for (i in 0 until pagingItems.itemCount) {
                    val itm = pagingItems[i]
                    if ((itm is ReaderItem.VerseLine) && (itm.verse.id == id)) {
                        listState.scrollToItem(i, 0)
                        onScrollHandled()
                        break
                    }
                }
            }
        } 
    }

    LaunchedEffect(listState, pagingItems.loadState.refresh) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect { idx ->
            if (idx < pagingItems.itemCount && pagingItems.loadState.refresh is LoadState.NotLoading) {
                for (i in idx until (idx + 5).coerceAtMost(pagingItems.itemCount)) {
                    val itm = pagingItems[i]
                    if (itm is ReaderItem.VerseLine) {
                        onUpdateActivePosition(itm.verse.book, itm.verse.chapter, itm.verse.id, itm.verse.verse)
                        break
                    } else if ((itm is ReaderItem.CompositeHeader) && (itm.book != null) && (itm.chapter != null)) {
                        onUpdateActivePosition(itm.book, itm.chapter, null, null)
                        break
                    }
                }
            }
        }
    }

    Box(
        Modifier.fillMaxSize()
    ) {
        if (state.isScriptoriumEnabled) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color(0xFF2E1A05).copy(alpha = 0.2f)),
                        center = center,
                        radius = size.maxDimension * 0.7f
                    )
                )
                // Subtle flicker
                drawRect(Color(0xFFFFA500).copy(alpha = scriptoriumAlpha))
            }
        }

        SelectionContainer(modifier = Modifier.fillMaxSize()) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
                if (pagingItems.itemCount == 0) { item { Box(Modifier.fillParentMaxSize(), Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Reading Pure Words..."); if (state.importProgress < 1f) { Text("Indexing: ${(state.importProgress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall); Spacer(Modifier.height(16.dp)); LinearProgressIndicator(progress = { state.importProgress }, modifier = Modifier.width(200.dp)) } } } } }
                items(pagingItems.itemCount, key = pagingItems.itemKey { itm -> when (itm) { is ReaderItem.SectionHeader -> "s_${itm.section}"; is ReaderItem.BookHeader -> "b_${itm.book}"; is ReaderItem.ChapterHeader -> "c_${itm.book}_${itm.chapter}"; is ReaderItem.VerseLine -> "v_${itm.verse.id}"; is ReaderItem.CompositeHeader -> "ch_${itm.section}_${itm.book}_${itm.chapter}"; is ReaderItem.VerseTitle -> "vt_${itm.title}_${itm.translation}" } }, contentType = pagingItems.itemContentType { it::class.java.simpleName }) { idx ->
                    val itm = pagingItems[idx] ?: return@items
                    when (itm) {
                        is ReaderItem.CompositeHeader -> {
                            val summary = chapterSummariesMap["${itm.book}_${itm.chapter}"]
                            CompositeHeaderItem(
                                itm = itm.copy(
                                    summary1611 = summary?.summary1611,
                                    titleEsv = summary?.titleEsv,
                                    titleStandard = summary?.titleStandard,
                                    sectionTitle = summary?.sectionTitle
                                ),
                                mode = state.orthographyMode,
                                translationMode = state.translationMode,
                                font = state.selectedFont,
                                onC = onChapterSelected
                            )
                        }
                        is ReaderItem.SectionHeader -> SectionHeaderItem(itm.section)
                        is ReaderItem.BookHeader -> BookHeaderItem(itm.book, itm.bookOriginal, state.orthographyMode, state.translationMode, state.selectedFont) {}
                        is ReaderItem.ChapterHeader -> ChapterHeaderItem(itm.chapter, state.translationMode, state.selectedFont) {}
                        is ReaderItem.VerseTitle -> VerseTitleItem(itm.title, itm.translation, state.selectedFont)
                        is ReaderItem.VerseLine -> {
                            val isF = if (idx > 0) pagingItems[idx - 1] is ReaderItem.ChapterHeader || pagingItems[idx - 1] is ReaderItem.CompositeHeader else false
                            val hlColor = state.highlightsMap[itm.verse.id]
                            val onVerseSelectHandler = {
                                val selStart = state.selectedVerseId
                                if (selStart != null && selStart != itm.verse.id) {
                                    val start = minOf(selStart, itm.verse.id)
                                    val end = maxOf(selStart, itm.verse.id)
                                    val spanned = (start..end).toList()
                                    onVerseRangeSelected(start, end, spanned, "")
                                } else {
                                    onVerseSelected(itm.verse.id)
                                }
                            }
                            if (isF && state.query.isBlank()) DropCapVerseLine(itm.verse, state.orthographyMode, state.translationMode, state.highlightedVerseId == itm.verse.id, state.selectedVerseId == itm.verse.id, state.isOrthographyModernized, state.isLexiconEnabled, state.fontSize, state.selectedFont, hlColor, onVerseSelectHandler, onStrongsClick, onGlossaryClick, onWordClick)
                            else VerseTextLine(itm.verse, state.orthographyMode, state.translationMode, state.highlightedVerseId == itm.verse.id, state.selectedVerseId == itm.verse.id, state.query.isNotBlank(), state.query, state.isOrthographyModernized, state.isLexiconEnabled, state.fontSize, state.selectedFont, hlColor, onVerseSelectHandler, onStrongsClick, onGlossaryClick, onWordClick)
                        }
                    }
                }
            }
        }

        if (state.isPencilEnabled) {
            MarginaliaDrawingView(
                initialPaths = state.activeMarginalia,
                onPathsChanged = onSaveMarginalia,
                modifier = Modifier.fillMaxSize()
            )
            
            Box(Modifier.align(Alignment.BottomEnd).padding(bottom = 100.dp, end = 16.dp)) {
                SmallFloatingActionButton(onClick = { onSaveMarginalia(emptyList()) }) {
                    Icon(Icons.Default.Clear, "Clear Marginalia")
                }
            }
        }
    }
}

@Composable
private fun ParallelScreen(
    state: StudyUiState,
    pagingItems: LazyPagingItems<ReaderItem>,
    onVerseSelected: (Long) -> Unit,
    onUpdateActivePosition: (String, Int, Long?, Int?) -> Unit,
    onToggleLeft: (TranslationMode) -> Unit,
    onToggleRight: (TranslationMode) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState, pagingItems.loadState.refresh) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect { idx ->
            if (idx < pagingItems.itemCount && pagingItems.loadState.refresh is LoadState.NotLoading) {
                for (i in idx until (idx + 5).coerceAtMost(pagingItems.itemCount)) {
                    val itm = pagingItems[i]
                    if (itm is ReaderItem.VerseLine) {
                        onUpdateActivePosition(itm.verse.book, itm.verse.chapter, itm.verse.id, itm.verse.verse)
                        break
                    }
                }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Compare:", style = MaterialTheme.typography.labelLarge)
                
                FilterChip(
                    selected = state.translationMode == TranslationMode.KJV_1611 && state.comparisonTranslation == TranslationMode.KJV_STANDARD,
                    onClick = { 
                        onToggleLeft(TranslationMode.KJV_1611)
                        onToggleRight(TranslationMode.KJV_STANDARD)
                    },
                    label = { Text("1611 vs KJV") }
                )
                FilterChip(
                    selected = state.translationMode == TranslationMode.KJV_1611 && state.comparisonTranslation == TranslationMode.ESV,
                    onClick = { 
                        onToggleLeft(TranslationMode.KJV_1611)
                        onToggleRight(TranslationMode.ESV)
                    },
                    label = { Text("1611 vs ESV") }
                )
                FilterChip(
                    selected = state.translationMode == TranslationMode.KJV_STANDARD && state.comparisonTranslation == TranslationMode.ESV,
                    onClick = { 
                        onToggleLeft(TranslationMode.KJV_STANDARD)
                        onToggleRight(TranslationMode.ESV)
                    },
                    label = { Text("KJV vs ESV") }
                )
            }
        }
        
        LazyColumn(state = listState, modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp)) {
            items(pagingItems.itemCount) { idx ->
                val itm = pagingItems[idx]
                if (itm is ReaderItem.VerseLine) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onVerseSelected(itm.verse.id) }
                            .padding(vertical = 12.dp)
                    ) {
                        Text(
                            text = "${itm.verse.book} ${itm.verse.chapter}:${itm.verse.verse}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(Modifier.weight(1f)) {
                                val leftLabel = when(state.translationMode) {
                                    TranslationMode.KJV_1611 -> "1611"
                                    TranslationMode.KJV_STANDARD -> "KJV"
                                    TranslationMode.ESV -> "ESV"
                                }
                                val leftText = when(state.translationMode) {
                                    TranslationMode.KJV_1611 -> itm.verse.originalText
                                    TranslationMode.KJV_STANDARD -> itm.verse.standardText
                                    TranslationMode.ESV -> itm.verse.comparativeText
                                } ?: ""
                                
                                Text(leftLabel, style = MaterialTheme.typography.labelExtraSmall, color = MaterialTheme.colorScheme.secondary)
                                Text(
                                    text = leftText.ifBlank { "[Text not available]" },
                                    style = if (state.translationMode == TranslationMode.KJV_1611) MaterialTheme.typography.bodySmall.copy(fontFamily = KJV_FONT_FAMILY) else MaterialTheme.typography.bodySmall,
                                    color = if (leftText.isBlank()) MaterialTheme.colorScheme.error.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                val rightLabel = when(state.comparisonTranslation) {
                                    TranslationMode.KJV_1611 -> "1611"
                                    TranslationMode.KJV_STANDARD -> "KJV"
                                    TranslationMode.ESV -> "ESV"
                                }
                                val rightText = when(state.comparisonTranslation) {
                                    TranslationMode.KJV_1611 -> itm.verse.originalText
                                    TranslationMode.KJV_STANDARD -> itm.verse.standardText
                                    TranslationMode.ESV -> itm.verse.comparativeText
                                } ?: ""
                                
                                Text(rightLabel, style = MaterialTheme.typography.labelExtraSmall, color = MaterialTheme.colorScheme.tertiary)
                                Text(
                                    text = rightText.ifBlank { "[Text not available]" },
                                    style = if (state.comparisonTranslation == TranslationMode.KJV_1611) MaterialTheme.typography.bodySmall.copy(fontFamily = KJV_FONT_FAMILY) else MaterialTheme.typography.bodySmall,
                                    color = if (rightText.isBlank()) MaterialTheme.colorScheme.error.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        HorizontalDivider(Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchScreen(
    state: StudyUiState,
    pagingItems: LazyPagingItems<ReaderItem>,
    onQueryChange: (String) -> Unit,
    onSectionFilterChange: (TestamentSection?) -> Unit,
    onVerseSelected: (String, Int, Long) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            label = { Text("Search Verses") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true
        )
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.searchFilterTestament == null,
                onClick = { onSectionFilterChange(null) },
                label = { Text("All") }
            )
            TestamentSection.entries.forEach { section ->
                FilterChip(
                    selected = state.searchFilterTestament == section,
                    onClick = { onSectionFilterChange(section) },
                    label = { 
                        Text(when(section) {
                            TestamentSection.OLD_TESTAMENT -> "Old"
                            TestamentSection.APOCRYPHA -> "Apoc"
                            TestamentSection.NEW_TESTAMENT -> "New"
                        }) 
                    }
                )
            }
        }
        
        if (state.query.isBlank()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("Enter a search term or citation (e.g. Gen 1:1) to begin", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                if (state.parsedCitations.isNotEmpty()) {
                    item {
                        Text("Suggested Citations", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    items(state.parsedCitations) { verse ->
                        SearchVerseResult(
                            v = verse,
                            mode = state.orthographyMode,
                            translation = state.translationMode,
                            query = "",
                            font = state.selectedFont,
                            onClick = { onVerseSelected(verse.book, verse.chapter, verse.id) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    }
                    item {
                        Text("Text Search Results", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                    }
                }

                items(pagingItems.itemCount) { index ->
                    val item = pagingItems[index]
                    if (item is ReaderItem.VerseLine) {
                        SearchVerseResult(
                            v = item.verse,
                            mode = state.orthographyMode,
                            translation = state.translationMode,
                            query = state.query,
                            font = state.selectedFont,
                            onClick = { onVerseSelected(item.verse.book, item.verse.chapter, item.verse.id) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }
                
                if (pagingItems.itemCount == 0 && !pagingItems.loadState.append.endOfPaginationReached) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (pagingItems.itemCount == 0) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                            Text("No verses found for '${state.query}'")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchVerseResult(
    v: VerseText,
    mode: OrthographyMode,
    translation: TranslationMode,
    query: String,
    font: StudyFont,
    onClick: () -> Unit
) {
    val ff = getFontFamily(font, translation)
    val bookLabel = if (translation == TranslationMode.KJV_1611 && mode == OrthographyMode.ORIGINAL_1611) {
        v.bookOriginal ?: v.book
    } else {
        v.book
    }
    val chapterLabel = if (translation == TranslationMode.KJV_1611 && mode == OrthographyMode.ORIGINAL_1611) {
        toRomanNumeral(v.chapter)
    } else {
        v.chapter.toString()
    }
    
    val legibleFont = if (translation == TranslationMode.KJV_1611) FontFamily.Serif else ff

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Text(
            text = "$bookLabel $chapterLabel:${v.verse}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            fontFamily = legibleFont
        )
        Spacer(Modifier.height(4.dp))
        val txt = when (translation) {
            TranslationMode.ESV -> v.comparativeText ?: v.modernizedText
            TranslationMode.KJV_STANDARD -> v.standardText ?: v.modernizedText
            TranslationMode.KJV_1611 -> if (mode == OrthographyMode.ORIGINAL_1611) v.originalText else v.modernizedText
        }
        
        val snippet = if (query.startsWith("\"") && query.endsWith("\"") && query.length > 2) {
            val phrase = query.substring(1, query.length - 1)
            val index = txt.indexOf(phrase, ignoreCase = true)
            if (index > 40) {
                "..." + txt.substring(index - 20).take(150) + "..."
            } else {
                txt.take(150) + if (txt.length > 150) "..." else ""
            }
        } else {
            txt
        }

        Text(
            text = highlightSearchQuery(snippet, query),
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = ff),
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CompositeHeaderItem(itm: ReaderItem.CompositeHeader, mode: OrthographyMode, translationMode: TranslationMode, font: StudyFont, onC: (String, Int) -> Unit) {
    val ff = getFontFamily(font, translationMode)
    val legibleFont = if (translationMode == TranslationMode.KJV_1611) FontFamily.Serif else ff
    
    Column(Modifier.fillMaxWidth().padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        itm.section?.let { SectionHeaderItem(it) }

        if (itm.showBookHeader && itm.sectionTitle != null && translationMode == TranslationMode.KJV_1611) {
            Text(
                text = itm.sectionTitle,
                style = MaterialTheme.typography.headlineLarge.copy(
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = legibleFont,
                    fontSize = 32.sp,
                    lineHeight = 40.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp).padding(top = 48.dp, bottom = 16.dp)
            )
            HorizontalDivider(Modifier.width(100.dp), thickness = 1.dp)
        }
        
        if (itm.showBookHeader && itm.book != null) {
            val t = if (translationMode == TranslationMode.KJV_1611 && mode == OrthographyMode.ORIGINAL_1611) {
                itm.bookOriginal ?: itm.book
            } else {
                itm.book
            }
            
            val shouldShowShortTitle = itm.sectionTitle == null || translationMode != TranslationMode.KJV_1611
            
            if (shouldShowShortTitle) {
                Text(
                    text = t, 
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Bold, 
                        fontSize = legibilityAdjustedFontSize(64, translationMode), 
                        lineHeight = 72.sp, 
                        fontFamily = legibleFont,
                        letterSpacing = (-1).sp
                    ), 
                    textAlign = TextAlign.Center, 
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 64.dp)
                )
            } else {
                Spacer(Modifier.height(32.dp))
            }
        }

        if (itm.chapter != null && itm.book != null) {
            val d = if (translationMode == TranslationMode.KJV_1611 && mode == OrthographyMode.ORIGINAL_1611) {
                "CHAP. ${toRomanNumeral(itm.chapter)}."
            } else {
                "Chapter ${itm.chapter}"
            }
            val title = when (translationMode) {
                TranslationMode.ESV -> itm.titleEsv
                TranslationMode.KJV_STANDARD -> itm.titleStandard
                else -> null
            }
            
            Column(Modifier.fillMaxWidth().clickable { onC(itm.book, itm.chapter) }.padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) { 
                HorizontalDivider(Modifier.width(48.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = d, 
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontStyle = if (translationMode == TranslationMode.KJV_1611) FontStyle.Normal else FontStyle.Italic, 
                            letterSpacing = 1.sp, 
                            fontFamily = legibleFont,
                            fontWeight = if (translationMode == TranslationMode.KJV_1611) FontWeight.Bold else FontWeight.Normal
                        ), 
                        modifier = Modifier.padding(top = 16.dp), 
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (title != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontFamily = legibleFont), modifier = Modifier.padding(top = 16.dp), color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                
                if (translationMode == TranslationMode.KJV_1611 && itm.summary1611 != null) {
                    Text(
                        text = itm.summary1611,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontStyle = FontStyle.Italic, 
                            fontFamily = legibleFont,
                            lineHeight = 24.sp
                        ),
                        modifier = Modifier.padding(horizontal = 48.dp, vertical = 12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun legibilityAdjustedFontSize(base: Int, mode: TranslationMode): TextUnit {
    return if (mode == TranslationMode.KJV_1611) (base.toFloat() * 1.3f).sp else base.sp
}

@Composable
private fun VerseTitleItem(title: String, translation: TranslationMode, font: StudyFont) {
    val ff = getFontFamily(font, translation)
    val legibleFont = if (translation == TranslationMode.KJV_1611) FontFamily.Serif else ff
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = legibleFont
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable private fun SectionHeaderItem(s: TestamentSection) { 
    val t = when (s) { 
        TestamentSection.OLD_TESTAMENT -> "THE OLD TESTAMENT"
        TestamentSection.APOCRYPHA -> "THE APOCRYPHA"
        TestamentSection.NEW_TESTAMENT -> "THE NEW TESTAMENT" 
    }; 
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 80.dp, bottom = 48.dp), 
        horizontalAlignment = Alignment.CenterHorizontally
    ) { 
        HorizontalDivider(Modifier.width(120.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        Spacer(Modifier.height(32.dp))
        Text(
            text = t, 
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.ExtraLight, 
                letterSpacing = 8.sp, 
                fontSize = 24.sp,
                fontFamily = FontFamily.Serif 
            ), 
            color = MaterialTheme.colorScheme.onSurface, 
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        HorizontalDivider(Modifier.width(120.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) 
    } 
}
@Composable private fun BookHeaderItem(b: String, bo: String?, m: OrthographyMode, t: TranslationMode, font: StudyFont, onClick: () -> Unit) { 
    val d = if (t == TranslationMode.KJV_1611 && m == OrthographyMode.ORIGINAL_1611) bo ?: b else b; 
    val ff = getFontFamily(font, t)
    val bookFont = if (t == TranslationMode.KJV_1611) FontFamily.Serif else ff
    Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), color = Color.Transparent) { 
        Text(d, style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, fontSize = 48.sp, fontFamily = bookFont), modifier = Modifier.padding(horizontal = 24.dp, vertical = 48.dp), textAlign = TextAlign.Center) 
    } 
}
@Composable private fun ChapterHeaderItem(c: Int, t: TranslationMode, font: StudyFont, onClick: () -> Unit) { 
    val d = if (t == TranslationMode.KJV_1611) "CHAP. ${toRomanNumeral(c)}." else "Chapter $c"; 
    val ff = getFontFamily(font, t)
    val legibleFont = if (t == TranslationMode.KJV_1611) FontFamily.Serif else ff
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) { 
        HorizontalDivider(Modifier.width(48.dp)); 
        Text(d, style = MaterialTheme.typography.titleLarge.copy(fontStyle = FontStyle.Italic, letterSpacing = 2.sp, fontFamily = legibleFont), modifier = Modifier.padding(top = 16.dp), color = MaterialTheme.colorScheme.onSurface) 
    } 
}
@Composable private fun VerseTextLine(v: VerseText, m: OrthographyMode, t: TranslationMode, iH: Boolean, iS: Boolean, iSM: Boolean, q: String, isOM: Boolean, isL: Boolean, fs: Float, f: StudyFont, hlColor: String? = null, onClick: () -> Unit, onS: (String) -> Unit, onG: (String) -> Unit, onW: (String?) -> Unit) { 
    val rowBg = if (hlColor != null) getHighlightColor(hlColor).copy(alpha = 0.35f) else if (iH) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else if (iS) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f) else Color.Transparent
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).background(rowBg).padding(horizontal = 16.dp, vertical = 8.dp)) { 
        Text(v.verse.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(24.dp).padding(top = 4.dp))
        Column(Modifier.weight(1f)) { 
            if (iSM) {
                val b = if (t == TranslationMode.KJV_1611 && m == OrthographyMode.ORIGINAL_1611) v.bookOriginal ?: v.book else v.book
                val c = if (t == TranslationMode.KJV_1611 && m == OrthographyMode.ORIGINAL_1611) toRomanNumeral(v.chapter) else v.chapter.toString()
                val legibleFont = if (t == TranslationMode.KJV_1611) FontFamily.Serif else getFontFamily(f, t)
                Text("$b $c", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(bottom = 2.dp), fontFamily = legibleFont)
            }
            
            val annotatedTxt = if (isL && v.strongsText != null && t == TranslationMode.KJV_1611 && m == OrthographyMode.ORIGINAL_1611) {
                parseStrongsText(v.strongsText, showTags = false)
            } else {
                val txt = when (t) {
                    TranslationMode.ESV -> v.comparativeText ?: v.modernizedText
                    TranslationMode.KJV_STANDARD -> v.standardText ?: v.modernizedText
                    TranslationMode.KJV_1611 -> {
                        val base = if (m == OrthographyMode.ORIGINAL_1611) v.originalText else v.modernizedText
                        if (isOM && m == OrthographyMode.ORIGINAL_1611) OrthographyUtils.modernize(base) else base
                    }
                }
                if (t == TranslationMode.KJV_1611 && m == OrthographyMode.ORIGINAL_1611 && q.isBlank()) {
                    FalseFriendGlossary.highlightFalseFriends(txt)
                } else {
                    highlightSearchQuery(txt, q)
                }
            }
            
            ClickableText(
                text = annotatedTxt,
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = (fs * 1.5).sp,
                    fontSize = fs.sp,
                    fontFamily = getFontFamily(f, t),
                    color = MaterialTheme.colorScheme.onSurface
                ),
                onClick = { offset ->
                    val strongs = annotatedTxt.getStringAnnotations(tag = "STRONGS", start = offset, end = offset).firstOrNull()
                    val glossary = annotatedTxt.getStringAnnotations(tag = "GLOSSARY", start = offset, end = offset).firstOrNull()
                    if (strongs != null) {
                        onS(strongs.item)
                    } else if (glossary != null) {
                        onG(glossary.item)
                    } else {
                        val word = extractWordAtOffset(annotatedTxt.text, offset)
                        if (word.isNotBlank()) onW(word)
                        onClick()
                    }
                }
            )
        } 
    } 
}
@Composable private fun DropCapVerseLine(v: VerseText, m: OrthographyMode, t: TranslationMode, iH: Boolean, iS: Boolean, isOM: Boolean, isL: Boolean, fs: Float, f: StudyFont, hlColor: String? = null, onClick: () -> Unit, onS: (String) -> Unit, onG: (String) -> Unit, onW: (String?) -> Unit) { 
    val rowBg = if (hlColor != null) getHighlightColor(hlColor).copy(alpha = 0.35f) else if (iH) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else if (iS) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f) else Color.Transparent
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).background(rowBg).padding(horizontal = 16.dp, vertical = 12.dp)) { 
        Text(v.verse.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(24.dp).padding(top = 8.dp))
        
        val annotatedTxt = if (isL && v.strongsText != null && t == TranslationMode.KJV_1611 && m == OrthographyMode.ORIGINAL_1611) {
            parseStrongsText(v.strongsText, showTags = false)
        } else {
            val txt = when (t) {
                TranslationMode.ESV -> v.comparativeText ?: v.modernizedText
                TranslationMode.KJV_STANDARD -> v.standardText ?: v.modernizedText
                TranslationMode.KJV_1611 -> {
                    val base = if (m == OrthographyMode.ORIGINAL_1611) v.originalText else v.modernizedText
                    if (isOM && m == OrthographyMode.ORIGINAL_1611) OrthographyUtils.modernize(base) else base
                }
            }
            if (t == TranslationMode.KJV_1611 && m == OrthographyMode.ORIGINAL_1611) {
                FalseFriendGlossary.highlightFalseFriends(txt)
            } else {
                parseItalicText(txt)
            }
        }
        
        if (annotatedTxt.isNotEmpty()) { 
            val dc = annotatedTxt.text.take(1); val rem = annotatedTxt.subSequence(1, annotatedTxt.length)
            Row(Modifier.weight(1f)) { 
                Text(dc, style = MaterialTheme.typography.displayLarge.copy(fontSize = (fs * 2.5).sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontFamily = getFontFamily(f, t)), modifier = Modifier.padding(end = 4.dp))
                ClickableText(
                    text = rem,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = (fs * 1.5).sp,
                        fontSize = fs.sp,
                        fontFamily = getFontFamily(f, t),
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.padding(top = 8.dp),
                    onClick = { offset ->
                        val strongs = rem.getStringAnnotations(tag = "STRONGS", start = offset, end = offset).firstOrNull()
                        val glossary = rem.getStringAnnotations(tag = "GLOSSARY", start = offset, end = offset).firstOrNull()
                        if (strongs != null) {
                            onS(strongs.item)
                        } else if (glossary != null) {
                            onG(glossary.item)
                        } else {
                            val word = extractWordAtOffset(rem.text, offset)
                            if (word.isNotBlank()) onW(word)
                            onClick()
                        }
                    }
                )
            } 
        } 
    } 
}

private val KJV_FONT_FAMILY = FontFamily(Font(R.font.kjva6aa))

private fun getFontFamily(font: StudyFont, translation: TranslationMode): FontFamily {
    return when (font) {
        StudyFont.SYSTEM -> {
            if (translation == TranslationMode.KJV_1611) {
                KJV_FONT_FAMILY
            } else {
                FontFamily.Default
            }
        }
        StudyFont.SERIF -> FontFamily.Serif
        StudyFont.SANS_SERIF -> FontFamily.SansSerif
        StudyFont.MONOSPACE -> FontFamily.Monospace
        StudyFont.BLACKLETTER -> KJV_FONT_FAMILY
    }
}

private fun toRomanNumeral(n: Int): String { val r = listOf(1000 to "M", 900 to "CM", 500 to "D", 400 to "CD", 100 to "C", 90 to "XC", 50 to "L", 40 to "XL", 10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I"); var v = n; val sb = StringBuilder(); r.forEach { (valAt, num) -> while (v >= valAt) { sb.append(num); v -= valAt } }; return sb.toString() }
private fun parseItalicText(t: String): AnnotatedString = buildAnnotatedString { t.split("_").forEachIndexed { i, p -> if (i % 2 == 1) withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(p) } else append(p) } }
private fun highlightSearchQuery(t: String, q: String): AnnotatedString {
    if (q.isBlank()) return parseItalicText(t)
    val base = parseItalicText(t)
    return buildAnnotatedString {
        append(base)
        val lowerText = base.text.lowercase(Locale.ROOT)
        val trimmedQuery = q.trim()
        
        val terms = if (trimmedQuery.startsWith("\"") && trimmedQuery.endsWith("\"") && trimmedQuery.length > 2) {
            listOf(trimmedQuery.substring(1, trimmedQuery.length - 1).lowercase(Locale.ROOT))
        } else {
            q.lowercase(Locale.ROOT).split("\\s+".toRegex()).filter { it.length > 2 }
        }
        
        terms.forEach { term ->
            var start = lowerText.indexOf(term)
            while (start >= 0) {
                addStyle(
                    SpanStyle(
                        background = Color.Yellow.copy(alpha = 0.3f),
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    ),
                    start,
                    start + term.length
                )
                start = lowerText.indexOf(term, start + term.length)
            }
        }
    }
}

@Composable
private fun StudyHubSheet(
    s: StudyUiState,
    onSp: () -> Unit,
    onR: () -> Unit,
    onRes: () -> Unit,
    onTPA: () -> Unit,
    onB: () -> Unit,
    onH: (String) -> Unit,
    onN: (String, String?) -> Unit,
    onExport: () -> Unit,
    onSh: (String) -> Unit,
    onCl: () -> Unit
) {
    val ff = getFontFamily(s.selectedFont, s.translationMode)
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Study Hub", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Verse ID: ${s.selectedVerseId}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onCl) {
                Icon(Icons.Default.Close, null)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = onSp, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(4.dp))
                Text("Listen")
            }
            Button(onClick = onR, modifier = Modifier.weight(1f)) {
                Text("Read from here")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onRes, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(4.dp))
                Text("Restart Chapter")
            }
            OutlinedButton(onClick = onTPA, modifier = Modifier.weight(1f)) {
                val label = if (s.isParallelAudioEnabled) "Disable Sequential Audio" else "Enable Sequential Audio (1611 then Modern)"
                Text(label, textAlign = TextAlign.Center)
            }
        }
        // Selection Toolbar & BibleGateway Swatches
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.medium
                )
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isStarred = s.selectedVerseId?.let { s.bookmarkedVerseIdsSet.contains(it) } == true
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onB) {
                    Icon(
                        imageVector = if (isStarred) androidx.compose.material.icons.Icons.Default.Star else androidx.compose.material.icons.Icons.Default.Star,
                        contentDescription = "Favorite",
                        tint = if (isStarred) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text("Highlight:", style = MaterialTheme.typography.labelMedium)
                val currentHighlight = s.selectedVerseId?.let { s.highlightsMap[it] }
                listOf(
                    "Yellow" to Color(0xFFFFF59D),
                    "Red" to Color(0xFFEF9A9A),
                    "Blue" to Color(0xFF90CAF9),
                    "Green" to Color(0xFFA5D6A7)
                ).forEach { (colorName, color) ->
                    val isSelected = currentHighlight == colorName
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                            .clickable {
                                onH(colorName)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            AssistChip(onClick = onB, label = { Text(if (s.selectedVerseId?.let { s.bookmarkedVerseIdsSet.contains(it) } == true) "Starred ★" else "Star ★") })
            AssistChip(
                onClick = {
                    val translationName = when(s.translationMode) {
                        TranslationMode.KJV_1611 -> "KJV 1611"
                        TranslationMode.KJV_STANDARD -> "Standard KJV"
                        TranslationMode.ESV -> "English Standard Version (ESV)"
                    }
                    val citation = if (s.translationMode == TranslationMode.KJV_1611) {
                        "${s.activeChapter?.bookOriginal ?: s.activeChapter?.book}, Chap. ${toRomanNumeral(s.activeChapter?.chapter ?: 1)}"
                    } else {
                        "${s.activeChapter?.book} ${s.activeChapter?.chapter}"
                    }
                    VerseShareImageGenerator.generateAndShareVerseImage(
                        context,
                        s.selectedVerseDisplayText,
                        citation,
                        translationName
                    )
                },
                label = { Text("Share Image Card") },
                leadingIcon = { Icon(Icons.Default.Share, null, Modifier.size(16.dp)) }
            )
            if (s.selectedPhrase != null) {
                AssistChip(
                    onClick = {
                        val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Selected Phrase", s.selectedPhrase)
                        clipboardManager.setPrimaryClip(clip)
                    },
                    label = { Text("Copy Phrase") },
                    leadingIcon = { Icon(Icons.Default.Add, null, Modifier.size(16.dp)) }
                )
            }
            AssistChip(
                onClick = {
                    val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Verse", s.selectedVerseDisplayText)
                    clipboardManager.setPrimaryClip(clip)
                },
                label = { Text("Copy Verse") },
                leadingIcon = { Icon(Icons.Default.Add, null, Modifier.size(16.dp)) }
            )
            AssistChip(
                onClick = onExport,
                label = { Text("Export PDF") },
                leadingIcon = { Icon(Icons.Default.Share, null, Modifier.size(16.dp)) }
            )
            AssistChip(
                onClick = { 
                    val translationName = when(s.translationMode) {
                        TranslationMode.KJV_1611 -> "KJV 1611"
                        TranslationMode.KJV_STANDARD -> "Standard KJV"
                        TranslationMode.ESV -> "English Standard Version (ESV)"
                    }
                    val citation = if (s.translationMode == TranslationMode.KJV_1611) {
                        "${s.activeChapter?.bookOriginal ?: s.activeChapter?.book}, Chap. ${toRomanNumeral(s.activeChapter?.chapter ?: 1)}"
                    } else {
                        "${s.activeChapter?.book} ${s.activeChapter?.chapter}"
                    }
                    val text = "📜 \"${s.selectedVerseDisplayText}\"\n— $citation ($translationName)\n\nStudy the Pure Words 1611 Bible App."
                    onSh(text) 
                },
                label = { Text("Share Text") },
                leadingIcon = { Icon(Icons.Default.Share, null, Modifier.size(16.dp)) }
            )
        }
        var noteText by remember { mutableStateOf("") }
        var selectedTag by remember { mutableStateOf<String?>(null) }
        val tags = listOf("Personal", "Theological", "Historical", "Prophetic")
        
        OutlinedTextField(
            value = noteText,
            onValueChange = { noteText = it },
            label = { Text("Personal Note") },
            modifier = Modifier.fillMaxWidth()
        )
        
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
            items(tags) { tag ->
                FilterChip(
                    selected = selectedTag == tag,
                    onClick = { selectedTag = if (selectedTag == tag) null else tag },
                    label = { Text(tag) }
                )
            }
        }
        
        Button(
            onClick = {
                onN(noteText, selectedTag)
                noteText = ""
                selectedTag = null
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Save Note")
        }
        
        if (s.selectedVerseGlossary.isNotEmpty()) {
            HorizontalDivider()
            Text("Glossary (False Friends)", style = MaterialTheme.typography.titleMedium)
            s.selectedVerseGlossary.forEach { (word, definition) ->
                Column(Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = word.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF673AB7),
                        fontWeight = FontWeight.Bold,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                    )
                    Text(
                        text = definition,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = ff
                    )
                }
            }
        }

        HorizontalDivider()
        Text("Word Study (Strong's)", style = MaterialTheme.typography.titleMedium)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.1f))
        ) {
            Text(
                text = if (s.isLexiconEnabled) "Tap words in the reader to see Strong's definitions and search for occurrences." else "Enable Strong's Lexicon in settings to see Greek/Hebrew root meanings and lexical data.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }

        HorizontalDivider()
        Text("Cross References", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "No related verses found in existing dataset.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )

        HorizontalDivider()
        Text("Marginal Notes", style = MaterialTheme.typography.titleMedium)
        if (s.selectedVerseNotes.isEmpty()) {
            Text("No notes.", style = MaterialTheme.typography.bodyMedium)
        } else {
            s.selectedVerseNotes.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, fontFamily = ff) }
        }
        HorizontalDivider()
        Text("Explanations", style = MaterialTheme.typography.titleMedium)
        if (s.selectedVerseExplanations.isEmpty()) {
            Text("No explanations.", style = MaterialTheme.typography.bodyMedium)
        } else {
            s.selectedVerseExplanations.forEach {
                Text(it.contentMarkdown, style = MaterialTheme.typography.bodyMedium, fontFamily = ff)
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ExplanationDepthToggle(depth: ExplanationDepth, onD: (ExplanationDepth) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Explanation Depth", style = MaterialTheme.typography.labelLarge)
        ExplanationDepth.entries.forEach { d ->
            FilterChip(
                selected = depth == d,
                onClick = { onD(d) },
                label = { Text(d.name.lowercase().replaceFirstChar { it.uppercase() }.replace("_", " ")) }
            )
        }
    }
}

@Composable
private fun OrthographyToggle(mode: OrthographyMode, onM: (OrthographyMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Orthography", style = MaterialTheme.typography.labelLarge)
        OrthographyMode.entries.forEach { m ->
            FilterChip(
                selected = mode == m,
                onClick = { onM(m) },
                label = { Text(m.name.lowercase().replaceFirstChar { it.uppercase() }) }
            )
        }
    }
}

@Composable
private fun NotesScreen(
    s: StudyUiState,
    bookmarks: List<BookmarkItem>,
    personalNotes: List<PersonalNoteItem>,
    highlights: List<HighlightItem>,
    onB: () -> Unit,
    onH: (String) -> Unit,
    onN: (String) -> Unit
) {
    val ff = getFontFamily(s.selectedFont, s.translationMode)
    var nD by rememberSaveable { mutableStateOf(value = "") }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Study Dashboard", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(Modifier.weight(1f)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(bookmarks.size.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Bookmarks", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Card(Modifier.weight(1f)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(highlights.size.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Highlights", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        item {
            Text("Active Study", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            Text("Selected verse: ${s.selectedVerseId ?: "None"}", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onB) { Text("Bookmark") }
                TextButton(onClick = { onH("yellow") }) { Text("Highlight") }
            }
            OutlinedTextField(
                value = nD,
                onValueChange = { nD = it },
                label = { Text("Quick Note") },
                modifier = Modifier.fillMaxWidth()
            )
            TextButton(onClick = { onN(nD); nD = "" }) { Text("Save Note") }
        }
        item { Text("Bookmarks", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
        items(bookmarks) { bookmark ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.small
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Favorite, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Verse ID ${bookmark.verseId}", style = MaterialTheme.typography.bodyMedium, fontFamily = ff)
                }
            }
        }
        item { Text("Highlights", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
        items(highlights) { highlight ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.small
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).background(Color.Yellow, CircleShape).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text("Verse ID ${highlight.verseId}", style = MaterialTheme.typography.bodyMedium, fontFamily = ff)
                }
            }
        }
        item { Text("Personal Notes", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
        items(personalNotes) { note -> 
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Verse ID: ${note.verseId}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        note.category?.let { 
                            Text(
                                it, 
                                style = MaterialTheme.typography.labelExtraSmall.copy(fontWeight = FontWeight.Bold), 
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), MaterialTheme.shapes.extraSmall).padding(horizontal = 4.dp, vertical = 2.dp)
                            ) 
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(note.note, style = MaterialTheme.typography.bodyMedium, fontFamily = ff)
                    Text(
                        text = java.text.DateFormat.getDateTimeInstance().format(java.util.Date(note.updatedAtEpochMillis)),
                        style = MaterialTheme.typography.labelExtraSmall,
                        modifier = Modifier.align(Alignment.End),
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun SeekerPathScreen(ts: List<SeekerTrackEntry>, aId: String?, ss: List<SeekerStepEntry>, onS: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Seeker Path", style = MaterialTheme.typography.headlineSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ts) { t ->
                    FilterChip(
                        selected = aId == t.trackId,
                        onClick = { onS(t.trackId) },
                        label = { Text(t.title) }
                    )
                }
            }
            ts.find { it.trackId == aId }?.let {
                Text(it.description, style = MaterialTheme.typography.bodyMedium)
            }
        }
        items(ss) { s ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(32.dp)) {
                    Surface(
                        modifier = Modifier.size(24.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        tonalElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(s.sequence.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    Box(modifier = Modifier.width(2.dp).height(80.dp).background(MaterialTheme.colorScheme.outlineVariant))
                }
                
                Spacer(Modifier.width(12.dp))
                
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(s.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(s.bodyMarkdown, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    androidx.compose.material3.TextButton(onClick = onClick, modifier = modifier, content = { content() })
}

private val Typography.labelExtraSmall: TextStyle
    get() = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.5.sp
    )

@Composable
private fun GalleryScreen(metadataFont: FontFamily, onSelect: (String) -> Unit) {
    val visuals = listOf(
        VisualModule("genealogies", "Genealogy Charts", "The Genealogies of the Holy Scriptures from the original 1611 edition.", Icons.AutoMirrored.Filled.List),
        VisualModule("maps", "Holy Land Maps", "Original maps of the biblical world as presented in the 1611 Authorized Version.", Icons.Default.LocationOn),
        VisualModule("woodcuts", "Ornate Woodcuts", "A collection of the detailed woodcut initials and decorative headers used in 1611.", Icons.Default.Star)
    )

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            "1611 Visual Gallery",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            fontFamily = metadataFont,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Explore the visual heritage of the original 1611 Authorized Version.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(32.dp))
        
        visuals.forEach { visual ->
            Card(
                onClick = { onSelect(visual.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                visual.icon,
                                null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            visual.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            fontFamily = metadataFont
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            visual.description,
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 16.sp
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                }
            }
        }
        
        Spacer(Modifier.height(32.dp))
        Text(
            "Historical Context",
            style = MaterialTheme.typography.titleMedium,
            fontFamily = metadataFont,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(12.dp))
        WoodcutExplainer()
        Spacer(Modifier.height(64.dp))
    }
}

data class VisualModule(val id: String, val title: String, val description: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
private fun WoodcutExplainer() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "The 1611 edition was famous for its ornate Drop Cap initials (woodcuts). Each major book and section began with a unique, hand-carved letter, often containing biblical motifs or royal symbols.",
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "In this app, we use 'Drop Caps' at the beginning of each chapter to honor this tradition while maintaining readability.",
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VisualDetailScreen(
    id: String,
    metadataFont: FontFamily,
    showMetadata: Boolean,
    onToggleMetadata: () -> Unit,
    onBack: () -> Unit
) {
    val title = when(id) {
        "genealogies" -> "Genealogy Charts"
        "maps" -> "Holy Land Maps"
        "woodcuts" -> "Ornate Woodcuts"
        else -> "Visual Detail"
    }
    
    val description = when(id) {
        "genealogies" -> "Speed's 'Genealogies of the Holy Scriptures' were included in the first 1611 edition to help readers trace the lineage from Adam to Christ."
        "maps" -> "Early 17th-century cartography of the Holy Land, providing a spatial context for the biblical narrative."
        "woodcuts" -> "A study of the intricate typography and decorative woodcut illustrations that adorned the original Barker printing."
        else -> ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontFamily = metadataFont) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                actions = {
                    IconButton(onClick = onToggleMetadata) {
                        Icon(if (showMetadata) Icons.Default.Info else Icons.Default.Info, null, tint = if (showMetadata) MaterialTheme.colorScheme.primary else LocalContentColor.current)
                    }
                }
            )
        }
    ) { p ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(p)
                .background(Color.Black)
        ) {
            ZoomableImage(
                resourceId = R.drawable.bible_title_1611,
                contentDescription = title
            )

            if (showMetadata) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 8.dp
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Historical Significance",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(description, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun ZoomableImage(
    resourceId: Int,
    contentDescription: String
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(androidx.compose.ui.graphics.RectangleShape)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    val maxX = (size.width * (scale - 1)) / 2
                    val maxY = (size.height * (scale - 1)) / 2
                    offset = Offset(
                        x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                        y = (offset.y + pan.y).coerceIn(-maxY, maxY)
                    )
                }
            }
    ) {
        Image(
            painter = painterResource(id = resourceId),
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                ),
            contentScale = ContentScale.Fit
        )
        
        if (scale > 1f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        scale = 1f
                        offset = Offset.Zero
                    },
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text("Reset Zoom", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

private fun parseStrongsText(t: String, showTags: Boolean = false): AnnotatedString = buildAnnotatedString {
    val regex = Regex("""([^{}]+)|\{(\(?[HG]\d+\)?)\}""")
    var lastWordStart = -1
    var lastWordEnd = -1

    regex.findAll(t).forEach { match ->
        val textPart = match.groups[1]?.value
        val tagPart = match.groups[2]?.value

        if (textPart != null) {
            val start = length
            append(textPart)
            
            val trimmed = textPart.trimEnd()
            if (trimmed.isNotEmpty()) {
                val lastSpace = trimmed.lastIndexOf(' ')
                lastWordStart = start + (if (lastSpace == -1) 0 else lastSpace + 1)
                lastWordEnd = start + trimmed.length
            }
        } else if (tagPart != null && lastWordStart != -1) {
            val cleanTag = tagPart.replace("(", "").replace(")", "")
            addStringAnnotation(
                tag = "STRONGS",
                annotation = cleanTag,
                start = lastWordStart,
                end = lastWordEnd
            )
            addStyle(
                SpanStyle(
                    textDecoration = TextDecoration.Underline,
                    color = Color.Unspecified,
                    background = Color.Transparent
                ),
                lastWordStart,
                lastWordEnd
            )
            if (showTags) {
                withStyle(SpanStyle(fontSize = 10.sp, baselineShift = BaselineShift.Superscript, color = Color.Gray)) {
                    append(tagPart)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LexiconEntrySheet(
    entry: LexiconEntity?,
    occurrenceCount: Int,
    onSearch: (String) -> Unit,
    onClose: () -> Unit
) {
    if (entry == null) return
    ModalBottomSheet(onDismissRequest = onClose) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = entry.word.ifEmpty { entry.strongsId },
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Serif
                )
                if (entry.transliteration?.isNotEmpty() == true) {
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "[${entry.transliteration}]",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            if (entry.pronunciation?.isNotEmpty() == true) {
                Text(
                    text = "Pronunciation: ${entry.pronunciation}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.strongsId,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary
                )
                
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = CircleShape
                ) {
                    Text(
                        text = if (occurrenceCount > 0) "$occurrenceCount occurrences" else "Loading count...",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            HorizontalDivider()
            
            Text(
                text = entry.definition.ifEmpty { "No definition available." },
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp
            )
            
            if (entry.info?.isNotEmpty() == true) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = entry.info,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            
            Button(
                onClick = { 
                    onSearch(entry.strongsId)
                    onClose()
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Icon(Icons.Default.Search, null)
                Spacer(Modifier.width(8.dp))
                Text("Search all occurrences in Bible")
            }
            
            Spacer(Modifier.height(32.dp))
        }
    }
}

private fun extractWordAtOffset(text: String, offset: Int): String {
    if (offset < 0 || offset >= text.length) return ""
    var start = offset
    while (start > 0 && text[start - 1].isLetterOrDigit()) start--
    var end = offset
    while (end < text.length && text[end].isLetterOrDigit()) end++
    return text.substring(start, end)
}

@Composable
private fun FacsimileOverlay(state: StudyUiState, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .clickable(onClick = onDismiss, indication = null, interactionSource = remember { MutableInteractionSource() })
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "1611 Facsimile Flashback",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }
            }
            
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                ZoomableImage(
                    resourceId = R.drawable.bible_title_1611,
                    contentDescription = "1611 Facsimile"
                )
                
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(24.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = CircleShape
                ) {
                    Text(
                        "${state.activeChapter?.book} ${state.activeChapter?.chapter}",
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            
            Text(
                "Pinch to zoom. Tap background to return to Study Mode.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = TextAlign.Center,
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
