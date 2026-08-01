# VoiceJournal — Comprehensive Screen-by-Screen UX Review

## Executive Summary
This document presents a granular UX, visual hierarchy, and structural review of every screen and modal experience in **VoiceJournal**.

---

## 1. Journal Main Feed (`JournalScreen.kt`)

### Purpose & Role
Primary landing view displaying reverse-chronological list of voice and text journal entries with search, drawer filtering, and quick voice recording.

### UX Layout Analysis
- **Top Bar**: [`JournalHeader.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/journal/components/JournalHeader.kt) (Title, Search Toggle, Navigation Drawer Trigger).
- **Sub-Header**: [`FilterBar.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/journal/components/FilterBar.kt) (`[All]`, `[Tags]`, `[People]`, `[Mood]`).
- **Body**: `LazyColumn` of [`EntryCard.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/journal/components/EntryCard.kt).
- **Floating Controls**: [`MicFab.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/journal/components/MicFab.kt).

### Identified UX Flaws & Friction
1. **Filter Active State Overload**: Applying multiple filters displays counter badges, but there is no prominent "Clear All Filters" button when a filter yields 0 entries.
2. **Card Sub-48dp Touch Targets**: Inline delete icons on tag chips embedded in entry cards are too small (~14dp).
3. **Empty State Lack of Guidance**: Empty list shows text without an inline "Record Voice Note" primary button.

---

## 2. Note Detail & Editor (`NoteDetailScreen.kt`)

### Purpose & Role
Detailed editing view for single journal entries with audio recording, full waveform playback, image attachments, tags, date editing, and speech-to-text transcript generation.

### UX Layout Analysis
- **Top Bar**: `NoteDetailHeader` (Back, Folder Selector, Date/Time Edit Button, Delete/Archive Overflow Menu).
- **Audio Header**: `AudioPlayerFull` with expandable transcript drawer.
- **Media Section**: `ImageMosaic` thumbnail grid.
- **Body Editor**: `UserTextInput` for title and main body text.
- **Bottom Bar**: `EditorToolbar` (Attach Image, Add Tag, Record Voice Note).

### Identified UX Flaws & Friction
1. **Unsaved State Risk**: Navigating back with unsaved changes launches a prompt dialog, but there is no clear indicator on the screen header showing whether changes are saved.
2. **Transcript Generation Motion**: Expanding transcript container pops abruptly without vertical size animation.
3. **Image Attachment Constraints**: Photo picker launcher is capped at 4 items without explaining this limit to the user.

---

## 3. Calendar View (`CalendarScreen.kt`)

### Purpose & Role
Monthly grid view for exploring journal entries by date, featuring day selection and mini audio card lists.

### UX Layout Analysis
- **Top Bar**: `MonthHeader` with month navigation arrows and year selector.
- **Header**: `CalendarFilterBar`.
- **Grid**: `CalendarGrid` rendering `DayCell` items.
- **List**: `DayEntryList` rendering `MiniAudioCard` items.

### Identified UX Flaws & Friction
1. **Day Cell Small Touch Targets**: `DayCell` touch areas on small screen devices feel cramped.
2. **Empty Day State**: Selecting a date with no entries displays a blank space under the calendar instead of an encouraging "No entries recorded on this day. Tap + to add" callout.

---

## 4. Insights & Reflection (`InsightScreen.kt`)

### Purpose & Role
Analytics screen showing mood trends, top tags, people mentioned, and recording activity heatmaps over custom time ranges.

### UX Layout Analysis
- **Top Bar**: Static "Insights" title.
- **Controls**: `PeriodSelector` (7d, 30d, 90d, 1y, All).
- **Cards**: `RecordingActivityCard`, `MoodTrendsCard`, `TopTagsCard`, `PeopleMentionedCard`, `PeakReflectionCard`.

### Identified UX Flaws & Friction
1. **Chart Interactivity Gap**: Bar charts and mood trends render via Vico, but tapping on individual bars does not display dynamic tooltips with exact note counts.
2. **Skeleton Shimmer Color Leak**: `InsightsShimmerSkeleton` uses hardcoded dark colors, breaking Light Theme display.

---

## 5. Settings Suite (`SettingsScreen.kt`)

### Purpose & Role
App configuration tree for theme, biometric app lock, local backups, and privacy controls.

### Identified UX Flaws & Friction
1. **App Lock Mode Ambiguity**: Selecting "Custom PIN" launches PIN setup, but if custom PIN setup is cancelled midway, the lock mode state is left in a transient incomplete state.
2. **Search Results Highlight**: `SettingsSearchBar` filters setting rows, but matching text within setting descriptions is not highlighted.

---

## 6. Tags Management (`TagsScreen.kt`)

### Purpose & Role
Overview of all hashtags, topics, people mentions, and mood tags with tag creation and deletion tools.

### Identified UX Flaws & Friction
1. **Tag Action Dialog Confusion**: Deleting a tag does not warn the user how many journal entries will lose that tag tag reference.

---

## 7. Folders Management (`FoldersScreen.kt`)

### Purpose & Role
Organizational folder tree allowing users to group notes into custom categories.

### Identified UX Flaws & Friction
1. **Icon Picker Discoverability**: Choosing custom folder icons relies on a horizontal scroll list of icons (`FolderIcons.kt`) with small tap targets.

---

## 8. Archive Screen (`ArchiveScreen.kt`)

### Purpose & Role
List of archived notes removed from main feed without permanent deletion.

### Identified UX Flaws & Friction
1. **Restore Action Visibility**: Restoring an archived note requires long-pressing or tapping overflow menu; no quick swipe-to-restore gesture.

---

## 9. Trash Screen (`TrashScreen.kt`)

### Purpose & Role
Soft-delete bin retaining deleted notes for 30 days before permanent destruction.

### Identified UX Flaws & Friction
1. **Auto-Purge Warning**: Lacks a prominent top header notification warning users that items in trash will be permanently deleted after 30 days.

---

## 10. Drafts Screen (`DraftScreen.kt`)

### Purpose & Role
Holds incomplete voice notes or unsaved draft entries.

### Identified UX Flaws & Friction
1. **Draft Resume Action**: Tapping a draft opens `NoteDetailScreen`, but does not automatically focus the editor or resume voice recording.

---

## 11. Security Lock Overlay Screens (`BiometricLockScreen` & `CustomPinLockScreen`)

### Purpose & Role
Authentication overlay safeguarding private journal data on app start or resume.

### Identified UX Flaws & Friction
1. **PIN Keypad Tactile Feedback**: Tapping numbers on custom PIN keypad produces no haptic tick or press compression visual.
