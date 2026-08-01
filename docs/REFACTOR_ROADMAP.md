# VoiceJournal — Phased Refactor & Engineering Migration Roadmap

## Executive Strategy
This roadmap details a 8-phase migration plan to systematically update **VoiceJournal**'s UI architecture, design consistency, component modularity, motion dynamics, accessibility, and lifecycle performance **without breaking any application logic, data persistence, audio engine functionality, or user features**.

---

## Migration Phase Overview

```
Phase 1: Design Tokens & Dependencies
  ↓
Phase 2: Typography & Spacing Grid
  ↓
Phase 3: Component Standardization
  ↓
Phase 4: Motion System & Transitions
  ↓
Phase 5: Screen Layout Migration
  ↓
Phase 6: Accessibility & Touch Targets
  ↓
Phase 7: Performance & Lifecycle Optimization
  ↓
Phase 8: Final Polish & Visual QA
```

---

## Phase 1 — Design Tokens & Foundational Dependencies

- **Goal**: Establish single source of truth for color, spacing, radius, and elevation tokens in `UiColorTheme.kt` and add `lifecycle-runtime-compose` dependency.
- **Files Likely Affected**:
  - [`gradle/libs.versions.toml`](file:///Volumes/Work%20Storage/VoiceJournal/gradle/libs.versions.toml)
  - [`app/build.gradle.kts`](file:///Volumes/Work%20Storage/VoiceJournal/app/build.gradle.kts)
  - [`app/src/main/java/dev/voicejournal/ui/theme/UiColorTheme.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/theme/UiColorTheme.kt)
  - [`app/src/main/java/dev/voicejournal/ui/theme/Color.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/theme/Color.kt)
- **Risk Level**: Very Low.
- **Estimated Complexity**: Low (1-2 hours).
- **Manual Testing Checklist**:
  - Verify app builds cleanly (`./gradlew assembleDebug`).
  - Verify light and dark color palette objects resolve without crashes.
- **Regression Risks**: None.
- **Rollback Strategy**: Revert changes in `libs.versions.toml` and `UiColorTheme.kt`.
- **Expected Improvements**: Establishes foundation for theme switching and lifecycle-aware state collection.
- **Dependencies**: None.

---

## Phase 2 — Typography & Spacing System

- **Goal**: Standardize typography styles in `Type.kt` and replace inline `fontSize = X.sp` and arbitrary `dp` paddings with design grid tokens.
- **Files Likely Affected**:
  - [`app/src/main/java/dev/voicejournal/ui/theme/Type.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/theme/Type.kt)
  - [`EntryCard.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/journal/components/EntryCard.kt)
  - [`MiniAudioCard.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/calendar/components/MiniAudioCard.kt)
  - [`JournalHeader.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/journal/components/JournalHeader.kt)
- **Risk Level**: Low.
- **Estimated Complexity**: Medium.
- **Manual Testing Checklist**:
  - Verify text rendering across entry cards, top bar headers, and detail titles.
  - Test font scaling under system accessibility settings.
- **Regression Risks**: Slight text wrapping changes on narrow screen devices.
- **Rollback Strategy**: Revert `Type.kt` modifications.
- **Expected Improvements**: Eliminates typography inconsistencies and aligns text hierarchy with Material 3 standard roles.
- **Dependencies**: Phase 1.

---

## Phase 3 — Component Standardization & Consolidation

- **Goal**: Eliminate component duplication (tag dialogs, audio wrappers) and enforce theme-aware loading skeletons.
- **Files Likely Affected**:
  - [`UnifiedAudioPlayerBar.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/components/UnifiedAudioPlayerBar.kt)
  - [`AudioPlayerBar.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/components/AudioPlayerBar.kt)
  - [`ShimmerSkeletonCard.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/journal/components/ShimmerSkeletonCard.kt)
  - [`InsightsShimmerSkeleton.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/insight/components/InsightsShimmerSkeleton.kt)
  - [`TagActionDialogs.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/components/TagActionDialogs.kt)
- **Risk Level**: Medium.
- **Estimated Complexity**: Medium-High.
- **Manual Testing Checklist**:
  - Verify audio playback controls work seamlessly in Journal feed, Note Detail view, and Calendar mini cards.
  - Verify loading shimmer skeleton cards display warm cream tones in Light Theme and dark charcoal in Dark Theme.
- **Regression Risks**: Broken parameter mapping in legacy component invocation sites.
- **Rollback Strategy**: Restore original component wrappers while preserving bug fixes.
- **Expected Improvements**: Single source of truth for audio playback UI and dynamic light/dark theme skeleton loaders.
- **Dependencies**: Phase 1 & Phase 2.

---

## Phase 4 — Motion System & Transitions

- **Goal**: Standardize animation easing specs, implement layout expansion transitions (`animateContentSize`), and refine screen transition spec in `AppNavHost`.
- **Files Likely Affected**:
  - [`AppNavHost.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/navigation/AppNavHost.kt)
  - [`AudioPlayerFull.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/notedetail/components/AudioPlayerFull.kt)
  - [`MicRecordingPill.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/notedetail/components/MicRecordingPill.kt)
- **Risk Level**: Medium.
- **Estimated Complexity**: Medium.
- **Manual Testing Checklist**:
  - Verify smooth transition between tabs in bottom navigation bar.
  - Verify expanding transcript box slides vertically without screen flickering.
- **Regression Risks**: Unexpected layout jumps during dynamic size changes.
- **Rollback Strategy**: Revert transition specs in `AppNavHost.kt`.
- **Expected Improvements**: Fluid physical motion across modals, tab switching, and expanding cards.
- **Dependencies**: Phase 3.

---

## Phase 5 — Screen Layout Migration

- **Goal**: Align all 10 feature screens to the 5-tier standard layout scaffold (Top bar, Content, FAB, Snackbar, BottomNav).
- **Files Likely Affected**:
  - [`JournalScreen.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/journal/JournalScreen.kt)
  - [`NoteDetailScreen.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/notedetail/NoteDetailScreen.kt)
  - [`CalendarScreen.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/calendar/CalendarScreen.kt)
  - [`InsightScreen.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/insight/InsightScreen.kt)
  - [`SettingsScreen.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/settings/SettingsScreen.kt)
- **Risk Level**: Medium.
- **Estimated Complexity**: High.
- **Manual Testing Checklist**:
  - Verify content does not clip under status bar or bottom navigation bar.
  - Verify bottom list padding allows full scrolling above floating FABs.
- **Regression Risks**: Window insets overlap on device models with camera cutouts.
- **Rollback Strategy**: Revert scaffold changes on individual screens.
- **Expected Improvements**: Standardized window inset handling and predictable layout behavior across all screen views.
- **Dependencies**: Phase 3 & Phase 4.

---

## Phase 6 — Accessibility & Touch Targets

- **Goal**: Enforce 48dp × 48dp minimum touch targets (`minimumInteractiveComponentSize()`) on all chips and interactive controls, and refine TalkBack semantics.
- **Files Likely Affected**:
  - [`TagChip.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/components/TagChip.kt)
  - [`FilterBar.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/journal/components/FilterBar.kt)
  - [`DayCell.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/calendar/components/DayCell.kt)
  - [`WaveformVisualizer.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/components/WaveformVisualizer.kt)
- **Risk Level**: Low.
- **Estimated Complexity**: Medium.
- **Manual Testing Checklist**:
  - Test screen navigation with TalkBack enabled.
  - Verify touch target bounds using Android Accessibility Scanner.
- **Regression Risks**: Slightly larger chip dimensions altering horizontal layout wrapping.
- **Rollback Strategy**: Revert padding and touch target modifier changes on affected chips.
- **Expected Improvements**: Compliance with hardware accessibility standards and frictionless touch interactions.
- **Dependencies**: Phase 5.

---

## Phase 7 — Performance & Lifecycle Optimization

- **Goal**: Replace all `collectAsState()` occurrences with `collectAsStateWithLifecycle()` across composable screens.
- **Files Likely Affected**:
  - All 10 Screen composable files under `dev.voicejournal.ui.*`.
- **Risk Level**: Low.
- **Estimated Complexity**: Medium.
- **Manual Testing Checklist**:
  - Test sending app to background during active flow emissions and verify collection pauses cleanly.
  - Verify state restores cleanly when returning app to foreground.
- **Regression Risks**: State emission delay if lifecycle state mapping is configured incorrectly.
- **Rollback Strategy**: Revert state collection imports back to `collectAsState()`.
- **Expected Improvements**: Eliminates background CPU/battery drain and aligns app with Android lifecycle best practices.
- **Dependencies**: Phase 1 & Phase 5.

---

## Phase 8 — Final Polish & Visual Quality QA

- **Goal**: Perform comprehensive visual regression testing, fine-tune haptic feedback triggers, and verify edge-to-edge rendering in both Dark and Light Premium modes.
- **Files Likely Affected**:
  - Fine-tuning across `ui/` layer.
- **Risk Level**: Very Low.
- **Estimated Complexity**: Medium.
- **Manual Testing Checklist**:
  - Complete end-to-end user journey smoke tests (Record note -> Transcribe -> Add Tags -> Search -> View Insights -> Export/Backup).
  - Verify biometric app lock overlay behavior.
- **Regression Risks**: None.
- **Rollback Strategy**: N/A.
- **Expected Improvements**: Flawless, craftsman-quality user experience ready for production release.
- **Dependencies**: All preceding phases.
