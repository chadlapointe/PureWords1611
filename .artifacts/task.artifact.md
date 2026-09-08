# Phase 7: Semantic Precision & Search Depth - Tasks

- [x] **Search Engine: Phrase Support**
    - [x] Update `StudyRepository.observeVerses` for quoted phrase matching
    - [x] Improve `highlightSearchQuery` for exact phrases
    - [x] Update `SearchVerseResult` for better contextual snippets
- [x] **Reader: Granular Selection**
    - [x] Replace `ClickableText` with selection-capable `Text` components using `pointerInput`
    - [x] Implement `SelectionContainer` in `ReadScreen`
    - [x] Add `selectedPhrase` state to `StudyViewModel`
    - [x] Update `StudyHubSheet` with phrase-specific actions (Copy Phrase)
- [ ] **Verification**
    - [ ] Verify exact phrase search results (e.g. searching `"in the beginning"`)
    - [ ] Verify text selection gestures (long-press) and Copy Verse action
