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
![Min SDK](https://img.shields.io/badge/Min%20SDK-26-blue)
![Kotlin](https://img.shields.io/badge/Kotlin-100%25-purple)
![Material 3](https://img.shields.io/badge/Material%203-Jetpack%20Compose-blue)
![Offline](https://img.shields.io/badge/Offline-First-success)

</p>

---

## Overview

VoiceJournal is a modern Android application that lets you record voice notes and automatically transcribe them **entirely on your device** using Whisper.cpp.

No cloud processing.

No accounts.

No subscriptions.

Your recordings and transcripts remain on your device unless you explicitly export them.

---

## Features

- 🎤 Record voice notes
- 🤖 Offline AI transcription (Whisper.cpp)
- 🔒 Privacy-first architecture
- 📱 Material Design 3 interface
- 🌙 Dark mode support
- ♿ Accessibility support
- 📂 Import & Export journals
- 🔍 Search transcripts
- 📝 Edit journal entries
- ▶️ Built-in audio playback
- 💾 Local storage
- ⚡ Fast and lightweight

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
├── service/          # Background services
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

- Android 8.0+
- Android Studio
- Gradle
- NDK (for Whisper.cpp)
- CMake

---

## Privacy

VoiceJournal follows an offline-first philosophy.

- No user accounts
- No cloud sync
- No cloud analytics
- No ads
- No audio uploaded
- Local processing only
- Stored inside Android app sandbox
- Never leaves your device unless exported

Your data belongs to you.

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

- [x] Audio recording
- [x] Offline transcription
- [x] Audio playback
- [x] Dark mode
- [x] Import & Export
- [x] Note tags
- [ ] Tags and Folder organization
- [ ] Individual note sharing
- [ ] Multi-language transcription
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
