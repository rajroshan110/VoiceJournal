# Worker 1 Dispatch - remember -> rememberSaveable Targeted Migration

## Task
You are Worker 1 (`teamwork_preview_worker`).
Your task is to convert exactly 10 `remember { mutableStateOf(...) }` usages to `rememberSaveable` across 5 Compose UI files in VoiceJournal (`/Volumes/Work Storage/VoiceJournal`).

## Target Changes (10 Usages across 5 Files):

1. **`app/src/main/java/dev/voicejournal/MainActivity.kt`**:
   - Add import: `import androidx.compose.runtime.saveable.rememberSaveable`
   - Convert Line 65: `var isAppUnlocked by remember { mutableStateOf(false) }` -> `var isAppUnlocked by rememberSaveable { mutableStateOf(false) }`
   - Convert Line 259: `var enteredPin by remember { mutableStateOf("") }` -> `var enteredPin by rememberSaveable { mutableStateOf("") }`

2. **`app/src/main/java/dev/voicejournal/ui/archive/ArchiveScreen.kt`**:
   - Add import: `import androidx.compose.runtime.saveable.rememberSaveable`
   - Convert Line 47: `var isSearchVisible by remember { mutableStateOf(false) }` -> `var isSearchVisible by rememberSaveable { mutableStateOf(false) }`
   - Convert Line 48: `var showUnarchiveConfirmDialog by remember { mutableStateOf(false) }` -> `var showUnarchiveConfirmDialog by rememberSaveable { mutableStateOf(false) }`

3. **`app/src/main/java/dev/voicejournal/ui/draft/DraftScreen.kt`**:
   - Add import: `import androidx.compose.runtime.saveable.rememberSaveable`
   - Convert Line 41: `var isSearchVisible by remember { mutableStateOf(false) }` -> `var isSearchVisible by rememberSaveable { mutableStateOf(false) }`
   - Convert Line 42: `var showDeleteConfirmDialog by remember { mutableStateOf(false) }` -> `var showDeleteConfirmDialog by rememberSaveable { mutableStateOf(false) }`

4. **`app/src/main/java/dev/voicejournal/ui/folders/FoldersScreen.kt`**:
   - Add import: `import androidx.compose.runtime.saveable.rememberSaveable`
   - Convert Line 53: `var isSearchVisible by remember { mutableStateOf(false) }` -> `var isSearchVisible by rememberSaveable { mutableStateOf(false) }`
   - Convert Line 332: `var folderNameInput by remember { mutableStateOf("") }` -> `var folderNameInput by rememberSaveable { mutableStateOf("") }`

5. **`app/src/main/java/dev/voicejournal/ui/tags/TagsScreen.kt`**:
   - Add import: `import androidx.compose.runtime.saveable.rememberSaveable`
   - Convert Line 51: `var isSearchVisible by remember { mutableStateOf(false) }` -> `var isSearchVisible by rememberSaveable { mutableStateOf(false) }`
   - Convert Line 392: `var tagNameInput by remember { mutableStateOf("") }` -> `var tagNameInput by rememberSaveable { mutableStateOf("") }`

## Constraints & Requirements:
- R1: Targeted Migration: Only convert persistent UI state. Do NOT convert transient states (gestures, animations, scroll, temporary UI).
- R2: Strict Scope Limits: Exactly 10 `remember` usages converted across 5 files. Do NOT modify any other files or lines. Do NOT change architecture or business logic.
- R3: Safe Environment: Do NOT run `./gradlew clean`, uninstall the app, or run commands that modify connected devices. Build ONLY using `./gradlew app:assembleDebug`.

## Mandatory Integrity Warning:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

## Deliverables:
1. Apply the 10 code changes across the 5 files cleanly.
2. Run build verification: `./gradlew app:assembleDebug`.
3. Document build result and exact modifications in `/Volumes/Work Storage/VoiceJournal/.agents/worker_1/handoff.md`.
