package com.purewords1611.android.study.ui

import com.purewords1611.android.study.data.ReaderItem

data class ScrollTarget(
    val verseId: Long?,
    val absolutePosition: Int,
    val token: Long
)

fun resolveScrollIndex(snapshot: List<ReaderItem?>, target: ScrollTarget, placeholdersEnabled: Boolean = false): Int? {
    if (snapshot.isEmpty()) return null

    // 1. Try to find the exact verse ID
    if (target.verseId != null) {
        val exactMatchIndex = snapshot.indexOfFirst { it is ReaderItem.VerseLine && it.verse.id == target.verseId }
        if (exactMatchIndex >= 0) return exactMatchIndex
    }

    // 2. If it's a chapter header and absolute position points to a CompositeHeader, trust it
    val absItem = snapshot.getOrNull(target.absolutePosition)
    if (absItem is ReaderItem.CompositeHeader) return target.absolutePosition
    
    // 3. We cannot definitively resolve yet, wait for the actual verse to stream in
    return null
}
