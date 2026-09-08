# Phase 7 Implementation Plan - Semantic Precision & Search Depth

This phase focuses on making the app more powerful for serious researchers by enabling granular text selection and advanced phrase-based search capabilities.

## User Review Required

> [!IMPORTANT]
> This phase replaces the older `ClickableText` component with a more modern selection-aware approach. This will change how the app handles long-press and selection gestures in the reader.

## Proposed Changes

### [Search Engine Refinement]

#### [MODIFY] [StudyRepository.kt](file:///C:/Users/chadl/AndroidStudioProjects/PureWords1611/app/src/main/kotlin/com/purewords1611/android/study/data/StudyRepository.kt)
- Update `observeVerses` to correctly format queries containing quotes for SQLite FTS4.
- Implement logic to handle `"exact phrase"` matching by wrapping the FTS query appropriately.

#### [MODIFY] [StudyAppRoot.kt](file:///C:/Users/chadl/AndroidStudioProjects/PureWords1611/app/src/main/kotlin/com/purewords1611/android/study/ui/StudyAppRoot.kt)
- **`highlightSearchQuery`**: Improve to handle exact phrases. If the query is in quotes, it should match the entire phrase as a single entity rather than individual words.
- **`SearchVerseResult`**: Update the snippet display to prioritize the context around the matching phrase.

### [Granular Text Selection]

#### [MODIFY] [StudyAppRoot.kt](file:///C:/Users/chadl/AndroidStudioProjects/PureWords1611/app/src/main/kotlin/com/purewords1611/android/study/ui/StudyAppRoot.kt)
- Wrap `ReaderItem` components in a `SelectionContainer` or implement a custom selection handler for verses.
- Update `VerseTextLine` and `DropCapVerseLine` to support text selection.
- Implement a floating action menu or update `StudyHubSheet` to handle actions on selected text (Copy Phrase, Highlight Selection).

#### [MODIFY] [StudyViewModel.kt](file:///C:/Users/chadl/AndroidStudioProjects/PureWords1611/app/src/main/kotlin/com/purewords1611/android/study/ui/StudyViewModel.kt)
- Add state to track selected text and its range within a verse.

## Verification Plan

### Manual Verification
1.  **Exact Phrase Search**: Search for `"Beginning of the Gospell"` (including quotes). Verify it only returns Mark 1:1 and not every verse containing "beginning" or "gospell".
2.  **Granular Selection**: Long-press a word in a verse. Verify that selection handles appear and allow selecting a specific phrase.
3.  **Phrase Copy**: Select a phrase and use the "Copy" action. Verify only the selected text is copied to the clipboard.
4.  **Phrase Highlight**: (Planned enhancement) Select a phrase and apply a highlight. Verify the highlight is anchored to those specific words.
