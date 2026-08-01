# VoiceJournal — Interaction Quality & Tactile Feedback Audit

## Executive Overview
Interaction quality measures how responsive, predictable, and physically satisfying an application feels under human touch. This audit examines tactile feedback, input handling, selection mechanics, and gesture consistency across **VoiceJournal**.

---

## 1. Input & Text Editing Quality

### 1.1 Search Input Interaction
- **Current State**: [`JournalHeader.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/journal/components/JournalHeader.kt#L150-L240) displays a collapsible search bar.
- **Deficiencies**:
  - **Focus Requester Delay**: Tapping the search icon opens the text field but occasionally fails to request keyboard focus automatically on first tap.
  - **No Clear Action**: Clearing text requires manually deleting characters; no `ClearText` button exists inside the search input box.

### 1.2 Note Detail Text Input (`UserTextInput`)
- **Current State**: Title field and body text input in `NoteDetailScreen`.
- **Deficiencies**:
  - **Capitalization Defaults**: Title field capitalization settings are not explicitly locked to `KeyboardCapitalization.Sentences`.
  - **Autosave Visual Indicator**: Edits to body text update `hasUnsavedChanges` silently; no subtle status text ("Saved", "Editing...") appears in the header.

---

## 2. Selection & Batch Operations

### 2.1 Multi-Selection Mode Interaction
- **Current State**: Long-pressing an `EntryCard` enters batch selection mode, displaying a top action bar (`BatchCategorizeSheet.kt`).
- **Deficiencies**:
  - **Selection Haptic Delay**: Entering selection mode relies solely on long-press duration without triggering an immediate haptic tick (`HapticFeedbackType.LongPress`).
  - **Item Tap Target in Selection Mode**: Tapping anywhere on a card in selection mode toggles selection, but tapping the card's inner play button starts audio playback *while still in selection mode*, creating state confusion.

---

## 3. Gesture & Sheet Controls

### 3.1 Bottom Sheet Drag Gestures
- **Current State**: Filter bottom sheets (`FilterSheets.kt`, `BatchCategorizeSheet.kt`) wrap Compose M3 `ModalBottomSheet`.
- **Deficiencies**:
  - **Nested Scroll Conflicts**: Scrolling a long list of tags inside `FilterSheets` conflicts with the sheet's downward drag-to-dismiss gesture, causing accidental sheet dismissal when scrolling up from the top of the tag list.

### 3.2 Audio Waveform Drag Scrubbing
- **Current State**: `WaveformVisualizer` handles pointer input drag gestures to calculate seek fractions.
- **Deficiencies**:
  - **Pointer Drag Capture**: Dragging across the waveform visualizer inside an `EntryCard` within a `LazyColumn` can accidentally trigger vertical list scrolling if the pointer moves slightly off the horizontal axis.

---

## 4. Interaction Quality Audit Matrix

| Interaction Domain | Current Quality | Deficit Identified | Target Quality Requirement |
| :--- | :--- | :--- | :--- |
| **Haptic Feedback** | Non-existent | No tactile response on record, play, delete, select | Integrate `LocalHapticFeedback` on all key state triggers |
| **Search Input** | Functional | Missing inline text clear button | Add trailing `IconButton` with `Icons.Default.Clear` |
| **Audio Seek Drag** | Touch sensitive | Vertical scroll conflict inside LazyColumn | Lock pointer drag gesture to horizontal axis during seek |
| **Multi-Select** | Ambiguous | Audio plays during multi-select mode | Disable inner media controls during batch selection mode |
| **Draft Auto-Save** | Silent | User uncertain if changes are saved | Add transient header status ("Saved to Drafts") |
