# VoiceJournal — Non-Negotiable UI Architecture Principles

## Overview
This document outlines the 15 non-negotiable engineering and design rules for **VoiceJournal**. Every future pull request, component modification, and feature implementation MUST strictly adhere to these principles.

---

## The 15 Non-Negotiable Principles

### 1. One Primary Action Per Screen
Every screen MUST have exactly one visually dominant primary action (e.g. Mic FAB on `JournalScreen`, Save/Record action on `NoteDetailScreen`). Secondary actions must use outlined or text-only button variants to maintain visual hierarchy.

### 2. Zero Magic Spacing Values
All paddings, margins, spacers, and component dimensions MUST consume spacing tokens from `Spacing.kt` (`SpaceXs: 4.dp`, `SpaceSm: 8.dp`, `SpaceMd: 12.dp`, `SpaceLg: 16.dp`, `SpaceXl: 24.dp`). Hardcoding raw `dp` values outside `Spacing.kt` is strictly prohibited.

### 3. Zero Hardcoded Colors
All color declarations MUST reference `AppTheme.colors`. Direct instantiation of hex colors (e.g., `Color(0xFF...)`) outside `UiColorTheme.kt` is strictly forbidden.

### 4. Zero Hardcoded Typography Overrides
All text composables MUST consume Material 3 typography tokens from `MaterialTheme.typography` (`displayLarge`, `titleMedium`, `bodyMedium`, `labelSmall`). Inline `fontSize = X.sp` overrides outside `Type.kt` are prohibited.

### 5. Instant Tactile & Visual Feedback
Every human interaction (taps, drags, long-presses, toggles) MUST trigger immediate visual state changes AND haptic feedback ticks via `LocalHapticFeedback`.

### 6. Mandatory Destructive Confirmation
Every permanent delete or wipe action MUST require explicit user confirmation via a modal alert dialog or bottom sheet prompt. Destructive buttons MUST use `AppTheme.colors.error`.

### 7. Skeleton Loaders For All Async States
Every data fetching or audio processing state MUST render a skeleton shimmer loader (`ShimmerSkeletonCard`, `InsightsShimmerSkeleton`) matching the exact geometry of the content surface. Blank screens or basic centered spinner indicators are prohibited for list loads.

### 8. Actionable Empty States
Every empty list or search result state MUST include a friendly illustration/icon, a clear explanation headline, AND a primary call-to-action button (e.g. "Record Your First Voice Note").

### 9. Seamless Dual-Theme Parity
Every component and screen MUST be tested and verified to render flawlessly in both **Dark Obsidian** mode and **Light Premium Paper** mode without contrast loss or visual artifacts.

### 10. Unified Motion System Navigation
Every screen transition, sheet presentation, and dialog popup MUST consume motion specs directly from `ANIMATION_CATALOG.md` (`NavigationMotion` / `MotionSpecs`). Custom ad-hoc `tween()` durations are forbidden.

### 11. Minimum 48dp Hardware Touch Targets
Every interactive control (buttons, chips, icon toggles, delete icons) MUST guarantee a minimum hardware touch target of **48dp × 48dp** using `Modifier.minimumInteractiveComponentSize()`.

### 12. Zero Component Duplication
Never recreate existing UI widgets. If a component (such as tag chips, audio player bars, or search headers) exists in `ui/designsystem/components/`, it MUST be reused.

### 13. Single-Instance Audio Lifecycle Control
Only one audio track may play at any time across the entire application. Navigating away from a screen or starting playback on another item MUST automatically pause existing playback via `AudioPlaybackManager` and `DisposableEffect`.

### 14. Mandatory Motion Catalog Registration
No animation or transition spec may be added to code without first being documented and cataloged in `ANIMATION_CATALOG.md`.

### 15. Standardized 5-Tier Screen Scaffold
All feature screens MUST inherit the 5-tier standard layout structure (Top Bar, Content, FAB, Snackbar, BottomNav) with clean status/navigation window insets handling.
