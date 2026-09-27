# VoiceJournal

<p align="center">
  <img src="assets/logo.png" alt="VoiceJournal Logo" width="140"/>
</p>

<p align="center">
  <strong>
    A privacy-first Android voice journaling app with fully offline AI transcription powered by Whisper.cpp.
  </strong>
</p>

<p align="center">

![Platform](https://img.shields.io/badge/Platform-Android-green)
![Min SDK](https://img.shields.io/badge/Min%20SDK-29%20(Android%2010%2B)-blue)
![Kotlin](https://img.shields.io/badge/Kotlin-100%25-purple)
![Material 3](https://img.shields.io/badge/Material%203-Jetpack%20Compose-blue)
![Offline](https://img.shields.io/badge/Offline-First-success)

</p>

---

## Overview

VoiceJournal is a modern Android application that lets you record voice notes and automatically transcribe them **entirely on your device** using Whisper.cpp.

- **100% Local Inference**: Runs locally on your device with no external server processing.
- **Bilingual Speech-to-Text**: Optimized for **English** and **Hindi/Urdu** speech out-of-the-box.
- **Strict Privacy**: No cloud processing, no accounts, no subscriptions, no ads, and zero telemetry.

Your recordings and transcripts remain on your device unless you explicitly export them.

---

## Features

- 🎤 High-quality voice note recording (M4A / WAV)
- 🤖 Offline AI transcription powered by Whisper.cpp (English & Hindi)
- 🌐 100% Offline operation (one-time ~60MB SHA-256 verified model download on first use)
- 🔒 Privacy-first architecture (no analytics, no external tracking, no cloud dependencies)
- 📱 Material Design 3 interface with dynamic theming
- 🌙 Dark and light mode support
- 🏷️ Flexible categorization with Tags and Folders
- 📂 Secure Backup Import & Export (with Zip-Slip and path traversal protection)
- 🔍 Fast local transcript search
- 📝 Rich text editor for journaling notes
- ▶️ Background audio playback with notification & lock-screen media controls
- 🔐 Keystore-backed PIN lock and Biometric app protection
- ⚡ Fast, lightweight, and battery-conscious

---

## Screenshots

<div align="center"> 

<table>
<tr>
<td align="center">
<img src="assets/screenshots/home.jpg" width="220" alt="Home">
<br><b>Home</b>
</td>

<td align="center">
<img src="assets/screenshots/notes.jpg" width="220" alt="Notes">
<br><b>Notes</b>
</td>

<td align="center">
<img src="assets/screenshots/transcript.jpg" width="220" alt="Transcript">
<br><b>Transcript</b>
</td>
</tr>

<tr>
<td align="center">
<img src="assets/screenshots/organize.jpg" width="220" alt="Organize">
<br><b>Organize</b>
</td>

<td align="center">
<img src="assets/screenshots/settings.jpg" width="220" alt="Settings">
<br><b>Settings</b>
</td>

<td align="center">
<img src="assets/screenshots/import-export.jpg" width="220" alt="Import & Export">
<br><b>Import & Export</b>
</td>
</tr>
</table>

</div>

---

## Why VoiceJournal?

Unlike many voice note applications, VoiceJournal is designed with privacy as the primary goal.

Everything happens locally:

- Recording
- Speech recognition
- Storage
- Search
- Playback

No audio is uploaded to external servers.

---

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | MVVM |
| Design | Material Design 3 |
| State Management | StateFlow |
| Speech Recognition | Whisper.cpp |
| Native Code | C++ (JNI) |
| Build System | Gradle |
| Version Control | Git & GitHub |

---

## Project Structure

```text
app/
├── audio/            # Audio recording, playback & encoding
├── data/             # Room database, repositories & import/export
├── di/               # Hilt dependency injection
├── domain/           # Models, repository interfaces & use cases
├── transcription/    # Offline Whisper.cpp engine
├── ui/               # Jetpack Compose UI
├── util/             # Shared utilities
├── MainActivity.kt
└── VoiceApp.kt
```

### Architecture

VoiceJournal follows **Clean Architecture** with **MVVM**.

```
Presentation (Jetpack Compose)
        │
        ▼
ViewModels
        │
        ▼
Domain (Use Cases)
        │
        ▼
Repositories
        │
        ▼
Data Layer
(Room • Preferences • Backup • Storage)
        │
        ▼
Whisper.cpp / Local Storage
```

For a complete package structure and architecture documentation, see
**[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)**.

---

## Installation

Clone the repository:

```bash
git clone https://github.com/rajroshan110/VoiceJournal.git
```

Open using Android Studio.

Build and run on an Android device.

---

## Requirements

### Target Device
- **Operating System**: Android 10+ (API 29+) (Supported: Android 10 to Android 15)
- **Permissions**: Zero storage permissions required (uses Android Photo Picker via `PickVisualMedia` and local sandbox storage)
- **Architecture**: 64-bit ARM (`arm64-v8a`) recommended for real-time inference (supports standard ARMv8-A baseline)
- **Memory**: 3GB+ RAM recommended (Whisper inference allocates ~200-300MB working RAM)

### Development & Build
- **IDE**: Android Studio Ladybug / Meerkat or newer
- **JDK**: Java 17 (e.g. Eclipse Adoptium Temurin 17)
- **Android NDK**: 27.0.12077973+
- **CMake**: 3.22.1+

---

## Privacy & Offline Architecture

VoiceJournal follows a strict offline-first philosophy:

- **No user accounts or logins**
- **No cloud sync or remote servers**
- **No analytics, crashlytics, or telemetry**
- **No ads or third-party trackers**
- **No audio uploaded**: voice recordings never leave your device
- **Zero storage permissions**: modern Android Photo Picker (`PickVisualMedia`) requires zero storage permissions
- **Local sandbox storage**: all database records and audio files remain strictly inside the Android application sandbox

### One-Time Whisper Model Download
To enable fully local AI speech recognition without bundling massive binaries into the repository:
1. On initial transcription (or via **Settings > General**), the app downloads the quantized Whisper Base Q5 bilingual model (~59.7 MB) via HTTPS from official mirrors.
2. The downloaded model is cryptographically verified against a strict **SHA-256 checksum** (`422f1ae452ade6f30a004d7e5c6a43195e4433bc370bf23fac9cc591f01a8898`).
3. Once verified, the model is stored locally on device. **The app never connects to the internet again** for daily note-taking, search, or transcription.

---

## Security & Architecture

VoiceJournal employs a defense-in-depth approach to protect user data and ensure architectural stability:

- **Keystore-Backed PIN Protection**: Access can be secured via a 4-digit PIN, heavily rate-limited (lockouts after 5 failed attempts) using standard `androidx.security.crypto` backed by the Android Keystore to prevent brute-force attacks.
- **Transaction-Based Integrity**: Rather than relying on rigid database-level `ForeignKey` constraints (which risk catastrophic migration failures on mismatched data), VoiceJournal guarantees integrity at the application layer. All multi-step operations (tag assignment, deletion, and folder categorization) are wrapped in robust Room `withTransaction` blocks. This was an intentional architectural decision to prioritize safe upgrades over theoretical database constraints.
- **Backup Protections**: Import and Export functionality is guarded against Zip Slip (path traversal) and Zip Bomb (decompression ratio limits and extraction size limits) attacks. Restores use a two-step staged process to ensure atomic safety, rolling back entirely if the import is corrupt.
- **Media Deletion Safety**: Deletions are transactional. The database is only updated if physical files (images/audio) are successfully deleted from internal storage, preventing orphan records and path traversal during deletion.
- **Whisper Integrity Verification**: The Whisper.cpp model is downloaded offline and verified against a trusted SHA-256 hash. Release builds strictly enforce this integrity check before executing the AI model.

---

## Roadmap

- [x] Audio recording (M4A / WAV)
- [x] Offline bilingual AI transcription (English & Hindi)
- [x] Audio playback with notification controls
- [x] Dark and light themes
- [x] Backup Import & Export with integrity rollback
- [x] Note tags and folder organization
- [ ] Multi-language transcription (Spanish, French, German, etc.)
- [ ] Individual note export & sharing
- [ ] Encrypted Backup & Restore

---

## Contributing

Contributions are welcome.

1. Fork the repository
2. Create a feature branch

```bash
git switch -c feature/your-feature
```

3. Commit your changes

```bash
git commit -m "Add awesome feature"
```

4. Push your branch

```bash
git push -u origin feature/your-feature
```

5. Open a Pull Request

---

## Acknowledgements

- OpenAI Whisper
- whisper.cpp
- Jetpack Compose
- Material Design 3
- Kotlin
- Android Open Source Project

---

## Author

**Raj Roshan**

GitHub: https://github.com/rajroshan110

---

<p align="center">
Built with ❤️ for people who value privacy.
</p>
