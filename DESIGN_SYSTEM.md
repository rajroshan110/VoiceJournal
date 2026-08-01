# VoiceJournal Design System

This document outlines the visual design system, UI components, typography, color palette, spacing rules, and global layout standards for VoiceJournal, inspired by modern minimal journaling apps (*Daily_You* and *June*).

---

## 1. Color Palette

### Primary & Accent Colors
- **Voice Primary (Olive Green)**: `#8B9A46` (Used for primary buttons, active chips, waveform progress, selected tab indicators)
- **Primary Container**: `rgba(139, 154, 70, 0.25)`
- **Accent Yellow (Warning/Permission)**: `#FFD54F`

### Dark Theme Surfaces
- **App Background**: `#121212` (Deep Charcoal Black)
- **Card Surface**: `#1E1E1E` (Elevated Card Fill)
- **Component Surface**: `#242424` (Chip & Bar Containers)
- **Card Outline Border**: `#333333` / `#2A2A2A`

### Typography Colors
- **Text High Emphasis**: `#E8E8E8` / `#FFFFFF`
- **Text Medium Emphasis**: `#A0A0A0` / `#B0B0B0`
- **Text Disabled / Muted**: `#666666` / `#888888`

---

## 2. Typography Hierarchy

Utilizes Material 3 Typography with Google Fonts / Serif accents for dates & titles:

- **Title Large**: `22sp`, Bold (Top App Bar "Journal" title)
- **Title Medium**: `16sp`, SemiBold (Section headers, empty state titles)
- **Body Medium**: `14sp`, Regular, Line Height `20sp` (Note content preview)
- **Body Small**: `12sp` / `13sp` (Date/Time readouts, audio player timestamps)
- **Label Small**: `11sp`, Medium (Badges, tag overflow counters)

---

## 3. Spacing, Elevation & Corner Radii

### Spacing Grid
- **xs**: `4dp`
- **sm**: `8dp`
- **md**: `12dp`
- **lg**: `16dp`
- **xl**: `24dp`

### Corner Radii
- **Cards**: `12dp`
- **Filter & Tag Chips**: `20dp` / `24dp`
- **Thumbnails & Inputs**: `8dp`
- **Modal Sheets**: `28dp` top rounded corners

### Minimum Touch Targets
- All interactive controls (IconButtons, FilterChips, TagChips, Navigation Items, FAB) MUST enforce a minimum hardware touch target of **48dp × 48dp** using `Modifier.minimumInteractiveComponentSize()`.

---

## 4. Reusable UI Components

1. `JournalHeader`: Minimalist M3 `TopAppBar` with integrated search bar toggle.
2. `FilterBar`: Horizontal scrollable row with `[All]`, `[Tags]`, `[People]`, `[Mood]` chips displaying numeric active badges.
3. `EntryCard`: Main journal entry item card with dynamic locale date/time formatting, 16:9 media previews, text preview preference (Header first), 32-bar `WaveformVisualizer`, and tag chips.
4. `WaveformVisualizer`: Interactive 32-bar amplitude canvas with drag/tap seek gestures and TalkBack accessibility semantics.
5. `FilterSheets`: Modal bottom sheets for Tags (multi-select `#tags`), People (multi-select `@mentions`), and Mood (5 emotion emojis).
6. `ShimmerSkeletonCard`: 3× animated loading skeleton cards.
7. `LightboxDialog`: Full-screen modal media viewer.

---

## 5. Global UI & Behavior Rules

1. **Header Preference in Note Previews**: Note previews on cards MUST show the explicitly written header line first (e.g. line starting with `#` or top title line). Show body text content only if no header line exists.
2. **Tag vs Person Mentions**:
   - Mentions starting with `@` (e.g., `@Alex`) are categorized under **People**.
   - Mentions starting with `#` or manual tags are categorized under **Tags**.
   - Mood filter is strictly limited to the 5 core note emojis (`😊`, `😄`, `😐`, `😔`, `😭`).
3. **Single Bottom Navigation Bar**: Only `MainActivity` renders the root `BottomNavBar`. Individual screens MUST NOT embed duplicate bottom navigation bars.
4. **Minimal Top Bar Padding**: Top bars MUST avoid redundant status bar padding when rendered inside nested scaffolding.
5. **Audio Lifecycle Management**:
   - Single-instance playback across the app.
   - Playback MUST automatically stop when leaving a screen or switching tabs (`DisposableEffect`).
6. **TalkBack Node Isolation**: Parent cards MUST apply `semantics(mergeDescendants = false)` so child controls are navigated independently by screen readers.
