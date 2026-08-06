# Handoff Report: UI State Persistence Audit & Classification (`remember` to `rememberSaveable`)

## 1. Observation

### Audit Context & Mandate
- **Original User Request**: Migrate `remember { mutableStateOf(...) }` to `rememberSaveable` for persistent UI state in the VoiceJournal Compose UI, adhering strictly to constraints (R1: Targeted Migration, R2: Strict Scope Limits - max 10 usages across max 5 files, R3: Safe Environment).
- **Audit Report Baseline**: Audit Report (`media_1786046209418.txt`, Finding #14: Compose performance and state) highlighted that *"Important local state uses `remember` rather than `rememberSaveable`... dialogs and user input are lost on recreation."*

### Search Findings & Occurrences Across Codebase
A full codebase search was performed for `remember` and `mutableStateOf` / `mutableIntStateOf` / `mutableLongStateOf` / `mutableFloatStateOf` across `/Volumes/Work Storage/VoiceJournal/app/src/main/java`.

#### Summary of All Identified Occurrences in UI Layer & Activity:

1. **`MainActivity.kt`**:
   - Line 65: `var isAppUnlocked by remember { mutableStateOf(false) }` (Boolean)
   - Line 66: `var isInitialCheckDone by remember { mutableStateOf(false) }` (Boolean)
   - Line 259: `var enteredPin by remember { mutableStateOf("") }` (String, in `CustomPinLockScreen`)
   - Line 260: `var isError by remember { mutableStateOf(false) }` (Boolean, in `CustomPinLockScreen`)

2. **`JournalScreen.kt`**:
   - Line 128: `var activeSheet by remember { mutableStateOf(ActiveSheet.NONE) }` (Enum `ActiveSheet`)
   - Line 129: `var selectedLightboxImage by remember { mutableStateOf<String?>(null) }` (String?)
   - Line 130: `var showDeleteDialog by remember { mutableStateOf(false) }` (Boolean)
   - Line 131: `var showCategorizeSheet by remember { mutableStateOf(false) }` (Boolean)
   - Line 144: `var prevSortOption by remember { mutableStateOf<SortOption?>(null) }` (Enum `SortOption?`)
   - Line 162: `var pendingNavEntryId by remember { mutableStateOf<Long?>(null) }` (Long?)

3. **`NoteDetailScreen.kt`**:
   - Line 75: `var showTagsDialog by remember { mutableStateOf(false) }` (Boolean)
   - Line 76: `var showDatePicker by remember { mutableStateOf(false) }` (Boolean)
   - Line 77: `var datePickerTab by remember { mutableIntStateOf(0) }` (Int)
   - Line 78: `var showAddItemSheet by remember { mutableStateOf(false) }` (Boolean)
   - Line 79: `var showDeleteConfirmDialog by remember { mutableStateOf(false) }` (Boolean)
   - Line 80: `var showDeleteSelectedTracksDialog by remember { mutableStateOf(false) }` (Boolean)
   - Line 81: `var showArchiveConfirmDialog by remember { mutableStateOf(false) }` (Boolean)
   - Line 83: `var selectedLightboxImage by remember { mutableStateOf<String?>(null) }` (String?)
   - Line 85: `var lastBackPressTime by remember { mutableLongStateOf(0L) }` (Long)
   - Line 86: `var showUnsavedPromptDialog by remember { mutableStateOf(false) }` (Boolean)
   - Line 87: `var showDiscardRecordingConfirmDialog by remember { mutableStateOf(false) }` (Boolean)
   - Line 155: `var currentPhotoFile by remember { mutableStateOf<File?>(null) }` (File?)

4. **`FoldersScreen.kt`**:
   - Line 52: `var selectedLightboxImage by remember { mutableStateOf<String?>(null) }` (String?)
   - Line 53: `var isSearchVisible by remember { mutableStateOf(false) }` (Boolean)
   - Line 332: `var folderNameInput by remember { mutableStateOf("") }` (String, in Create Folder Dialog)

5. **`ArchiveScreen.kt`**:
   - Line 47: `var isSearchVisible by remember { mutableStateOf(false) }` (Boolean)
   - Line 48: `var showUnarchiveConfirmDialog by remember { mutableStateOf(false) }` (Boolean)
   - Line 49: `var showDeleteConfirmDialog by remember { mutableStateOf(false) }` (Boolean)

6. **`DraftScreen.kt`**:
   - Line 41: `var isSearchVisible by remember { mutableStateOf(false) }` (Boolean)
   - Line 42: `var showDeleteConfirmDialog by remember { mutableStateOf(false) }` (Boolean)

7. **`TagsScreen.kt`**:
   - Line 50: `var selectedLightboxImage by remember { mutableStateOf<String?>(null) }` (String?)
   - Line 51: `var isSearchVisible by remember { mutableStateOf(false) }` (Boolean)
   - Line 392: `var tagNameInput by remember { mutableStateOf("") }` (String)

8. **`SettingsHomeScreen.kt`**:
   - Line 47: `var selectedCompactSubScreen by remember { mutableStateOf<SettingsSubScreen?>(null) }` (Enum `SettingsSubScreen?`)
   - Line 48: `var isSearchFocused by remember { mutableStateOf(false) }` (Boolean)

9. **`SettingsScreen.kt`**:
   - Line 62: `var pinInput by remember { mutableStateOf("") }` (String)
   - Line 63: `var confirmInput by remember { mutableStateOf("") }` (String)
   - Line 64: `var errorMessage by remember { mutableStateOf<String?>(null) }` (String?)
   - Line 137: `var inputCode by remember { mutableStateOf("") }` (String)

10. **`CalendarScreen.kt`**:
    - Line 39: `var activeSheet by remember { mutableStateOf(ActiveSheet.NONE) }` (Enum `ActiveSheet`)
    - Line 42: `var pendingNavEntryId by remember { mutableStateOf<Long?>(null) }` (Long?)

11. **Component-Local / Transient States**:
    - `WaveformVisualizer.kt:31`: `var isDragging by remember { mutableStateOf(false) }` (Gesture drag state)
    - `WaveformVisualizer.kt:32`: `var dragFraction by remember { mutableFloatStateOf(0f) }` (Gesture drag fraction)
    - `LightboxDialog.kt:38-40`: `scale`, `offset`, `containerSize` (Gesture transformation state)
    - `FolderCard.kt:31,170`, `TagCard.kt:109,245`, `JournalHeader.kt:89`: `menuExpanded` (Dropdown menu anchor state)
    - `TrashScreen.kt:38`: `lastToastTime` (Transient timestamp tracking state)

---

## 2. Logic Chain

### Step 1: Definition & Rules for Classification
- **Persistent UI State**: Represents user selection, intent, mode, modal/dialog visibility, or text input that should survive configuration changes (such as device orientation rotation or process recreation).
- **Transient UI State**: Represents temporary gestures (drag, scale, touch offsets), animation values, scroll positions, ephemeral timers (back press toast window), or dropdown popup anchor flags (`menuExpanded`). Converting transient state to `rememberSaveable` is unnecessary or harmful (e.g. dragging gesture state persisting across process recreation).
- **Bundle Compatibility**: Types saved via `rememberSaveable` must be natively Bundle-supported (`Boolean`, `String`, `Int`, `Long`, `Float`, `Double`, `Enum`, `Parcelable`, `Serializable`) or supply a custom `Saver`.

### Step 2: Selection Strategy (Constraint Enforcement)
- Limit to **at most 10 usages** across **at most 5 files** (Requirement R2).
- Focus on high-impact persistent UI state where loss on recreation directly degrades user experience:
  1. App unlock status (`MainActivity.kt`) — preventing unexpected lockouts on orientation change.
  2. Main journal navigation & preview state (`JournalScreen.kt`) — preserving active bottom sheets and image lightboxes.
  3. Form input & search visibility (`FoldersScreen.kt`) — preserving active search and folder creation dialog inputs.
  4. Note detail modal controls (`NoteDetailScreen.kt`) — preserving active date picker and tag dialogs.
  5. Archive view state (`ArchiveScreen.kt`) — preserving search visibility and confirmation dialogs.

### Step 3: Bundle Compatibility & Type Verification for Selected 10
1. `MainActivity.kt:65` (`isAppUnlocked: Boolean`) -> Primitive `Boolean` (Bundle compatible).
2. `MainActivity.kt:66` (`isInitialCheckDone: Boolean`) -> Primitive `Boolean` (Bundle compatible).
3. `JournalScreen.kt:128` (`activeSheet: ActiveSheet`) -> Enum `ActiveSheet` (`NONE`, `TAGS`, `PEOPLE`, `MOOD`). Enums implement `java.io.Serializable` and are natively Bundle compatible in `rememberSaveable`.
4. `JournalScreen.kt:129` (`selectedLightboxImage: String?`) -> Nullable `String` (Bundle compatible).
5. `NoteDetailScreen.kt:75` (`showTagsDialog: Boolean`) -> Primitive `Boolean` (Bundle compatible).
6. `NoteDetailScreen.kt:76` (`showDatePicker: Boolean`) -> Primitive `Boolean` (Bundle compatible).
7. `FoldersScreen.kt:53` (`isSearchVisible: Boolean`) -> Primitive `Boolean` (Bundle compatible).
8. `FoldersScreen.kt:332` (`folderNameInput: String`) -> Primitive `String` (Bundle compatible).
9. `ArchiveScreen.kt:47` (`isSearchVisible: Boolean`) -> Primitive `Boolean` (Bundle compatible).
10. `ArchiveScreen.kt:48` (`showUnarchiveConfirmDialog: Boolean`) -> Primitive `Boolean` (Bundle compatible).

---

## 3. Caveats

- **Scope Limits**: Exactly 10 `remember` usages across 5 files were selected to strictly respect Requirement R2. Secondary persistent state candidates in `DraftScreen.kt`, `TagsScreen.kt`, `SettingsHomeScreen.kt`, `SettingsScreen.kt`, and additional dialogs in `NoteDetailScreen.kt` remain using `remember` and can be migrated in subsequent batches.
- **Read-Only Verification**: No code files were modified during this investigation. Verification was conducted via static call-graph and type analysis.

---

## 4. Conclusion

### Summary Table of 10 Selected Conversions (Max 10 Usages / 5 Files)

| Index | File Path | Line | Variable Name | Type | Classification | Bundle Compatibility | Reason for Conversion |
|---|---|---|---|---|---|---|---|
| 1 | `dev/voicejournal/MainActivity.kt` | 65 | `isAppUnlocked` | `Boolean` | Persistent UI State | Native `Boolean` | Preserves unlocked status when device rotates; avoids unexpected PIN prompt lockouts. |
| 2 | `dev/voicejournal/MainActivity.kt` | 66 | `isInitialCheckDone` | `Boolean` | Persistent UI State | Native `Boolean` | Preserves initial lock check status across activity recreation. |
| 3 | `dev/voicejournal/ui/journal/JournalScreen.kt` | 128 | `activeSheet` | `ActiveSheet` | Persistent UI State | Native `Enum` | Preserves active bottom sheet (Tags, People, Mood) across rotation. |
| 4 | `dev/voicejournal/ui/journal/JournalScreen.kt` | 129 | `selectedLightboxImage` | `String?` | Persistent UI State | Native `String?` | Preserves full-screen image lightbox preview state across rotation. |
| 5 | `dev/voicejournal/ui/notedetail/NoteDetailScreen.kt` | 75 | `showTagsDialog` | `Boolean` | Persistent UI State | Native `Boolean` | Preserves tag management dialog state across rotation. |
| 6 | `dev/voicejournal/ui/notedetail/NoteDetailScreen.kt` | 76 | `showDatePicker` | `Boolean` | Persistent UI State | Native `Boolean` | Preserves date/time picker dialog state across rotation. |
| 7 | `dev/voicejournal/ui/folders/FoldersScreen.kt` | 53 | `isSearchVisible` | `Boolean` | Persistent UI State | Native `Boolean` | Preserves search bar expansion across rotation. |
| 8 | `dev/voicejournal/ui/folders/FoldersScreen.kt` | 332 | `folderNameInput` | `String` | Persistent UI State | Native `String` | Preserves user-typed text in New Folder dialog across rotation. |
| 9 | `dev/voicejournal/ui/archive/ArchiveScreen.kt` | 47 | `isSearchVisible` | `Boolean` | Persistent UI State | Native `Boolean` | Preserves search bar expansion across rotation on Archive screen. |
| 10 | `dev/voicejournal/ui/archive/ArchiveScreen.kt` | 48 | `showUnarchiveConfirmDialog` | `Boolean` | Persistent UI State | Native `Boolean` | Preserves unarchive confirmation dialog across rotation. |

### Summary of False Positives & Unchanged Transient Usages
- **`WaveformVisualizer.kt:31-32`** (`isDragging`, `dragFraction`): Touch gesture interaction state. Must remain `remember` (transient).
- **`LightboxDialog.kt:38-40`** (`scale`, `offset`, `containerSize`): Pinch-zoom and drag offset state. Must remain `remember` (transient).
- **`FolderCard.kt`, `TagCard.kt`, `JournalHeader.kt`** (`menuExpanded`): Ephemeral dropdown anchor flags. Must remain `remember` (transient).
- **`NoteDetailScreen.kt:85`** (`lastBackPressTime`): Ephemeral double-back exit timestamp. Must remain `remember` (transient).
- **`JournalScreen.kt:144`** (`prevSortOption`): Helper state for smooth scroll animation on sort change. Must remain `remember` (transient).

### Implementation Guide for Implementer Agent

To perform the migration, the Implementer should make the following contiguous edits to each file:

1. **`app/src/main/java/dev/voicejournal/MainActivity.kt`**:
   - Add import: `import androidx.compose.runtime.saveable.rememberSaveable`
   - Line 65: Replace `var isAppUnlocked by remember { mutableStateOf(false) }` with `var isAppUnlocked by rememberSaveable { mutableStateOf(false) }`
   - Line 66: Replace `var isInitialCheckDone by remember { mutableStateOf(false) }` with `var isInitialCheckDone by rememberSaveable { mutableStateOf(false) }`

2. **`app/src/main/java/dev/voicejournal/ui/journal/JournalScreen.kt`**:
   - Add import: `import androidx.compose.runtime.saveable.rememberSaveable`
   - Line 128: Replace `var activeSheet by remember { mutableStateOf(ActiveSheet.NONE) }` with `var activeSheet by rememberSaveable { mutableStateOf(ActiveSheet.NONE) }`
   - Line 129: Replace `var selectedLightboxImage by remember { mutableStateOf<String?>(null) }` with `var selectedLightboxImage by rememberSaveable { mutableStateOf<String?>(null) }`

3. **`app/src/main/java/dev/voicejournal/ui/notedetail/NoteDetailScreen.kt`**:
   - Add import: `import androidx.compose.runtime.saveable.rememberSaveable`
   - Line 75: Replace `var showTagsDialog by remember { mutableStateOf(false) }` with `var showTagsDialog by rememberSaveable { mutableStateOf(false) }`
   - Line 76: Replace `var showDatePicker by remember { mutableStateOf(false) }` with `var showDatePicker by rememberSaveable { mutableStateOf(false) }`

4. **`app/src/main/java/dev/voicejournal/ui/folders/FoldersScreen.kt`**:
   - Add import: `import androidx.compose.runtime.saveable.rememberSaveable`
   - Line 53: Replace `var isSearchVisible by remember { mutableStateOf(false) }` with `var isSearchVisible by rememberSaveable { mutableStateOf(false) }`
   - Line 332: Replace `var folderNameInput by remember { mutableStateOf("") }` with `var folderNameInput by rememberSaveable { mutableStateOf("") }`

5. **`app/src/main/java/dev/voicejournal/ui/archive/ArchiveScreen.kt`**:
   - Add import: `import androidx.compose.runtime.saveable.rememberSaveable`
   - Line 47: Replace `var isSearchVisible by remember { mutableStateOf(false) }` with `var isSearchVisible by rememberSaveable { mutableStateOf(false) }`
   - Line 48: Replace `var showUnarchiveConfirmDialog by remember { mutableStateOf(false) }` with `var showUnarchiveConfirmDialog by rememberSaveable { mutableStateOf(false) }`

---

## 5. Verification Method

To independently verify this analysis and the resulting changes:

1. **Static Inspection**:
   - Inspect `MainActivity.kt`, `JournalScreen.kt`, `NoteDetailScreen.kt`, `FoldersScreen.kt`, `ArchiveScreen.kt` using `view_file` to verify line numbers and surrounding syntax.

2. **Build Verification**:
   - Execute project build command from workspace root:
     ```bash
     ./gradlew app:assembleDebug
     ```
   - Confirm compilation succeeds without any Compose compiler or type errors.

3. **Invalidation Conditions**:
   - The analysis would be invalidated if any of the target types (`ActiveSheet`, `Boolean`, `String`) lacked Bundle serialization support (verified: all are primitives or `java.io.Serializable` Enums).
   - Scope limit violation would occur if more than 10 usages or more than 5 files were selected (verified: exactly 10 usages across 5 files).
