# Implementation Plan - Fix False Friends, Compare Screen, and Highlighting

This plan addresses several issues and feature requests for the PureWords1611 app, including fixing False Friend interaction/styling, investigating the Compare Screen "Text not available" bug, and planning for word-level highlighting.

## User Review Required

> [!IMPORTANT]
> - **Highlighting Color**: Changing False Friend highlight color from Wine Red to Deep Purple (`0xFF673AB7`) as requested.
> - **Highlighting Scope**: The current database model supports verse-level highlighting. Implementing word-level highlighting would require a schema change. I will start with fixing verse-level interaction first.
> - **Compare Screen Bug**: I suspect a book name mismatch between the 1611 base DB and the KJV/ESV JSON assets. I will add logging and a normalization step.

## Proposed Changes

### [Component] Study UI

#### [MODIFY] [FalseFriendGlossary.kt](file:///C:/Users/chadl/AndroidStudioProjects/PureWords1611/app/src/main/kotlin/com/purewords1611/android/study/ui/FalseFriendGlossary.kt)
- Change `highlightColor` default value to `Color(0xFF673AB7)` (Deep Purple).

#### [MODIFY] [StudyAppRoot.kt](file:///C:/Users/chadl/AndroidStudioProjects/PureWords1611/app/src/main/kotlin/com/purewords1611/android/study/ui/StudyAppRoot.kt)
- Update `VerseTextLine` and `DropCapVerseLine` to handle `GLOSSARY` annotations in `ClickableText`.
- Show a snackbar or tooltip with the False Friend definition when clicked.

### [Component] Data Initialization

#### [MODIFY] [ManualDatabaseInitializer.kt](file:///C:/Users/chadl/AndroidStudioProjects/PureWords1611/app/src/main/kotlin/com/purewords1611/android/study/data/local/ManualDatabaseInitializer.kt)
- Add book name normalization in `populateAlternateTexts` to ensure 1611 book names match KJV/ESV JSON keys (e.g., handling "1 Samuel" vs "I Samuel" or similar).
- Add more robust logging to track how many verses are updated.

### [Component] Features & Improvements

- **KJV vs ESV Comparison**: Add a new comparison mode in `ParallelScreen` to allow comparing Standard KJV directly with Modern ESV.
- **Improved Tools Idea**: Add a "Word Study" tool that allows selecting any word and searching it across the entire Bible with Lexicon integration.

## Verification Plan

### Automated Tests
- N/A (UI and Data Init changes are hard to test without full DB setup, will rely on manual verification via logs and UI inspection).

### Manual Verification
- Deploy the app and navigate to the Study screen.
- Verify False Friends are highlighted in purple and clickable.
- Verify the Compare screen shows actual text for KJV/ESV.
- Verify the new KJV vs ESV comparison mode.
