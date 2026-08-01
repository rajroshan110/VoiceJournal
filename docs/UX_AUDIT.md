# VoiceJournal — UX & Interaction Audit

## Executive Overview
This document presents a complete User Experience (UX) and interaction audit of **VoiceJournal**. The analysis evaluates the application from the perspective of a first-time user, identifying friction points, cognitive overload, feedback gaps, and usability inconsistencies across all major user journeys.

---

## 1. Global System Interactions & Micro-Flows

### 1.1 Navigation & Screen Flow
- **Identified Issues**:
  - **Inconsistent Backstack Behavior**: Navigating from nested drawer items (Folders, Tags, Archive, Drafts) back to the main Journal tab exhibits inconsistent stack clearing behavior, occasionally requiring multiple back presses.
  - **Deep-Link Argument Passing**: Passing `person` and `tag` arguments to `JournalScreen` does not trigger visual breadcrumbs in the top bar, leaving users uncertain if a filter is active.
- **Usability Impact**: High. First-time users feel disoriented when exploring side drawer destinations.

### 1.2 Motion & Animation Quality
- **Identified Issues**:
  - **Abrupt State Transitions**: Expanding the transcript box in `AudioPlayerFull` pops into view instantly or abruptly changes card height without smooth vertical layout animation.
  - **Static Dialog Appearance**: Modal sheets and confirm dialogs (e.g. Delete/Archive confirm) pop onto screen without spring physics or backdrop blur transitions.
- **Usability Impact**: Medium. Makes the application feel mechanical rather than polished.

### 1.3 Touch Targets & Selection Feedback
- **Identified Issues**:
  - **Sub-48dp Touch Boundaries**: The close icon on `TagChip` (14dp × 14dp) and small action icons on filter bars are extremely difficult to tap accurately on small screens.
  - **Long-Press Mode Discoverability**: Entering multi-selection mode via long press on an entry card lacks immediate visual/haptic feedback, causing users to accidentally launch the note detail view instead of selecting items.
- **Usability Impact**: High. Triggers user frustration during batch operations.

---

## 2. Feature-Specific UX Audits

### 2.1 Audio Recording & Mic FAB Experience
- **Current UX State**: Tapping the floating mic button launches recording mode.
- **Friction & Deficiencies**:
  - **Lack of Pre-Recording Visual Signal**: Pressing record initiates audio capture immediately without a subtle countdown pulse, leading to clipped speech at the start of recordings.
  - **Discard Safety Prompt**: Tapping back while recording shows a transient toast message ("Press back again to discard") instead of an explicit, safe bottom sheet prompt with "Save Draft" or "Discard" options.
  - **Amplitude Feedback Visuals**: Recording pill amplitude visualizer stutters when low-frequency background noise is present.

### 2.2 Audio Playback & Waveform Seeking
- **Current UX State**: `UnifiedAudioPlayerBar` renders a 32-bar waveform with play/pause and progress scrubbing.
- **Friction & Deficiencies**:
  - **Seek Touch Precision**: Scrubbing across a short audio track (e.g. 5 seconds) on a 32-bar visualizer requires high precision; the touch hit-box on waveform bars is narrow.
  - **Lack of Time Scrubbing Preview**: Scrubbing does not display a floating tooltip showing the target timestamp.
  - **Multi-Track Playback Ambiguity**: When an entry card contains 3 audio recordings, starting playback on track #2 does not clearly highlight track #2 relative to tracks #1 and #3.

### 2.3 Search & Filter Experience
- **Current UX State**: Top app bar toggles inline search field; `FilterBar` displays scrollable filter chips.
- **Friction & Deficiencies**:
  - **Search Query Clear Action**: Clearing a search query requires deleting text character-by-character; clear (`X`) button is missing inside the input box.
  - **Filter Active Indicator Overload**: Active filter badges display numeric counters, but active mood filters and tag filters conflict visually when applied concurrently.

### 2.4 Journal Browsing & Entry Cards
- **Current UX State**: `JournalScreen` displays a `LazyColumn` of `EntryCard`s.
- **Friction & Deficiencies**:
  - **Empty State Lack of Action**: When zero journal entries exist, the empty state displays a static message with no direct "Record Your First Entry" button.
  - **Text Truncation Indicator**: Note title previews truncate abruptly with ellipses without indicating whether additional text content exists inside the note.

### 2.5 Tag & Folder Management
- **Current UX State**: Dedicated `TagsScreen` and `FoldersScreen` list tags and folder categories.
- **Friction & Deficiencies**:
  - **Tag Creation Feedback**: Creating a new tag in `TagsScreen` closes the creation dialog without automatically scrolling to or highlighting the newly created tag chip.
  - **Person Mention vs Tag Disconnect**: Mentions starting with `@` are automatically routed to People, while `#` goes to Tags; this rule is not explained to users anywhere in the UI.

### 2.6 App Lock & Authentication Overlay
- **Current UX State**: `BiometricLockScreen` and `CustomPinLockScreen` render full-screen security overlays.
- **Friction & Deficiencies**:
  - **PIN Error Feedback**: Entering an incorrect PIN resets the dots without a shake animation or haptic error vibration.
  - **Biometric Prompt Timing**: On app start, biometric prompt launches immediately over the lock screen background, occasionally causing double-prompt triggers if the system biometric sheet is dismissed.

---

## 3. Comprehensive UX Audit Matrix

| Journey / Screen | Usability Friction | Cognitive Load | Visual Feedback | Recommended UX Improvement |
| :--- | :--- | :--- | :--- | :--- |
| **Journal Feed** | No quick filter reset button | Medium | Moderate | Add explicit "Clear All Filters" chip when filters are active |
| **Note Detail** | Unsaved changes prompt relies on back key | High | Low | Add sticky header status indicator ("Saved" / "Unsaved Changes") |
| **Audio Recorder** | Mic FAB tap can be accidental | Low | High | Add haptic confirmation on record start and stop |
| **Calendar View** | Days with entries show tiny dots | Medium | Low | Increase dot touch target and highlight current selected date |
| **Insights Tab** | Charts display raw numbers without context | High | Moderate | Add interactive tap tooltips on Vico chart bars showing exact totals |
| **Settings Tab** | App lock options buried in nested sub-screen | Low | Moderate | Expose lock mode status directly on the primary settings card |
| **Trash Screen** | 30-day purge warning is hard to notice | Medium | Low | Display permanent warning banner at top of trash list |
