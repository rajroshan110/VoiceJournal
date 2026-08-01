# VoiceJournal — System Standardization Report

## Executive Summary
Rather than treating UI defects as isolated bug fixes, this report reduces **VoiceJournal**'s entire frontend architecture into eight standardized systems. Migrating to these unified systems replaces hundreds of ad-hoc implementations with predictable, maintainable architectural primitives.

---

## 1. Spacing System

### Current State
Paddings, margins, and component gaps use hardcoded arbitrary values scattered across composables (`6.dp`, `10.dp`, `13.dp`, `14.dp`, `15.dp`, `18.dp`, `22.dp`, `34.dp`).

### Target State
A strict 4dp grid design system with standardized spacing tokens: `SpaceXs (4.dp)`, `SpaceSm (8.dp)`, `SpaceMd (12.dp)`, `SpaceLg (16.dp)`, `SpaceXl (24.dp)`.

### Migration Strategy
1. Audit all composable padding/margin declarations.
2. Replace ad-hoc `dp` values with centralized spacing tokens.
3. Enforce spacing linting rules in build pipeline.

---

## 2. Typography System

### Current State
Frequent use of inline font parameter overrides (`fontSize = 12.sp`, `fontSize = 13.sp`, `fontSize = 18.sp`, `fontWeight = FontWeight.Bold`) bypassing Material 3 typography roles.

### Target State
100% adherence to M3 `MaterialTheme.typography` hierarchy (`displayLarge`, `headlineLarge`, `titleMedium`, `bodyMedium`, `labelSmall`) with explicit font scaling support.

### Migration Strategy
1. Refine `Type.kt` to cover all text usage roles across the app.
2. Replace inline `.sp` font size declarations with `MaterialTheme.typography.<role>`.

---

## 3. Color System

### Current State
Multiple composables contain hardcoded hex colors (`Color(0xFF2A2A2A)`, `Color(0xFF666666)`, `Color(0xFFFFD54F)`), breaking Light Theme rendering (e.g. shimmer skeletons rendering dark boxes in Light Mode).

### Target State
Zero hardcoded hex values outside `UiColorTheme.kt`. All UI components draw colors dynamically from `AppTheme.colors` (`DarkColors` & `LightPremiumColors`).

### Migration Strategy
1. Map all hardcoded color instances in code to semantic tokens in `AppColors`.
2. Refactor `ShimmerSkeletonCard` and `InsightsShimmerSkeleton` to use `AppTheme.colors.surfaceVariant`.
3. Add static analysis check forbidding `Color(0xFF...)` outside theme definitions.

---

## 4. Motion System

### Current State
Inconsistent animation durations (`180ms`, `220ms`, `260ms`, `1200ms`) and linear easing specs without standardized spring physics.

### Target State
Standardized motion system with 4 duration tiers (100ms, 200ms, 300ms, 375ms) and 3 core easing curves (`FastOutSlowInEasing`, `LinearOutSlowInEasing`, Spring Damping).

### Migration Strategy
1. Define central motion constants object (`VoiceMotion`).
2. Update `AppNavHost` transitions and card expansion composables (`animateContentSize`) to consume central motion tokens.

---

## 5. Component System

### Current State
Duplicated tag management dialogs (`TagActionDialogs.kt`, `JournalTagsDialog.kt`, `TagEditorSection.kt`) and legacy wrappers (`AudioPlayerBar.kt`).

### Target State
Single-source-of-truth component library (`dev.voicejournal.ui.components.*`) where every visual widget is decoupled and reusable.

### Migration Strategy
1. Deprecate legacy wrappers and consolidate tag creation/editing into `UnifiedTagDialog`.
2. Unify filter bottom sheet implementations across feeds.

---

## 6. State Handling & Lifecycle System

### Current State
All 10 Compose screens collect `StateFlow` using `.collectAsState()`, causing flow collection to remain active in background and drain battery/CPU.

### Target State
Universal use of `.collectAsStateWithLifecycle()` across all screen composables.

### Migration Strategy
1. Add `androidx.lifecycle:lifecycle-runtime-compose` dependency.
2. Replace `.collectAsState()` with `collectAsStateWithLifecycle()` in all screen composables.

---

## 7. Navigation System

### Current State
Ad-hoc backstack manipulation when navigating between nested drawer items and primary bottom tabs.

### Target State
Centralized navigation router with explicit tab index resolution and single-top top-level navigation.

### Migration Strategy
1. Standardize route argument handling in `Screen.kt`.
2. Ensure all tab switches pop up to start destination with state restoration enabled (`restoreState = true`).

---

## 8. Accessibility System

### Current State
Sub-48dp hardware touch targets on chips and inline action icons; inconsistent screen readerTalkBack node descriptions.

### Target State
100% compliance with Android 48dp × 48dp minimum touch target standard and TalkBack node merging semantics.

### Migration Strategy
1. Apply `Modifier.minimumInteractiveComponentSize()` across all chip and icon button components.
2. Conduct TalkBack navigation pass to ensure proper node isolation (`mergeDescendants = false`).
