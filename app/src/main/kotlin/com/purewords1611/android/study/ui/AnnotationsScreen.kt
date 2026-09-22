package com.purewords1611.android.study.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.purewords1611.android.study.data.BookmarkItem
import com.purewords1611.android.study.data.HighlightItem
import com.purewords1611.android.study.data.PersonalNoteItem
import com.purewords1611.android.study.data.local.MarginaliaEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AnnotationTab { ALL, HIGHLIGHTS, FAVORITES, NOTES }

@Composable
fun AnnotationsScreen(
    highlights: List<HighlightItem>,
    bookmarks: List<BookmarkItem>,
    marginalia: List<MarginaliaEntity>,
    personalNotes: List<PersonalNoteItem>,
    onVerseClick: (Long) -> Unit,
    onChapterClick: (String, Int) -> Unit,
    onRemoveHighlight: (Long) -> Unit,
    onRemoveBookmark: (Long) -> Unit
) {
    var selectedTab by remember { mutableStateOf(AnnotationTab.ALL) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "My Annotations & Highlights",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Filter Tab Row
        ScrollableTabRow(
            selectedTabIndex = selectedTab.ordinal,
            edgePadding = 0.dp
        ) {
            AnnotationTab.values().forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = {
                        val count = when (tab) {
                            AnnotationTab.ALL -> highlights.size + bookmarks.size + marginalia.size + personalNotes.size
                            AnnotationTab.HIGHLIGHTS -> highlights.size
                            AnnotationTab.FAVORITES -> bookmarks.size
                            AnnotationTab.NOTES -> marginalia.size + personalNotes.size
                        }
                        Text("${tab.name.lowercase().replaceFirstChar { it.uppercase() }} ($count)")
                    }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Highlights Section
            if (selectedTab == AnnotationTab.ALL || selectedTab == AnnotationTab.HIGHLIGHTS) {
                if (highlights.isNotEmpty()) {
                    item {
                        Text(
                            text = "Verse Highlights",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(highlights.sortedBy { it.verseId }) { item ->
                        val swatchColor = getHighlightColor(item.colorName)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onVerseClick(item.verseId) },
                            colors = CardDefaults.cardColors(
                                containerColor = swatchColor.copy(alpha = 0.25f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(swatchColor)
                                    )
                                    Column {
                                        Text(
                                            text = "Verse ID #${item.verseId}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Color: ${item.colorName} • ${formatDate(item.createdAtEpochMillis)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(onClick = { onRemoveHighlight(item.verseId) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove Highlight")
                                }
                            }
                        }
                    }
                }
            }

            // Favorites / Bookmarks Section
            if (selectedTab == AnnotationTab.ALL || selectedTab == AnnotationTab.FAVORITES) {
                if (bookmarks.isNotEmpty()) {
                    item {
                        Text(
                            text = "Starred Favorites & Bookmarks",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(bookmarks.sortedBy { it.verseId }) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onVerseClick(item.verseId) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Starred",
                                        tint = Color(0xFFFFC107)
                                    )
                                    Column {
                                        Text(
                                            text = "Verse ID #${item.verseId}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Starred on ${formatDate(item.createdAtEpochMillis)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(onClick = { onRemoveBookmark(item.verseId) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove Favorite")
                                }
                            }
                        }
                    }
                }
            }

            // Notes & Marginalia Section
            if (selectedTab == AnnotationTab.ALL || selectedTab == AnnotationTab.NOTES) {
                if (personalNotes.isNotEmpty()) {
                    item {
                        Text(
                            text = "Personal Notes",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(personalNotes) { note ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onVerseClick(note.verseId) }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text(
                                        text = "Verse ID #${note.verseId} ${if (note.category != null) "• [${note.category}]" else ""}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(note.note, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                if (marginalia.isNotEmpty()) {
                    item {
                        Text(
                            text = "Marginalia Drawings",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(marginalia) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onChapterClick(item.book, item.chapter) }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "${item.book} Chapter ${item.chapter}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Handwritten Marginalia Drawing • Updated ${formatDate(item.updatedAtEpochMillis)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getHighlightColor(colorName: String): Color {
    return when (colorName.lowercase(Locale.ROOT)) {
        "yellow" -> Color(0xFFFFF59D)
        "red", "pink" -> Color(0xFFEF9A9A)
        "blue" -> Color(0xFF90CAF9)
        "green" -> Color(0xA5D6A7)
        else -> Color(0xFFFFF59D)
    }
}

private fun formatDate(epochMillis: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(epochMillis))
}
