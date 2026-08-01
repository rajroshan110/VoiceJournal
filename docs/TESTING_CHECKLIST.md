# VoiceJournal — Quality Assurance & Testing Checklist

## Overview
This document contains the mandatory testing checklists required before any pull request or refactoring phase in [`REFACTOR_ROADMAP.md`](file:///Volumes/Work%20Storage/VoiceJournal/docs/REFACTOR_ROADMAP.md) can be marked complete.

---

## Master Verification Quality Criteria

Every component and screen modification must satisfy 6 core quality domains:

```
[ Visual QA ] -> [ Interaction & Motion QA ] -> [ Accessibility & Touch QA ]
                                                            │
[ Performance & Lifecycle QA ] <- [ Audio Engine & State QA ] <- [ Theme & Device QA ]
```

---

## Phase-by-Phase Testing Checklists

### Phase 1 — Design Tokens & Dependencies

#### Visual & Theme QA
- [ ] Verify `DarkColors` background is `#121212` and primary is `#8B9A46`.
- [ ] Verify `LightPremiumColors` background is `#F9F2E9` and primary is `#6B1100`.
- [ ] Confirm `AppTheme.colors` resolves cleanly in Compose previews.

#### Build & Regression QA
- [ ] Build succeeds with `./gradlew assembleDebug`.
- [ ] Gradle dependency tree resolves `androidx.lifecycle:lifecycle-runtime-compose` without conflicts.

---

### Phase 2 — Typography & Spacing System

#### Spacing & Grid QA
- [ ] All paddings/margins use tokens from `Spacing.kt` (`SpaceXs`, `SpaceSm`, `SpaceMd`, `SpaceLg`, `SpaceXl`).
- [ ] Zero hardcoded `dp` spacing values remain in modified files.

#### Typography QA
- [ ] Text composables consume `MaterialTheme.typography` styles.
- [ ] Zero inline `fontSize = X.sp` parameter overrides remain outside `Type.kt`.
- [ ] Verify font scaling under system accessibility settings (100%, 130%, 150% text size).

---

### Phase 3 — Component Standardization

#### Component Visual & Theme QA
- [ ] `ShimmerSkeletonCard` and `InsightsShimmerSkeleton` display warm cream tones in Light Theme and dark charcoal in Dark Theme.
- [ ] `TagChip` displays correct icon prefixes (`🏷️`, `👤`, `😌`, `📁`).

#### Audio Engine QA
- [ ] `UnifiedAudioPlayerBar` waveform renders 32 dynamic amplitude bars.
- [ ] Touch/drag scrubbing along waveform visualizer seeks audio position correctly.
- [ ] Single-instance playback enforced: playing audio on item #2 automatically pauses item #1.

---

### Phase 4 — Motion System & Shared Transitions

#### Motion & Animation QA
- [ ] All transition specs match durations defined in `ANIMATION_CATALOG.md`.
- [ ] Tab switching in root bottom navigation uses `NavigationMotion.TabSwitch` (260ms `FastOutSlowInEasing`).
- [ ] Detail screen transition uses `NavigationMotion.ScreenPush` (300ms slide + fade).
- [ ] Transcript box expansion uses `animateContentSize` vertical transition without layout flickering.

---

### Phase 5 — Screen Migration & Layout Scaffold

#### Layout & Inset QA
- [ ] All 10 screens inherit 5-tier standard layout scaffold.
- [ ] Edge-to-edge status bar and navigation bar window insets render without content overlap.
- [ ] Bottom list padding (`88.dp`) allows full scrolling above floating Mic FAB.

#### State Restoration & Navigation QA
- [ ] Backstack navigation clears correctly when switching root tabs in `BottomNavBar`.
- [ ] Screen state restores cleanly after device rotation.
- [ ] Process death restoration verified via Android Studio Don't Keep Activities setting.

---

### Phase 6 — Accessibility & Touch Targets

#### Accessibility & Hardware Touch QA
- [ ] All interactive controls (buttons, chips, icon toggles, chip delete icons) enforce a minimum **48dp × 48dp** touch target via `minimumInteractiveComponentSize()`.
- [ ] Verified using Android Accessibility Scanner app with zero touch target warnings.
- [ ] TalkBack screen reader navigation pass: content descriptions present and accurate on all icons.
- [ ] Parent cards apply `semantics(mergeDescendants = false)` for independent node navigation.

---

### Phase 7 — Performance & Lifecycle Optimization

#### Lifecycle & Performance QA
- [ ] All 10 Compose screens collect state flows via `collectAsStateWithLifecycle()`.
- [ ] Sending app to background pauses StateFlow collection without CPU/battery drain.
- [ ] Zero unnecessary recompositions during audio playback waveform updates (verified via Compose Layout Inspector).

---

### Phase 8 — Final Polish & End-to-End Regression

#### End-to-End Journey QA
- [ ] **Recording Journey**: Tap Mic FAB -> Record audio -> Stop -> Transcript generates -> Save note.
- [ ] **Browsing Journey**: Filter by `#tag`, `@person`, or `mood` -> Search query -> Open note detail -> Edit title -> Save.
- [ ] **Security Journey**: Enable Biometric/PIN Lock in Settings -> Send app to background -> Re-open app -> Lock screen overlay prompts for authentication cleanly.
- [ ] **Small-Screen Device QA**: Verified on 4.7-inch small screen device without text truncation or overflow clipping.
