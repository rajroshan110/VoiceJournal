# VoiceJournal Architecture Documentation

This document provides a detailed technical overview of the **VoiceJournal** Android application architecture. It is designed for developers, contributors, and maintainers working on the codebase.

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Architecture Overview](#architecture-overview)
3. [Architecture Diagram](#architecture-diagram)
4. [Complete Directory Structure](#complete-directory-structure)
5. [Layer Documentation](#layer-documentation)
   - [Presentation Layer](#presentation-layer)
   - [Domain Layer](#domain-layer)
   - [Data Layer](#data-layer)
   - [Audio System](#audio-system)
   - [Transcription System](#transcription-system)
   - [Dependency Injection](#dependency-injection)
   - [Navigation](#navigation)
   - [Design System](#design-system)
6. [Data Flow](#data-flow)
   - [Recording Flow](#recording-flow)
   - [Playback Flow](#playback-flow)
   - [Transcription Flow](#transcription-flow)
   - [Import / Export Flow](#import--export-flow)
7. [Dependency Injection Details](#dependency-injection-details)
8. [Storage & Persistence](#storage--persistence)
9. [Design Principles](#design-principles)
10. [Future Improvements](#future-improvements)

---

## Project Overview

VoiceJournal is a privacy-first, offline-first Android application that combines voice recording, intelligent local transcription, topic & mention tagging, and visual journal entries.

### Key Architectural Goals

* **Offline-First**: All data, audio recordings, images, database entities, and AI transcriptions execute 100% on-device without cloud dependencies.
* **Privacy-First**: No telemetry, analytics, or remote servers. User data remains encrypted or locally stored on the device filesystem.
* **Maintainable**: Strict adherence to Clean Architecture and MVVM guarantees clear boundaries between business logic, data persistence, and UI.
* **Scalable & Modular**: Centralized managers handle storage, audio playback/recording, and native transcription via C++ JNI bindings (`Whisper.cpp`).

---

## Architecture Overview

VoiceJournal follows **Clean Architecture** combined with **MVVM (Model-View-ViewModel)** and **Unidirectional Data Flow (UDF)**.

```
┌────────────────────────────────────────────────────────────────────────┐
│                          PRESENTATION LAYER                            │
│           Jetpack Compose Screens  ──►  ViewModels (StateFlow)         │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                             DOMAIN LAYER                               │
│            Use Cases (Business Logic)  ──►  Domain Models              │
│                                        └──  Repository Interfaces      │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                              DATA LAYER                                │
│       Repository Impl  ──►  Room DB / DataStore / MediaStorageManager  │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        INFRASTRUCTURE & NATIVE                         │
│       Whisper.cpp JNI Engine  │  AudioRecord/MediaPlayer  │  Services  │
└────────────────────────────────────────────────────────────────────────┘
```

### Layer Responsibilities

1. **Presentation Layer**: Built with Jetpack Compose. ViewModels expose immutable `StateFlow` states and consume `UserIntent` / actions. Composable functions render state and emit events.
2. **Domain Layer**: The core business logic containing domain models (`JournalEntry`, `AudioTrack`, `Tag`), repository interfaces, and specific single-responsibility Use Cases (`SaveEntryUseCase`, `GenerateTranscriptUseCase`, `GetInsightsSummaryUseCase`).
3. **Data Layer**: Implements repository interfaces (`JournalRepositoryImpl`, `UserPreferencesRepositoryImpl`). Manages SQLite persistence via Room DB (`AppDatabase`), user settings via DataStore (`UserPreferencesManager`), and structured media file storage (`MediaStorageManager`).
4. **Infrastructure & Native Layer**: Android system framework integration including `AudioRecord`, `MediaPlayer`, `AudioPlaybackService`, `TranscriptionService`, and the `WhisperEngine` C++ JNI bindings for local AI inference.

---

## Architecture Diagram

The ASCII diagram below demystifies the data flow and boundary interactions between application components:

```
                      +----------------------------------+
                      |       Jetpack Compose UI         |
                      | (JournalScreen, NoteDetail, etc.)|
                      +----------------+-----------------+
                                       |
                             StateFlow | User Events
                                       v
                      +----------------------------------+
                      |            ViewModel             |
                      |  (NoteDetailViewModel, etc.)     |
                      +----------------+-----------------+
                                       |
                               Invokes | Returns Domain Models
                                       v
                      +----------------------------------+
                      |            Use Cases             |
                      | (GenerateTranscript, SaveEntry)  |
                      +----------------+-----------------+
                                       |
                       Calls Interface | Returns Entities / Models
                                       v
                      +----------------------------------+
                      |      Repository Interfaces       |
                      |       (JournalRepository)        |
                      +----------------+-----------------+
                                       |
                            Implements |
                                       v
                      +----------------------------------+
                      |     Repository Implementations    |
                      |     (JournalRepositoryImpl)      |
                      +---+------------+-------------+---+
                          |            |             |
           DAO Operations |            | Read/Write  | Delegate Tasks
                          v            v             v
   +------------------------+  +---------------+  +---------------------+
   |      Room Database     |  | Media Storage |  | Background Services |
   | (Entities, DAOs, Migr.)|  | (recordings/  |  | (AudioPlaybackSvc,  |
   +------------------------+  |  images/)     |  |  TranscriptionSvc)  |
                               +---------------+  +----------+----------+
                                                             |
                                                             | JNI Native Call
                                                             v
                                                  +---------------------+
                                                  | Whisper.cpp Engine  |
                                                  |  (libwhisper.so)    |
                                                  +---------------------+
```

---

## Complete Directory Structure

Below is the repository package hierarchy under `dev.voicejournal`:

```text
app/src/main/java/dev/voicejournal/
├── MainActivity.kt                      # Single activity entry point
├── VoiceApp.kt                          # Hilt Application class
├── audio/                               # Low-level audio recording & playback system
│   ├── AmplitudeExtractor.kt            # Extracts peak amplitude values for waveforms
│   ├── AudioFileRepair.kt               # Validates & repairs corrupted WAV file headers
│   ├── AudioFocusManager.kt             # Handles Android system audio focus requests
│   ├── AudioPlaybackService.kt          # Foreground service for continuous playback
│   ├── AudioPlayerManager.kt            # MediaPlayer wrapper for seek/pause/play controls
│   ├── AudioRecorderManager.kt          # AudioRecord wrapper for real-time PCM capture
│   ├── M4aEncoder.kt                    # MediaCodec AAC/M4A encoder
│   └── WavFileWriter.kt                 # Standard PCM to 16kHz 16-bit WAV writer
├── data/                                # Data persistence, repositories, and backup
│   ├── backup/                          # Import & Export orchestrators
│   │   ├── ExportManager.kt             # High-level data export facade
│   │   ├── ImportManager.kt             # High-level data import facade
│   │   └── v1/                          # Versioned backup format (v1) implementation
│   │       ├── BackupDeserializer.kt    # Deserializes JSON backup payload into objects
│   │       ├── BackupExporter.kt        # Writes entries and media to ZIP archive
│   │       ├── BackupImporter.kt        # Reads and unpacks ZIP backups
│   │       ├── BackupManifestBuilder.kt # Generates manifest.json metadata
│   │       ├── BackupMediaCollector.kt  # Collects recording and image files for backup
│   │       ├── BackupRestorer.kt        # Restores database records inside transaction
│   │       ├── BackupSerializer.kt      # Serializes database entities into JSON
│   │       ├── BackupUnpacker.kt        # Unzips backup ZIP archive into temp directory
│   │       ├── BackupValidator.kt       # Validates backup integrity and schema version
│   │       ├── BackupZipWriter.kt       # Streams ZIP archive entries
│   │       └── dto/                     # Backup Data Transfer Objects
│   │           ├── BackupDto.kt         # Master JSON schema DTOs
│   │           └── BackupResult.kt      # Import/export execution result wrappers
│   ├── local/                           # Local storage drivers
│   │   ├── datastore/                   # Jetpack DataStore preferences
│   │   │   └── UserPreferencesManager.kt# Manages theme, app lock, and recording settings
│   │   └── db/                          # Room Database
│   │       ├── AppDatabase.kt           # RoomDatabase configuration & SQLite migrations
│   │       ├── Converters.kt            # Legacy Room type converters
│   │       ├── dao/                     # Data Access Objects (JournalEntry, Tag, Image)
│   │       │   ├── EntryImageDao.kt
│   │       │   ├── JournalEntryDao.kt
│   │       │   └── TagDao.kt
│   │       ├── entity/                  # Database tables
│   │       │   ├── EntryImageEntity.kt
│   │       │   ├── EntryTagCrossRef.kt
│   │       │   ├── JournalEntryEntity.kt
│   │       │   └── TagEntity.kt
│   │       └── relation/                # Room relational mapping (@Relation)
│   │           └── EntryWithTagsAndImages.kt
│   ├── mapper/                          # Data layer mappers
│   │   └── EntryMapper.kt               # Maps between Room entities and Domain models
│   ├── repository/                      # Concrete repository implementations
│   │   ├── JournalRepositoryImpl.kt     # Journal data operations implementation
│   │   └── UserPreferencesRepositoryImpl.kt # Preferences repository implementation
│   └── storage/                         # Unified File I/O manager
│       └── MediaStorageManager.kt       # Canonical manager for recordings and images
├── di/                                  # Hilt Dependency Injection Modules
│   ├── AppModule.kt                     # Application-wide singletons (Dispatchers, DataStore)
│   ├── AudioModule.kt                   # Audio recorder/player manager bindings
│   ├── DatabaseModule.kt                # Room database & DAO providers
│   └── TranscriptionModule.kt           # Whisper transcription engine providers
├── domain/                              # Business domain models, interfaces, and logic
│   ├── model/                           # Pure Kotlin domain data classes
│   │   ├── AppLockTimeout.kt
│   │   ├── AudioFormat.kt
│   │   ├── AudioTrack.kt
│   │   ├── EntryImage.kt
│   │   ├── InsightDateRangeMode.kt
│   │   ├── JournalEntry.kt
│   │   ├── SettingModels.kt
│   │   ├── Tag.kt
│   │   └── TagType.kt
│   ├── repository/                      # Domain repository contracts (interfaces)
│   │   ├── JournalRepository.kt
│   │   └── UserPreferencesRepository.kt
│   └── usecase/                         # Business use case implementations
│       ├── DeleteEntryUseCase.kt
│       ├── GenerateTranscriptUseCase.kt
│       ├── GetAllEntriesUseCase.kt
│       ├── GetAllTagsUseCase.kt
│       ├── GetCalendarDataUseCase.kt
│       ├── GetEntriesByDateUseCase.kt
│       ├── GetEntriesByTagUseCase.kt
│       ├── GetEntryByIdUseCase.kt
│       ├── GetInsightDataUseCase.kt
│       ├── GetInsightsSummaryUseCase.kt
│       ├── SaveEntryUseCase.kt
│       └── TrashUseCases.kt
├── service/                             # Android Application Services
│   └── TranscriptionService.kt          # Foreground service for background transcription
├── transcription/                       # Local AI Speech-to-Text via Whisper.cpp JNI
│   ├── AudioFileResolver.kt             # Resolves audio file Uri/Path for processing
│   ├── AudioResampler.kt                # Resamples audio signals to 16kHz mono PCM
│   ├── M4aDecoder.kt                    # Decodes AAC/M4A audio to PCM float arrays
│   ├── WavToFloatConverter.kt           # Converts 16-bit WAV PCM bytes to normalized float arrays
│   ├── WhisperLib.kt                    # JNI interface declaration (`external fun initContext`)
│   ├── WhisperManager.kt                # High-level coordinator for Whisper model loading
│   ├── engine/                          # Speech engine contracts & Whisper JNI wrapper
│   │   ├── SpeechToTextEngine.kt        # Generic speech-to-text interface
│   │   └── WhisperEngine.kt             # Direct native JNI wrapper implementation
│   └── model/                           # Transcription result data classes
│       └── TranscriptResult.kt
├── ui/                                  # Presentation layer (Jetpack Compose)
│   ├── archive/                         # Archive view & ViewModel
│   ├── calendar/                        # Calendar view & ViewModel
│   ├── components/                      # Shared legacy UI widgets
│   ├── designsystem/                    # VoiceJournal Design System
│   │   ├── components/                  # Canonical reusable design components
│   │   │   ├── SearchBar.kt             # Unified SearchBar component
│   │   │   ├── audio/                   # Canonical audio player & waveform components
│   │   │   │   ├── UnifiedAudioPlayerBar.kt
│   │   │   │   └── WaveformVisualizer.kt
│   │   │   └── feedback/
│   │   ├── motion/                      # Custom Compose animations
│   │   ├── scaffolds/                   # Standardized screen layouts
│   │   ├── theme/                       # Colors, Typography, Shapes
│   │   └── tokens/                      # Spacing & Radius design tokens
│   ├── draft/                           # Draft entries view & ViewModel
│   ├── folders/                         # Folders view & ViewModel
│   ├── insight/                         # Analytics & Insights view & ViewModel
│   ├── journal/                         # Main Journal feed & ViewModel
│   ├── navigation/                      # AppNavHost, Screen sealed classes, BottomNavBar
│   ├── notedetail/                      # Note Editor / Detail view & ViewModel
│   ├── settings/                        # Settings dashboard & sub-screens
│   ├── tags/                            # Tags Manager & ViewModel
│   ├── theme/                           # AppTheme wrapper
│   └── trash/                           # Trash bin view & ViewModel
└── util/                                # Utility classes
    ├── Constants.kt
    ├── DateUtils.kt
    └── FileUtils.kt
```

### Major Package Descriptions

* **`audio/`**: Encapulates Android `AudioRecord` and `MediaPlayer` APIs. Provides PCM amplitude extraction for live visualizers, header repair for unclosed WAV files, and MediaCodec AAC encoding.
* **`data/`**: Manages data persistence. Contains Room entities/DAOs, DataStore configuration, centralized `MediaStorageManager`, and the robust `backup/v1` exporter/importer.
* **`di/`**: Configures Hilt dependency injection bindings for database instances, audio engines, repository singletons, and transcription engines.
* **`domain/`**: Pure Kotlin layer devoid of Android framework dependencies. Holds business logic via Use Cases and defines Repository contracts.
* **`service/`**: Foreground service management ensuring background execution during lengthy audio playback or speech-to-text processing.
* **`transcription/`**: On-device AI engine leveraging `Whisper.cpp` compiled via NDK into native JNI libraries (`libwhisper.so`). Decodes M4A/WAV into normalized 16kHz float buffers.
* **`ui/`**: 100% Jetpack Compose presentation layer structured cleanly by screen features, governed by a unified Design System.

---

## Layer Documentation

### Presentation Layer

* **Framework**: Jetpack Compose using Material 3 and custom Design System tokens (`dev.voicejournal.ui.designsystem`).
* **Architecture Pattern**: MVVM with Unidirectional Data Flow (UDF).
* **State Management**: ViewModels expose UI state using `StateFlow<UiState>` collected reactively in Composables via `collectAsStateWithLifecycle()`.
* **User Input**: Screens send user events directly to ViewModels via explicit method calls (e.g., `viewModel.onSearchQueryChange(query)`).

### Domain Layer

* **Independence**: Has zero dependencies on Android APIs or UI frameworks.
* **Use Cases**: Single-purpose classes providing explicit business operations:
  * `SaveEntryUseCase`: Handles creating or updating a entry.
  * `GenerateTranscriptUseCase`: Coordinates local audio transcription with the `WhisperEngine`.
  * `GetInsightsSummaryUseCase`: Aggregates tag counts, recording durations, and mood metrics for reporting.
* **Models**: Domain data structures (`JournalEntry`, `AudioTrack`, `Tag`, `EntryImage`) decoupled from database tables.

### Data Layer

* **Room Database (`AppDatabase`)**: Configured with version 8 schema. Uses SQLite migrations (`MIGRATION_7_8`) to populate unique `UUID` strings across tables for multi-device sync readiness.
* **Repositories**: `JournalRepositoryImpl` orchestrates transactions using `JournalEntryDao`, `TagDao`, `EntryImageDao`, and `MediaStorageManager`.
* **Mappers**: `EntryMapper.kt` handles conversion between Room entities and domain objects, including JSON stringification of `AudioTrack` track lists (`org.json.JSONArray`) with legacy delimiter fallback.

### Audio System

* **Recording (`AudioRecorderManager`)**: Captures 16kHz 16-bit mono PCM audio. Supports live peak amplitude extraction emitted via Kotlin `Flow` for real-time waveform rendering.
* **Encoding**: Offers direct 16-bit WAV PCM output (`WavFileWriter`) or hardware-accelerated MediaCodec AAC/M4A compression (`M4aEncoder`).
* **Playback (`AudioPlayerManager`)**: Manages `MediaPlayer` state machine, seek positioning, and playback speed (0.5x - 2.0x). Integrates with `AudioFocusManager` to pause on ducking/calls and `AudioPlaybackService` for persistent notification controls.

### Transcription System

* **Engine (`WhisperEngine`)**: JNI bridge communicating with C++ native library `libwhisper.so`. Supports GGUF/GGML quantized models (`tiny`, `base`, `small`).
* **Audio Preprocessing Pipeline**:
  1. Resolves Uri/File path via `AudioFileResolver`.
  2. Decodes M4A/AAC via `M4aDecoder` or WAV via `WavToFloatConverter`.
  3. Resamples audio to 16,000 Hz mono PCM using `AudioResampler`.
  4. Passes `FloatArray` buffers to native `WhisperLib` JNI functions.

### Dependency Injection

* **Hilt**: Manages compile-time dependency injection across the application.
* **Scopes**: Singletons (`@Singleton`) are scoped to the application lifetime (Database, MediaStorageManager, Repositories, Audio Managers).

### Navigation

* **AppNavHost**: Built on `androidx.navigation.compose.NavHost`.
* **Routes**: Sealed class `Screen` defines screen routes (`Journal`, `NoteDetail`, `Archive`, `Draft`, `Folders`, `Tags`, `Calendar`, `Insight`, `Trash`, `Settings`).

### Design System

* **Canonical Components**: Standardized reusable UI elements enforcing project-wide UI consistency:
  * `SearchBar.kt`: Unified search bar supporting local `TextFieldValue` state (fixing cursor jumping), configurable auto-focus, and inline top-bar integration.
  * `UnifiedAudioPlayerBar.kt`: Canonical audio playback bar featuring dynamic waveform rendering, play/pause toggles, seeking, and timing labels.
  * `WaveformVisualizer.kt`: Canvas-based visualizer drawing real-time and static amplitude bars.

---

## Data Flow

### Recording Flow

```
[User Taps Record]
       │
       ▼
[NoteDetailViewModel] ──► [AudioRecorderManager.startRecording()]
                                   │
                                   ├─► Captures PCM audio from AudioRecord
                                   ├─► Emits live amplitudes to [WaveformVisualizer]
                                   └─► Writes bytes via [WavFileWriter / M4aEncoder]
       │
[User Stops Record]
       │
       ▼
[MediaStorageManager] ──► Saves file to `filesDir/recordings/<uuid>.wav`
       │
       ▼
[SaveEntryUseCase] ──► [JournalRepositoryImpl] ──► Inserts record into [Room DB]
```

### Playback Flow

```
[User Taps Play on UnifiedAudioPlayerBar]
       │
       ▼
[AudioPlayerManager] ──► Requests Audio Focus via [AudioFocusManager]
       │
       ├─► Starts [AudioPlaybackService] for notification controls
       ├─► Prepares MediaPlayer with canonical path from [MediaStorageManager]
       └─► Emits current playback position (ms) via Flow to UI
```

### Transcription Flow

```
[User Taps Transcribe (Aa)]
       │
       ▼
[GenerateTranscriptUseCase] ──► Launches [TranscriptionService] (Foreground)
                                        │
                                        ▼
                                 [WhisperEngine]
                                        │
                                        ├─► Decodes audio via [M4aDecoder / WavToFloatConverter]
                                        ├─► Resamples to 16kHz mono via [AudioResampler]
                                        └─► Invokes native Whisper JNI C++ library
                                        │
                                        ▼
                               [TranscriptResult]
                                        │
                                        ▼
[JournalRepositoryImpl] ──► Updates AudioTrack JSON & transcript in [Room DB]
```

### Import / Export Flow

```
================ EXPORT FLOW ================
[ExportManager.exportData()]
       │
       ▼
[BackupExporter (v1)]
       │
       ├─► Queries Room DB for all entries, tags, cross-refs, and images
       ├─► Serializes entities into `backup_data.json` via [BackupSerializer]
       ├─► Generates `manifest.json` via [BackupManifestBuilder]
       ├─► Bundles recordings into `media/recordings/`
       ├─► Bundles images into `media/images/`
       └─► Writes compressed stream to target ZIP file via [BackupZipWriter]

================ IMPORT FLOW ================
[ImportManager.importData()]
       │
       ▼
[BackupImporter (v1)]
       │
       ├─► Unpacks ZIP archive into temporary cache directory via [BackupUnpacker]
       ├─► Validates schema & version in `manifest.json` via [BackupValidator]
       ├─► Parses JSON payload via [BackupDeserializer]
       ├─► Restores entities inside Room DB transaction via [BackupRestorer]
       └─► Copies media files into canonical `filesDir/recordings/` and `filesDir/images/`
```

---

## Dependency Injection Details

VoiceJournal configures Hilt modules in package `dev.voicejournal.di`:

1. **`AppModule`**:
   * Provides Application Context (`@ApplicationContext`).
   * Provides Coroutine Dispatchers (`Dispatchers.IO`, `Dispatchers.Default`).
   * Provides Datastore `UserPreferencesManager`.
2. **`DatabaseModule`**:
   * Provides `@Singleton AppDatabase` built via Room.
   * Provides DAO singletons: `JournalEntryDao`, `TagDao`, `EntryImageDao`.
3. **`AudioModule`**:
   * Provides `@Singleton AudioRecorderManager`.
   * Provides `@Singleton AudioPlayerManager`.
   * Provides `@Singleton AudioFocusManager`.
4. **`TranscriptionModule`**:
   * Provides `@Singleton SpeechToTextEngine` bound to `WhisperEngine`.
   * Provides `@Singleton WhisperManager`.

---

## Storage & Persistence

VoiceJournal enforces clear separation of storage domains:

```text
context.filesDir/
├── recordings/                 # Canonical storage directory for audio tracks (.wav, .m4a)
└── images/                     # Canonical storage directory for entry image attachments (.jpg, .png)
```

### Storage Components

1. **SQLite Database (Room)**:
   * Class: `AppDatabase` (Version 8).
   * Tables: `journal_entries`, `tags`, `entry_tag_cross_ref`, `entry_images`.
   * Schema: Migration 7 to 8 populated unique `UUID` strings for every database record.
2. **Media Storage (`MediaStorageManager`)**:
   * Serves as the single source of truth for generating media filenames and resolving file paths.
   * Standardizes media paths under `filesDir/recordings/` and `filesDir/images/`.
   * Provides legacy fallback support checking `filesDir/audio/` if a recording file is not found in the new canonical folder.
3. **Preferences DataStore (`UserPreferencesManager`)**:
   * Stores lightweight user settings (App Lock PIN, lock timeout, dark/light theme mode, default audio recording format).
4. **Backup Archive Format (v1)**:
   ```text
   voicejournal_backup_<timestamp>.zip
   ├── manifest.json              # Version metadata, app build info, entity counts
   ├── backup_data.json          # Serialized entries, tags, cross-references, images
   └── media/
       ├── recordings/            # All exported audio files
       └── images/                # All exported image attachments
   ```

---

## Design Principles

1. **Offline-First**: The app is 100% functional without an internet connection. AI transcription model files (`ggml-tiny.bin`) run locally via NDK.
2. **Privacy-First**: User journal entries, audio recordings, and transcriptions never leave the device.
3. **Repository Pattern**: ViewModels and Use Cases depend solely on domain interfaces (`JournalRepository`), making the data layer easily swappable or testable.
4. **Single Source of Truth (SSOT)**: Database records in Room combined with `MediaStorageManager` drive the application state.
5. **MVVM & Unidirectional Data Flow**: State flows down from ViewModels to Composables; events flow up from Composables to ViewModels.

---

## Future Improvements

* **Multimodule Gradle Structure**: Split monolithic `:app` module into distinct Gradle feature modules (`:core:designsystem`, `:core:database`, `:core:audio`, `:feature:journal`, `:feature:transcription`).
* **Kotlin Serialization Migration**: Replace remaining native `org.json` usage with kotlinx.serialization across Room entity mappers.
* **Model Download & Caching Manager**: Implement background download/verification for larger Whisper AI models (`base`, `small`, `medium`).
* **Encrypted Database & Storage**: Integrate SQLCipher for Room database encryption and encrypted file storage for sensitive journal entries.
