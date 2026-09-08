# Walkthrough - Facsimile Flashback Implementation

I have implemented the "Facsimile Flashback" feature, which allows users to view high-resolution scans of the original 1611 pages.

## Changes Made

### 1. StudyViewModel.kt
- Added `isFacsimileMode` to `StudyUiState`.
- Added `isFacsimileMode` StateFlow to `StudyViewModel`.
- Integrated `isFacsimileMode` into the `uiState` combined flow.
- Added `toggleFacsimileMode()` method to handle UI interaction.

### 2. StudyAppRoot.kt
- Added a "Visibility" (Info) icon button to the `TopAppBar` when in `READ` mode.
- Implemented `FacsimileOverlay` Composable that displays a zoomable image of the original 1611 page.
- Integrated the `FacsimileOverlay` into the main `Box` of `StudyAppRoot`, ensuring it appears as an immersive overlay.
- Reused the `ZoomableImage` component to allow users to inspect the historical typography in detail.

## Verification
- Built the project successfully (`app:assembleDebug`).
- The UI logic is wired up to toggle the overlay when the button is pressed.
- The overlay shows the current Book and Chapter as a context label.

## Future Improvements
- Map each chapter to its specific high-res scan once the assets are available.
- Currently uses `bible_title_1611.png` as a representative placeholder for all chapters.
