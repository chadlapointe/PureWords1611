# Modern Bible App UX / Action Bar Refactoring

To make the verse selection, highlighting, sharing, and copying feel fluid and comparable to top-tier Bible apps like YouVersion, we will decouple the "Study click" (for Glossary/Strongs) from the "Verse select" click.

## Goal
Implement a single-tap-to-select verse flow that activates a Contextual Action Bar anchored to the bottom of the screen, supporting multi-selection and actions (Copy, Share, Bookmark, Highlight).

## Proposed Changes

### `StudyViewModel.kt`
- Add a new state flow: `val selectedVersesForAction = MutableStateFlow<Set<Long>>(emptySet())`
- Add intent methods: `fun toggleVerseSelection(verseId: Long)`, `fun clearVerseSelection()`
- Remove single-verse `selectedVerseId` logic (or adapt it to handle multi-select).

### `StudyAppRoot.kt` (UI Layer)
- Modify `VerseTextLine` and `DropCapVerseLine` modifiers to detect standard clicks to toggle `selectedVersesForAction`.
- Remove the `onClick` passthrough from the `ClickableText` (which was causing gesture interference). The `ClickableText` should ONLY fire for `onS` (Strongs) and `onG` (Glossary).
- **Contextual Action Bar Component:** Add an `AnimatedVisibility` block inside `ReadScreen` (anchored to the bottom) that appears when `selectedVersesForAction` is not empty.
- The Action Bar will have buttons for:
  - Copy (copies selected verses text)
  - Share (triggers standard Android share sheet with verse text)
  - Bookmark (bookmarks selected verses)
  - Color Picker (highlight selected verses with 1611-themed colors)
  - Clear (X button to dismiss selection)

### Theming / "1611" Aesthetic
- The Contextual Action Bar will have a dark, sleek design with classical iconography.
- Highlight colors will use classic names/hexes (e.g., Ochre, Tyrian, Verdigris).

## Open Questions
- Do we want long-press to activate the Lexicon instead of standard click, so regular click can be strictly for selection? Or should the text only be selectable via tapping the *row background* instead of the text itself?
  > For this plan, we will route *all unannotated text clicks* back up to the Row to toggle verse selection.

## Verification Plan
- Deploy to emulator.
- Tap a verse; it should highlight and display the Bottom Action Bar.
- Tap a glossary term; it should still open the Glossary bottom sheet.
- Test Copy and Share intents.
