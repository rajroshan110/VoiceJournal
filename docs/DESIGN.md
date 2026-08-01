# VoiceJournal — Design System Specification

## 1. Design Philosophy

VoiceJournal is a contemplative, privacy-first audio and text journaling platform. Its design philosophy balances high-speed voice capture with deep personal reflection.

### Personality & Emotional Goals
- **Calm & Unhurried**: The app acts as a serene sanctuary for personal thoughts. It reduces cognitive noise and encourages quiet introspection.
- **Intimate & Personal**: The visual presentation feels private, organic, and personal—resembling a physical leather-bound journal or high-end paper notebook digitized for touch interfaces.
- **Lightning-Fast Capture**: Recording a voice note or jotting down a thought requires zero friction. The recording experience must trigger instantly without waiting for animations to settle.
- **Premium Craftsman Quality**: High tactile quality achieved through curated color palettes, elegant serif/sans-serif typography pairing, soft glassmorphic layers, and micro-haptics.
- **Focused & Intensional**: Everything on screen serves a clear purpose. No clutter, zero ads, no social metrics, and no gamification pressure.

### What VoiceJournal Should Feel Like
- Soft ambient paper textures (in Light Premium mode) or deep charcoal obsidian surfaces (in Dark mode).
- Precise, responsive tactile feedback—like pressing mechanical record buttons or sliding physical audio faders.
- Smooth, natural motion with physical spring dynamics.

### What VoiceJournal Should NEVER Feel Like
- **Social Media Apps**: No likes, follower counts, streak badges, public share buttons, or feed notifications.
- **Corporate Productivity Suite**: No dense data tables, heavy borders, sterile grey forms, or complex enterprise toolbars.
- **Playful / Childish Games**: Avoid bright neon pop colors, cartoonish badges, bouncy confetti, or casual playful fonts.
- **Cluttered Utility Apps**: Avoid crammed settings menus, redundant navigation bars, or intrusive pop-up dialogs.

---

## 2. Visual Language

### 2.1 Color Philosophy
VoiceJournal operates with a strict dual-theme model: **Dark Obsidian** (default) and **Light Premium Paper**. Colors express semantic meaning, audio state, and emotional warmth.

#### Primary & Accent Palette
- **Voice Primary (Olive Green)**: `#8B9A46` — Used for main action triggers, active waveform progress, selected tab indicators, and recording activity heatmaps.
- **Primary Container (Dark)**: `#262C18` — Subtle tint behind active chips and voice player containers.
- **Primary Container (Light)**: `#F5E6E3` — Soft warm tint for selected elements in Light Premium theme.
- **Light Primary (Deep Crimson)**: `#6B1100` — Primary accent for Light Premium theme.
- **Accent Yellow (Warning / Permission)**: `#FFD54F` — Used for recording permissions warnings and non-destructive alerts.
- **Destructive Red**: `#CF6679` (Dark) / `#D32F2F` (Light) — Strictly reserved for permanent delete actions and error states.

#### Dark Theme Surface Tokens
- **App Background**: `#121212` (Deep Charcoal Black)
- **Secondary Background**: `#181818` (Bar and Drawer fill)
- **Card Surface**: `#1E1E1E` (Elevated Card fill)
- **Component Surface**: `#242424` (Chip & Audio Bar containers)
- **Border / Divider**: `#333333` / `#2E2E2E`

#### Light Premium Surface Tokens
- **App Background**: `#F9F2E9` (Warm Cream / Soft Parchment)
- **Secondary Background**: `#FFF8F1`
- **Card Surface**: `#FFFFFF`
- **Component Surface**: `#FDFBF8`
- **Border / Divider**: `#EFE7DD` / `#E8DED2`

#### Typography Color Roles
- **Text High Emphasis**: `#FFFFFF` (Dark) / `#1C1B1F` (Light)
- **Text Medium Emphasis**: `#A0A0A0` (Dark) / `#796E65` (Light)
- **Text Disabled / Muted**: `#666666` (Dark) / `#B0A8A0` (Light)

---

### 2.2 Typography Hierarchy
VoiceJournal pairs clean modern sans-serif fonts (**Inter**) for body text and navigation UI with expressive serif typography (**Newsreader** / **Playfair Display**) for dates, journal titles, and reflection headers.

| Role | Font Family | Weight | Size (sp) | Line Height (sp) | Letter Spacing | Purpose |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `displayLarge` | Inter / Sans | Bold | 57 | 64 | -0.25sp | Splash & Hero Stats |
| `headlineLarge` | Serif Accent | SemiBold | 32 | 40 | 0sp | Date Headers & Reflection Titles |
| `headlineMedium` | Serif Accent | SemiBold | 28 | 36 | 0sp | Note Detail Main Titles |
| `titleLarge` | Inter / Sans | SemiBold | 22 | 28 | 0sp | Screen Top Bar Titles ("Journal", "Insights") |
| `titleMedium` | Inter / Sans | Medium | 16 | 24 | 0.15sp | Section Headers, Card Titles |
| `bodyLarge` | Inter / Sans | Regular | 16 | 24 | 0.5sp | Main Note Editor Text |
| `bodyMedium` | Inter / Sans | Regular | 14 | 20 | 0.25sp | Card Body Previews, Transcripts |
| `bodySmall` | Inter / Sans | Medium | 12 | 16 | 0.4sp | Timestamps, Audio Counters |
| `labelLarge` | Inter / Sans | Medium | 14 | 20 | 0.1sp | Button Text, Filter Chips |
| `labelMedium` | Inter / Sans | Medium | 12 | 16 | 0.5sp | Tag Badges, Navigation Labels |
| `labelSmall` | Inter / Sans | Medium | 11 | 16 | 0.5sp | Overflow Counters (`+3 more`) |

---

### 2.3 Spacing, Elevation & Corner Radii

#### 8dp/4dp Spacing Grid
All paddings, margins, and component dimensions MUST be multiples of 4dp, adhering strictly to the spacing grid:
- **3xs**: `2dp` (Micro spacing between icon and text)
- **2xs**: `4dp` (xs - Minimal chip inner padding, small vertical gaps)
- **xs**: `8dp` (sm - Standard gap between chips, inner card elements)
- **md**: `12dp` (Medium card padding, list item gaps)
- **lg**: `16dp` (Standard screen horizontal padding, card content padding)
- **xl**: `24dp` (Section spacing, drawer padding)
- **2xl**: `32dp` (Hero header spacing, empty state top spacing)

#### Corner Radii Specification
- **Small Components (Inputs, Thumbnails, Badges)**: `8dp`
- **Medium Components (Entry Cards, Dialog Containers)**: `12dp` / `14dp`
- **Pill Components (Chips, Filter Buttons, Audio Bars)**: `20dp` / `24dp` / `Full Circle (50%)`
- **Bottom Sheets**: `28dp` top-left and top-right rounded corners.

#### Elevation & Shadows
- **Level 0 (Flat Surfaces)**: Backgrounds, list item cards (bordered with `1.dp`).
- **Level 1 (Elevated Cards)**: Entry cards in hover/selection state (`2.dp` shadow in Light Mode, border highlight in Dark Mode).
- **Level 2 (Floating Controls)**: Mic FAB, Floating Audio Bar (`6.dp` elevation).
- **Level 3 (Modal Surfaces)**: Bottom Sheets and Dialogs (`16.dp` elevation with `#000000` 60% scrim).

---

### 2.4 Iconography, Imagery & Media
- **Iconography**: Outline Material Symbols (24dp default, 20dp inline) with a consistent line weight of 2dp. Active states transition smoothly to filled variants or primary color tints.
- **Media Previews**: Image thumbnails embedded inside journal entries rendered at a fixed aspect ratio of 16:9 or square 44dp thumbnails with `8dp` corner rounding.
- **Empty States**: Minimal vector illustration or clean iconography surrounded by a soft primary container circle, paired with a clear headline ("No voice notes yet") and a secondary action prompt.
- **Loading States**: Shimmer skeleton loaders matching the exact geometric layout of content cards, transitioning smoothly via alpha crossfades when data resolves.

---

## 3. Component Philosophy

### 3.1 Entry Card (`EntryCard`)
- **Purpose**: Displays a summary preview of a journal entry within the main list.
- **Usage**: Used in `JournalScreen`, `ArchiveScreen`, `TrashScreen`, `DraftScreen`.
- **Variations**: Standard Card, Compact Card (for Calendar daily view), Multi-Select Active Card.
- **DO**:
  - Show the explicitly written title/header line first; fallback to text snippet or transcript.
  - Render up to 3 voice tracks inline via `UnifiedAudioPlayerBar`.
  - Format timestamps according to the user's localized time format preference.
- **DON'T**:
  - Truncate tag chips abruptly without showing an overflow counter (`+2`).
  - Allow inner buttons (play, image click) to swallow card long-press selection events.

### 3.2 Unified Audio Player Bar (`UnifiedAudioPlayerBar`)
- **Purpose**: Standardized playback interface for voice recordings.
- **Usage**: Used across Entry Cards, Note Detail view, Calendar Mini Cards, and Floating Player.
- **Variations**: Full (with time readout and waveform seek), Compact, Inline Editor Bar.
- **DO**:
  - Display dynamic 32-bar waveform canvas with active/inactive state coloring.
  - Support touch/drag scrubbing along the waveform.
  - Automatically pause playback when navigating away from the screen (`DisposableEffect`).
- **DON'T**:
  - Allow multiple audio bars across the app to play simultaneously.
  - Hardcode height below 44dp.

### 3.3 Tag & Filter Chips (`TagChip` & `FilterBar`)
- **Purpose**: Categorize notes by Topic (`#tag`), Person (`@mention`), Mood (`emoji`), or Folder (`📁`).
- **Variations**: Filter Bar Chip, Entry Card Tag Badge, Editable Deletable Tag Chip.
- **DO**:
  - Prefix tags with intuitive icons (`🏷️`, `👤`, `😌`, `📁`).
  - Enforce minimum 48dp × 48dp hardware touch target size.
- **DON'T**:
  - Allow long tag names to wrap to multiple lines inside a single chip.

### 3.4 Floating Action Button (`MicFab`)
- **Purpose**: Primary trigger for instantaneous voice recording.
- **Usage**: Fixed at the bottom-right or bottom-center of `JournalScreen` and `NoteDetailScreen`.
- **Variations**: Idle Mic Button, Recording Pulsing State, Expanded Recording Pill.
- **DO**:
  - Provide immediate haptic feedback when pressed.
  - Display live audio amplitude meter ring or pulsing visual during active recording.
- **DON'T**:
  - Obscure the bottom entry card or main navigation bar without proper content padding (`100.dp`).

---

## 4. Standard Screen Structure & Layout Scaffold

All screen implementations MUST follow a standardized 5-tier layout hierarchy:

```
+-------------------------------------------------------+
| 1. Top App Bar / Search Bar Header                    |
+-------------------------------------------------------+
| 2. Sticky Sub-Header / Filter Bar (Optional)          |
+-------------------------------------------------------+
| 3. Main Content (LazyColumn / Grid / Custom Scroll)   |
|                                                       |
|                                                       |
+-------------------------------------------------------+
| 4. Floating Overlay (Mic FAB / Audio Player Bar)      |
+-------------------------------------------------------+
| 5. Root Bottom Navigation Bar (MainActivity level)    |
+-------------------------------------------------------+
```

### Layout Rules:
1. **Single Bottom Navigation Bar**: Only `MainActivity` renders the root `BottomNavBar`. Individual sub-screens MUST NOT instantiate their own bottom bars.
2. **Window Insets Management**: Every screen root container must handle status bar and navigation bar insets cleanly using `Modifier.windowInsetsPadding()`.
3. **Bottom Content Padding**: All scrollable lists must apply `contentPadding = PaddingValues(bottom = 88.dp)` to ensure content is fully scrollable above floating FABs and bottom bars.

---

## 5. Design Tokens Summary

```kotlin
// Spacing Tokens
val SpaceXs = 4.dp
val SpaceSm = 8.dp
val SpaceMd = 12.dp
val SpaceLg = 16.dp
val SpaceXl = 24.dp

// Radius Tokens
val RadiusSm = 8.dp
val RadiusMd = 12.dp
val RadiusLg = 14.dp
val RadiusPill = 24.dp
val RadiusSheet = 28.dp

// Touch Target
val MinTouchTarget = 48.dp

// Animation Spec Tokens
val AnimFast = 150 // ms
val AnimNormal = 250 // ms
val AnimSlow = 350 // ms
```

---

## 6. Future Contributor Rules

1. **Zero Hardcoded Hex Colors**: All colors MUST reference `AppTheme.colors`. Direct use of `Color(0xFF...)` outside `UiColorTheme.kt` is strictly prohibited.
2. **Mandatory Touch Targets**: All interactive composables MUST apply `Modifier.minimumInteractiveComponentSize()` or ensure a 48dp minimum bounding box.
3. **Lifecycle State Collection**: All state flows collected in composables MUST use `collectAsStateWithLifecycle()`.
4. **Single-Instance Audio Control**: Any component initializing media playback MUST register with `AudioPlaybackManager`.
