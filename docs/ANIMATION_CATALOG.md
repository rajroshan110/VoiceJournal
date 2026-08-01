# VoiceJournal — Animation & Motion Catalog

## Overview
This document defines every motion and animation allowed within **VoiceJournal**. No animation, transition, or duration spec may exist outside this catalog.

---

## Motion Token Specification (`MotionSpecs.kt`)

```kotlin
object MotionSpecs {
    // Duration Tiers
    const val DurationInstant = 100 // ms
    const val DurationFast = 200 // ms
    const val DurationStandard = 300 // ms
    const val DurationExpressive = 375 // ms
    const val DurationShimmer = 1200 // ms

    // Easing Curves
    val EasingStandard = FastOutSlowInEasing
    val EasingExit = FastOutLinearInEasing
    val EasingEnter = LinearOutSlowInEasing
    val SpringBouncy = spring<Float>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
    val SpringSmooth = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
}
```

---

## Motion Catalog Index

### 1. Navigation & Screen Transitions

#### Screen Push (Forward Navigation)
- **Purpose**: Transitions from parent list view into detail view (e.g. `EntryCard` to `NoteDetailScreen`).
- **Where Used**: `AppNavHost.kt` (`NoteDetail` route enter)
- **Duration**: `300ms`
- **Delay**: `0ms`
- **Easing**: `FastOutSlowInEasing`
- **Motion Token**: `NavigationMotion.ScreenPush`
- **Spec**: Horizontal slide from 35% right offset (`+35% x`) combined with fade-in (`0f` -> `1f`).

#### Screen Pop (Back Navigation)
- **Purpose**: Returns from detail view to parent list view.
- **Where Used**: `AppNavHost.kt` (`NoteDetail` route pop exit)
- **Duration**: `220ms`
- **Delay**: `0ms`
- **Easing**: `FastOutLinearInEasing`
- **Motion Token**: `NavigationMotion.ScreenPop`
- **Spec**: Horizontal slide to 35% right offset (`+35% x`) combined with fade-out (`1f` -> `0f`).

#### Tab Switch (Root Bottom Navigation)
- **Purpose**: Swapping between root tabs (`Journal`, `Calendar`, `Insight`).
- **Where Used**: `AppNavHost.kt` (Root tab enter/exit)
- **Duration**: `260ms`
- **Delay**: `0ms`
- **Easing**: `FastOutSlowInEasing`
- **Motion Token**: `NavigationMotion.TabSwitch`
- **Spec**: Directional horizontal slide based on tab index offset combined with subtle crossfade.

---

### 2. Modal & Sheet Animations

#### Bottom Sheet Present & Dismiss
- **Purpose**: Sliding bottom sheets (`FilterSheets`, `BatchCategorizeSheet`, `AddItemSheet`).
- **Where Used**: All Modal Bottom Sheet composables
- **Duration**: `375ms`
- **Delay**: `0ms`
- **Easing**: `SpringBouncy`
- **Motion Token**: `MotionSpecs.SheetSpring`
- **Spec**: Vertical slide from bottom with spring damping ratio `LowBouncy` and scrim alpha fade (`0.6f` -> `0f`).

---

### 3. Component Expansion & State Changes

#### Card Expand / Collapse
- **Purpose**: Smoothly resizing card height when inline content changes.
- **Where Used**: `EntryCard`, `AudioPlayerFull`
- **Duration**: `250ms`
- **Delay**: `0ms`
- **Easing**: `FastOutSlowInEasing`
- **Motion Token**: `MotionSpecs.CardExpand`
- **Spec**: Vertical layout bounds animation using `Modifier.animateContentSize(tween(250, FastOutSlowInEasing))`.

#### FAB Recording Pill Expand
- **Purpose**: Morphing idle circular `MicFab` into full-width active `MicRecordingPill`.
- **Where Used**: `JournalScreen`, `NoteDetailScreen`
- **Duration**: `300ms`
- **Delay**: `0ms`
- **Easing**: `SpringSmooth`
- **Motion Token**: `MotionSpecs.FabExpand`
- **Spec**: Shape morphing and width expansion from 56dp circle to full rounded pill container.

#### Transcript Expand / Collapse
- **Purpose**: Expanding speech-to-text transcript box under audio player bar.
- **Where Used**: `AudioPlayerFull.kt`
- **Duration**: `250ms`
- **Delay**: `0ms`
- **Easing**: `FastOutSlowInEasing`
- **Motion Token**: `MotionSpecs.TranscriptExpand`
- **Spec**: Vertical expand/shrink transition using `AnimatedVisibility(expandVertically() + fadeIn())`.

---

### 4. Dynamic Feedback Animations

#### Recording Pulse
- **Purpose**: Live audio recording indication on mic pill.
- **Where Used**: `MicFab`, `MicRecordingPill`
- **Duration**: `1000ms` (Infinite loop)
- **Delay**: `0ms`
- **Easing**: `LinearEasing`
- **Motion Token**: `MotionSpecs.RecordingPulse`
- **Spec**: Dynamic ring scale (`1.0f` -> `1.25f`) and alpha pulse (`0.8f` -> `0.2f`).

#### Loading Shimmer Translate
- **Purpose**: Skeleton card loading effect while notes or insights data resolves.
- **Where Used**: `ShimmerSkeletonCard`, `InsightsShimmerSkeleton`
- **Duration**: `1200ms` (Infinite loop)
- **Delay**: `0ms`
- **Easing**: `LinearEasing`
- **Motion Token**: `MotionSpecs.ShimmerTranslate`
- **Spec**: Brush linear gradient translation from offset `-200f` to `1000f`.

#### Selection Checkmark Pop
- **Purpose**: Visual checkmark indicator when entry card is selected in multi-select mode.
- **Where Used**: `EntryCard.kt`
- **Duration**: `150ms`
- **Delay**: `0ms`
- **Easing**: `SpringBouncy`
- **Motion Token**: `MotionSpecs.SelectionPop`
- **Spec**: Scale animation from `0.6f` to `1.0f` on selection badge.

#### PIN Keypad Button Press
- **Purpose**: Tactile compression effect on PIN lock screen number buttons.
- **Where Used**: `CustomPinLockScreen` in `MainActivity.kt`
- **Duration**: `100ms`
- **Delay**: `0ms`
- **Easing**: `LinearOutSlowInEasing`
- **Motion Token**: `MotionSpecs.KeypadPress`
- **Spec**: Button surface scale compression (`1.0f` -> `0.92f` -> `1.0f`) paired with haptic click.

#### Delete & Dismiss Item
- **Purpose**: Removing an entry or tag item from a list.
- **Where Used**: `JournalScreen`, `TrashScreen`, `TagsScreen`
- **Duration**: `200ms`
- **Delay**: `0ms`
- **Easing**: `FastOutLinearInEasing`
- **Motion Token**: `MotionSpecs.DeleteDismiss`
- **Spec**: Item fade-out (`1f` -> `0f`) combined with height collapse (`shrinkVertically()`).
