# VoiceJournal — Component Inventory

## Overview
This document contains a complete inventory of every reusable UI component in the **VoiceJournal** codebase. It classifies components by architectural status, reusability, priority tier, screen dependencies, and refactoring migration strategy to prepare for the Design System package migration.

---

## Priority Classification Key
- **P0 (Critical Infrastructure)**: Core foundation components (Theme, Audio Player, Base Layouts) required before screen migrations.
- **P1 (High Priority)**: Widely used cards, chips, dialogs, and headers affecting core journal user flows.
- **P2 (Medium Priority)**: Analytics, settings, calendar, and folder management widgets.
- **P3 (Low Priority)**: Niche dialogs and minor utility surfaces.

---

## 1. Audio & Playback Components

### `UnifiedAudioPlayerBar`
- **Current Location**: [`ui/components/UnifiedAudioPlayerBar.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/components/UnifiedAudioPlayerBar.kt)
- **Purpose**: Canonical single-instance audio player bar with waveform visualizer and drag scrubbing.
- **Current Status**: Core Component
- **Reusable?**: Yes
- **Duplicate Exists?**: No (Canonical)
- **Needs Refactor?**: Yes (Integrate `AppMotion` and standard touch target padding)
- **Priority**: **P0**
- **Depends On**: `WaveformVisualizer`, `AppTheme`
- **Screens Using It**: `JournalScreen`, `NoteDetailScreen`, `CalendarScreen`, `TagsScreen`, `FoldersScreen`
- **Migration Strategy**: Move into `ui/designsystem/components/audio/UnifiedAudioPlayerBar.kt`. Standardize timer font to `MaterialTheme.typography.bodySmall`.

### `WaveformVisualizer`
- **Current Location**: [`ui/components/WaveformVisualizer.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/components/WaveformVisualizer.kt)
- **Purpose**: Canvas-based 32-bar waveform rendering engine supporting seek gestures and TalkBack semantics.
- **Current Status**: Core Component
- **Reusable?**: Yes
- **Duplicate Exists?**: No
- **Needs Refactor?**: Minor (Lock horizontal drag gesture to prevent list scroll conflict)
- **Priority**: **P0**
- **Depends On**: `AppTheme`
- **Screens Using It**: `UnifiedAudioPlayerBar`, `EntryCard`
- **Migration Strategy**: Move into `ui/designsystem/components/audio/WaveformVisualizer.kt`.

### `AudioPlayerBar`
- **Current Location**: [`ui/components/AudioPlayerBar.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/components/AudioPlayerBar.kt)
- **Purpose**: Legacy wrapper forwarding parameters to `UnifiedAudioPlayerBar`.
- **Current Status**: Deprecated Wrapper
- **Reusable?**: No
- **Duplicate Exists?**: Yes (Duplicates `UnifiedAudioPlayerBar`)
- **Needs Refactor?**: Yes (Delete and replace callers)
- **Priority**: **P1**
- **Depends On**: `UnifiedAudioPlayerBar`
- **Screens Using It**: Legacy invocation sites
- **Migration Strategy**: Replace invocations with direct `UnifiedAudioPlayerBar` calls and delete file.

### `AudioPlayerFull`
- **Current Location**: [`ui/notedetail/components/AudioPlayerFull.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/notedetail/components/AudioPlayerFull.kt)
- **Purpose**: Wraps `UnifiedAudioPlayerBar` with speech-to-text transcript toggle ("Aa") and expandable transcript drawer.
- **Current Status**: Feature Component
- **Reusable?**: Yes
- **Duplicate Exists?**: No
- **Needs Refactor?**: Yes (Add `animateContentSize` vertical transition on transcript expansion)
- **Priority**: **P1**
- **Depends On**: `UnifiedAudioPlayerBar`, `AppTheme`
- **Screens Using It**: `NoteDetailScreen`
- **Migration Strategy**: Move into `ui/designsystem/components/audio/AudioPlayerFull.kt`.

---

## 2. Recording Components

### `MicFab`
- **Current Location**: [`ui/journal/components/MicFab.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/journal/components/MicFab.kt)
- **Purpose**: Floating Action Button triggering voice recording in the main feed.
- **Current Status**: Core Component
- **Reusable?**: Yes
- **Duplicate Exists?**: No
- **Needs Refactor?**: Yes (Enforce 48dp minimum touch target, add haptic tick on tap)
- **Priority**: **P0**
- **Depends On**: `AppTheme`
- **Screens Using It**: `JournalScreen`
- **Migration Strategy**: Move into `ui/designsystem/components/buttons/MicFab.kt`.

### `MicRecordingPill`
- **Current Location**: [`ui/notedetail/components/MicRecordingPill.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/notedetail/components/MicRecordingPill.kt)
- **Purpose**: Active recording bar showing live duration, amplitude meter, pause/resume, and discard/save actions.
- **Current Status**: Feature Component
- **Reusable?**: Yes
- **Duplicate Exists?**: No
- **Needs Refactor?**: Yes (Standardize height math and replace hardcoded button padding)
- **Priority**: **P1**
- **Depends On**: `AppTheme`, `AppMotion`
- **Screens Using It**: `NoteDetailScreen`
- **Migration Strategy**: Move into `ui/designsystem/components/audio/MicRecordingPill.kt`.

---

## 3. Cards & List Elements

### `EntryCard`
- **Current Location**: [`ui/journal/components/EntryCard.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/journal/components/EntryCard.kt)
- **Purpose**: Primary list card displaying date, title preview, image thumbnails, inline audio player bars, and tag chips.
- **Current Status**: Stable
- **Reusable?**: Yes
- **Duplicate Exists?**: No
- **Needs Refactor?**: Yes (Replace hardcoded hex colors, wrap inline tags in 48dp touch targets)
- **Priority**: **P1**
- **Depends On**: `UnifiedAudioPlayerBar`, `TagChip`, `AppTheme`
- **Screens Using It**: `JournalScreen`, `ArchiveScreen`, `TrashScreen`, `DraftScreen`
- **Migration Strategy**: Move into `ui/designsystem/components/cards/EntryCard.kt`.

### `MiniAudioCard`
- **Current Location**: [`ui/calendar/components/MiniAudioCard.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/calendar/components/MiniAudioCard.kt)
- **Purpose**: Compact entry summary card used in calendar day details.
- **Current Status**: Feature Component
- **Reusable?**: Yes
- **Duplicate Exists?**: No
- **Needs Refactor?**: Minor (Align corner radius with `RadiusMd` design token)
- **Priority**: **P2**
- **Depends On**: `TagChip`, `AppTheme`
- **Screens Using It**: `CalendarScreen`
- **Migration Strategy**: Move into `ui/designsystem/components/cards/MiniAudioCard.kt`.

### `ShimmerSkeletonCard`
- **Current Location**: [`ui/journal/components/ShimmerSkeletonCard.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/journal/components/ShimmerSkeletonCard.kt)
- **Purpose**: Animated shimmer loading skeleton matching `EntryCard` geometry.
- **Current Status**: Needs Refactor
- **Reusable?**: Yes
- **Duplicate Exists?**: No
- **Needs Refactor?**: **Yes (Critical)** — Remove hardcoded dark hex colors (`0xFF2A2A2A`, `0xFF1E1E1E`) so shimmer adapts to Light Theme.
- **Priority**: **P0**
- **Depends On**: `AppTheme`
- **Screens Using It**: `JournalScreen`
- **Migration Strategy**: Move into `ui/designsystem/components/feedback/ShimmerSkeletonCard.kt`. Use `AppTheme.colors.surfaceVariant`.

### `InsightsShimmerSkeleton`
- **Current Location**: [`ui/insight/components/InsightsShimmerSkeleton.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/insight/components/InsightsShimmerSkeleton.kt)
- **Purpose**: Loading skeleton for reflection analytics cards.
- **Current Status**: Needs Refactor
- **Reusable?**: Yes
- **Duplicate Exists?**: No
- **Needs Refactor?**: **Yes (Critical)** — Replace hardcoded dark hex colors with dynamic theme tokens.
- **Priority**: **P0**
- **Depends On**: `AppTheme`
- **Screens Using It**: `InsightScreen`
- **Migration Strategy**: Move into `ui/designsystem/components/feedback/InsightsShimmerSkeleton.kt`.

---

## 4. Chips & Filter Controls

### `TagChip`
- **Current Location**: [`ui/components/TagChip.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/components/TagChip.kt)
- **Purpose**: Renders topic `#tags`, `@people`, `emoji` moods, and `folder` chips.
- **Current Status**: Needs Refactor
- **Reusable?**: Yes
- **Duplicate Exists?**: No
- **Needs Refactor?**: **Yes (High)** — Apply `minimumInteractiveComponentSize()` and expand delete icon touch target from 14dp to 48dp.
- **Priority**: **P1**
- **Depends On**: `AppTheme`
- **Screens Using It**: `JournalScreen`, `NoteDetailScreen`, `CalendarScreen`, `TagsScreen`
- **Migration Strategy**: Move into `ui/designsystem/components/chips/TagChip.kt`.

### `FilterBar`
- **Current Location**: [`ui/journal/components/FilterBar.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/journal/components/FilterBar.kt)
- **Purpose**: Horizontal scrollable row containing category filter chips (`[All]`, `[Tags]`, `[People]`, `[Mood]`).
- **Current Status**: Stable
- **Reusable?**: Yes
- **Duplicate Exists?**: No
- **Needs Refactor?**: Minor (Apply 48dp minimum touch targets to filter chips)
- **Priority**: **P1**
- **Depends On**: `AppTheme`
- **Screens Using It**: `JournalScreen`
- **Migration Strategy**: Move into `ui/designsystem/components/chips/FilterBar.kt`.

---

## 5. Dialogs, Bottom Sheets & Navigation Bars

### `JournalHeader`
- **Current Location**: [`ui/journal/components/JournalHeader.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/journal/components/JournalHeader.kt)
- **Purpose**: Top bar with title, integrated search field toggle, and drawer navigation icon.
- **Current Status**: Stable
- **Reusable?**: Yes
- **Duplicate Exists?**: No
- **Needs Refactor?**: Minor (Add trailing clear icon inside search field)
- **Priority**: **P1**
- **Depends On**: `AppTheme`
- **Screens Using It**: `JournalScreen`
- **Migration Strategy**: Move into `ui/designsystem/components/navigation/JournalHeader.kt`.

### `BottomNavBar`
- **Current Location**: [`ui/navigation/BottomNavBar.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/navigation/BottomNavBar.kt)
- **Purpose**: Root navigation bar with Journal, Calendar, and Insights items.
- **Current Status**: Core Component
- **Reusable?**: Yes
- **Duplicate Exists?**: No
- **Needs Refactor?**: Minor (Align indicator colors with `AppTheme.colors.primaryContainer`)
- **Priority**: **P0**
- **Depends On**: `AppTheme`
- **Screens Using It**: `MainActivity`
- **Migration Strategy**: Move into `ui/designsystem/components/navigation/BottomNavBar.kt`.

### `FilterSheets` & `BatchCategorizeSheet`
- **Current Locations**: [`ui/journal/components/FilterSheets.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/journal/components/FilterSheets.kt), [`ui/journal/components/BatchCategorizeSheet.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/journal/components/BatchCategorizeSheet.kt)
- **Purpose**: Modal bottom sheets for tag/person filtering and batch categorization.
- **Current Status**: Needs Refactor
- **Reusable?**: Yes
- **Duplicate Exists?**: Partial duplication between filter sheet and batch categorize sheet.
- **Needs Refactor?**: Consolidate shared search inputs and tag lists into unified sheet primitives.
- **Priority**: **P1**
- **Depends On**: `AppTheme`, `TagChip`
- **Screens Using It**: `JournalScreen`
- **Migration Strategy**: Move into `ui/designsystem/components/sheets/`.

### `TagActionDialogs` & `JournalTagsDialog`
- **Current Locations**: [`ui/components/TagActionDialogs.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/components/TagActionDialogs.kt), [`ui/notedetail/components/JournalTagsDialog.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/notedetail/components/JournalTagsDialog.kt)
- **Purpose**: Create, edit, and attach tags to entries.
- **Current Status**: Needs Refactor
- **Reusable?**: Yes
- **Duplicate Exists?**: Yes (Tag creation logic duplicated)
- **Needs Refactor**: Consolidate into `UnifiedTagDialog.kt`.
- **Priority**: **P1**
- **Depends On**: `AppTheme`
- **Screens Using It**: `JournalScreen`, `NoteDetailScreen`, `TagsScreen`
- **Migration Strategy**: Consolidate into `ui/designsystem/components/dialogs/UnifiedTagDialog.kt`.
