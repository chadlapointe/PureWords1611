package com.purewords1611.android.study.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.purewords1611.android.study.data.ChapterIndexEntry
import com.purewords1611.android.study.data.OrthographyMode
import com.purewords1611.android.study.data.TestamentSection
import com.purewords1611.android.study.data.TranslationMode
import com.purewords1611.android.study.data.StudyFont
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.purewords1611.android.R
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.rememberLazyListState

@Composable
fun BibleSelectorDialog(
    chapters: List<ChapterIndexEntry>,
    completedChapters: Set<Pair<String, Int>>,
    orthographyMode: OrthographyMode,
    translationMode: TranslationMode,
    selectedFont: StudyFont,
    initialBook: String? = null,
    onChapterSelected: (String, Int, Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    val ff = getFontFamily(selectedFont, translationMode)
    val legibleFont = if (translationMode == TranslationMode.KJV_1611) FontFamily.Serif else ff
    
    var selectedSection by remember { 
        mutableStateOf(
            chapters.find { it.book == initialBook }?.section ?: TestamentSection.OLD_TESTAMENT
        ) 
    }
    
    val filteredBooks = remember(selectedSection, chapters) {
        chapters.asSequence()
            .filter { it.section == selectedSection }
            .map { it.book }
            .distinct()
            .toList()
    }
    
    var selectedBookState by remember(selectedSection) { 
        mutableStateOf(initialBook?.takeIf { b -> chapters.any { it.book == b && it.section == selectedSection } }) 
    }
    val selectedBook = selectedBookState ?: filteredBooks.firstOrNull()
    
    var selectedChapter by remember(selectedBook) {
        mutableStateOf(chapters.find { it.book == selectedBook }?.chapter ?: 1)
    }

    val bookListState = rememberLazyListState()
    
    LaunchedEffect(selectedBook) {
        val index = filteredBooks.indexOf(selectedBook)
        if (index >= 0) {
            bookListState.animateScrollToItem(index)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column {
                val sections = TestamentSection.entries
                TabRow(
                    selectedTabIndex = selectedSection.ordinal,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                ) {
                    sections.forEach { section ->
                        Tab(
                            selected = selectedSection == section,
                            onClick = { selectedSection = section },
                            text = { 
                                Text(
                                    text = when(section) {
                                        TestamentSection.OLD_TESTAMENT -> "Old"
                                        TestamentSection.APOCRYPHA -> "Apoc"
                                        TestamentSection.NEW_TESTAMENT -> "New"
                                    },
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = legibleFont
                                    ),
                                )
                            }
                        )
                    }
                }

                Row(modifier = Modifier.weight(1f).padding(4.dp)) {
                    // Books Column
                    LazyColumn(
                        modifier = Modifier.weight(2.5f),
                        state = bookListState
                    ) {
                        items(filteredBooks) { book ->
                            val bookOriginal = chapters.firstOrNull { it.book == book }?.bookOriginal
                            val bookName = if (translationMode == TranslationMode.KJV_1611 && orthographyMode == OrthographyMode.ORIGINAL_1611) {
                                bookOriginal ?: book
                            } else {
                                book
                            }
                            Surface(
                                color = if (selectedBook == book) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = bookName,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily = legibleFont,
                                        fontWeight = if (selectedBook == book) FontWeight.Bold else FontWeight.Normal,
                                        letterSpacing = 0.5.sp
                                    ),
                                    textAlign = TextAlign.Start,
                                    color = if (selectedBook == book) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedBookState = book }
                                        .padding(vertical = 12.dp, horizontal = 12.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant))

                    // Chapters Column
                    val bookChapters = remember(selectedBook, chapters) {
                        chapters.filter { it.book == selectedBook }
                    }

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(bookChapters) { chapter ->
                            val isCompleted = completedChapters.contains(chapter.book to chapter.chapter)
                            val chapterName = if (translationMode == TranslationMode.KJV_1611 && orthographyMode == OrthographyMode.ORIGINAL_1611) {
                                "Chap. ${toRomanNumeral(chapter.chapter)}"
                            } else {
                                "Chapter ${chapter.chapter}"
                            }
                            Surface(
                                color = if (selectedChapter == chapter.chapter) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) else Color.Transparent,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedChapter = chapter.chapter }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = chapterName,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontFamily = legibleFont,
                                            fontWeight = if (selectedChapter == chapter.chapter) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        textAlign = TextAlign.Start,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isCompleted) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Completed",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant))

                    // Verses Column
                    val activeChapter = remember(selectedBook, selectedChapter, chapters) {
                        chapters.find { it.book == selectedBook && it.chapter == selectedChapter }
                    }
                    val verseCount = activeChapter?.verseCount ?: 0

                    LazyColumn(modifier = Modifier.weight(0.8f)) {
                        item {
                            Text(
                                text = "Chapter Start",
                                style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedBook?.let { b ->
                                            onChapterSelected(b, selectedChapter, 1)
                                            onDismiss()
                                        }
                                    }
                                    .padding(vertical = 12.dp, horizontal = 12.dp)
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                        }
                        items(verseCount) { index ->
                            val verseNumber = index + 1
                            Text(
                                text = "Verse $verseNumber",
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = legibleFont),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedBook?.let { b ->
                                            onChapterSelected(b, selectedChapter, verseNumber)
                                            onDismiss()
                                        }
                                    }
                                    .padding(vertical = 12.dp, horizontal = 12.dp)
                            )
                        }
                    }
                }
                
                HorizontalDivider()
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.padding(8.dp).fillMaxWidth()
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

private fun toRomanNumeral(number: Int): String {
    val romanNumerals = listOf(
        1000 to "M", 900 to "CM", 500 to "D", 400 to "CD",
        100 to "C", 90 to "XC", 50 to "L", 40 to "XL",
        10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I"
    )
    var n = number
    val result = StringBuilder()
    for ((value, numeral) in romanNumerals) {
        while (n >= value) {
            result.append(numeral)
            n -= value
        }
    }
    return result.toString()
}

private fun getFontFamily(font: StudyFont, translation: TranslationMode): FontFamily {
    return when (font) {
        StudyFont.SYSTEM -> {
            if (translation == TranslationMode.KJV_1611) {
                FontFamily(Font(R.font.kjva6aa))
            } else {
                FontFamily.Default
            }
        }
        StudyFont.SERIF -> FontFamily.Serif
        StudyFont.SANS_SERIF -> FontFamily.SansSerif
        StudyFont.MONOSPACE -> FontFamily.Monospace
        StudyFont.BLACKLETTER -> FontFamily(Font(R.font.kjva6aa))
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
