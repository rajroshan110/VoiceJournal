# VoiceJournal — Motion & Motion System Audit

## Executive Overview
Motion in VoiceJournal should convey physical craftsmanship, liquid audio responsiveness, and spatial continuity. This audit reviews the motion system across screen navigation, component state changes, modal presentations, and micro-interactions.

---

## 1. Motion System Architecture & Easing Specifications

### 1.1 Standard Motion Tokens
Currently, motion parameters are defined ad-hoc across composables with varying durations (`180ms`, `220ms`, `260ms`, `1200ms`). The motion system must standardize on three core easing curves and four duration tiers:

| Motion Role | Duration (ms) | Easing Curve (`AnimationSpec`) | Usage |
| :--- | :--- | :--- | :--- |
| **Instant Micro** | 100ms | `LinearOutSlowInEasing` | Touch press states, checkbox toggles |
| **Fast Component** | 200ms | `FastOutSlowInEasing` | Chip selection, expansion toggles, audio play/pause |
| **Standard Screen** | 300ms | `CubicBezier(0.2f, 0.0f, 0.0f, 1.0f)` | Screen horizontal slide enter/exit |
| **Expressive Modal** | 375ms | `Spring(stiffness = Low, dampingRatio = Medium)` | Bottom sheets, FAB recording pill expansion |

---

## 2. Motion & Animation Audit Findings

### 2.1 Navigation & Screen Transitions
- **Current State**: [`AppNavHost.kt`](file:///Volumes/Work%20Storage/VoiceJournal/app/src/main/java/dev/voicejournal/ui/navigation/AppNavHost.kt#L46-L133) implements custom horizontal slide transitions based on tab index (`getTabIndex()`).
- **Deficiencies & Gaps**:
  - **Shared Element Transitions**: Opening `NoteDetailScreen` from an `EntryCard` on `JournalScreen` uses a generic horizontal slide. There is no shared element transition for the entry card container or image preview, breaking visual continuity.
  - **Back Gesture Spatial Continuity**: Swipe-to-dismiss or system back navigation does not scale down the exiting screen smoothly.

### 2.2 Bottom Sheet & Modal Motion
- **Current State**: Bottom sheets (`FilterSheets.kt`, `BatchCategorizeSheet.kt`, `AddItemSheet.kt`) use default Compose M3 sheet animation specs.
- **Deficiencies & Gaps**:
  - **Scrim Fade Synchronization**: The dark backdrop scrim fades out faster than the sheet slides down, creating a visual flash before dismiss completes.
  - **Drag-to-Dismiss Spring Physics**: Dragging a bottom sheet downward lacks velocity-based spring snapback when released above the dismiss threshold.

### 2.3 Audio & Waveform Visualizer Motion
- **Current State**: `WaveformVisualizer` updates bar heights based on static byte amplitude lists, while `ShimmerSkeletonCard` uses an infinite linear transition (`1200ms`).
- **Deficiencies & Gaps**:
  - **Waveform Seeking Smoothness**: Scrubbing along `WaveformVisualizer` jumps immediately to the new progress fraction without smooth interpolation between amplitude bars.
  - **Recording Pulse Motion**: Mic FAB recording state uses a basic opacity loop rather than a fluid liquid audio wave expansion.

### 2.4 Micro-Interactions & Touch Feedback
- **Current State**: Standard Compose ripples are present on clickable surfaces.
- **Deficiencies & Gaps**:
  - **Missing Haptic Synchronization**: Button presses, recording start/stop, and tag deletion lack synchronized haptic feedback (`LocalHapticFeedback`).
  - **Pin Keypad Motion**: Custom PIN keypad buttons in `CustomPinLockScreen` render no scale compression effect when pressed.

---

## 3. Motion Audit Matrix & Remediation Blueprint

| Component / Flow | Existing Motion | Problem Identified | Target Motion Specification |
| :--- | :--- | :--- | :--- |
| **Card to Note Detail** | Generic horizontal slide | Lacks spatial continuity | Shared Element Transform (`Modifier.sharedElement()`) |
| **Transcript Box** | Instant pop-in | Layout jump | `animateContentSize(tween(250, FastOutSlowInEasing))` |
| **PIN Lock Screen** | Static dot fill | Instant feedback | Scale animation + Error Shake keyframe transition |
| **Mic FAB Record** | Static color change | Unclear recording status | Spring expansion into full-width `MicRecordingPill` |
| **Filter Sheet** | Linear slide | Stiff dismissal | `spring(dampingRatio = DampingRatioLowBouncy)` |
