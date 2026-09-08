# Implementation Plan - Pure Words 1611 Improvements

This plan addresses data loading issues, schema inconsistencies, seeker path completion, and front matter expansion.

## User Review Required

> [!IMPORTANT]
> The database initialization logic is complex because it manually handles Room's master table. Changing the identity hash is critical for Room to accept the pre-populated database.

## Proposed Changes

### Database & Data Loading
Fixes for initialization, schema, and search.

#### [MODIFY] [ManualDatabaseInitializer.kt](file:///C:/Users/chadl/AndroidStudioProjects/PureWords1611/app/src/main/kotlin/com/purewords1611/android/study/data/local/ManualDatabaseInitializer.kt)
- Update `identityHash` to `521cc8c9b14f70e88e8bbd4d4d73affb`.
- Update `fixSchema` to include `comparativeText` column in `verses` table.
- Move FTS rebuild to a separate method `rebuildFts` and call it at the very end of `ensureInitialized` to ensure all alternate texts are indexed.

### Seeker Path
Expansion of seeker tracks and steps.

#### [MODIFY] [seeker_tracks_v1.json](file:///C:/Users/chadl/AndroidStudioProjects/PureWords1611/app/src/main/assets/study/seeker_tracks_v1.json)
- Expand descriptions for existing tracks.
- Add a new track: `HISTORICAL_CONTEXT`.

#### [MODIFY] [seeker_steps_v1.json](file:///C:/Users/chadl/AndroidStudioProjects/PureWords1611/app/src/main/assets/study/seeker_steps_v1.json)
- Add more steps for `FULL_EVIDENCE`.
- Add steps for the new `HISTORICAL_CONTEXT` track.

### 1611 Front Matter
Expanding historical content.

#### [MODIFY] [front_matter_v1.json](file:///C:/Users/chadl/AndroidStudioProjects/PureWords1611/app/src/main/assets/study/front_matter_v1.json)
- Expand "The Almanack" and "The Proper Lessons" with more authentic 1611 content.
- Add "Rules of Translation" document.

### UI Improvements
#### [MODIFY] [StudySettingsSheet](file:///C:/Users/chadl/AndroidStudioProjects/PureWords1611/app/src/main/kotlin/com/purewords1611/android/study/ui/StudyAppRoot.kt)
- Verify and ensure Light/Dark mode toggle works correctly. (It seems already implemented but I will double check the `ThemeToggle` usage).

## Verification Plan

### Automated Tests
- Run the app and verify it proceeds past "Reading Pure Words...".
- Perform a search for a word present in ESV or Standard KJV to verify FTS is working.

### Manual Verification
- Check the "Seeker Path" screen to see the new tracks and steps.
- Check the "1611" (Front Matter) screen to see the expanded content.
- Toggle between Light, Dark, and Sepia modes in settings.
