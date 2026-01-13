# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew build              # Full build
./gradlew assembleDebug      # Debug APK only
./gradlew installDebug       # Build and install on connected device
./gradlew clean              # Clean build artifacts
./gradlew test               # Run unit tests
./gradlew connectedAndroidTest  # Run instrumentation tests
```

## Project Overview

VoiceNotes is a minimal Android app for capturing voice notes using local Whisper transcription via whisper.cpp. Records audio as PCM, then transcribes in background using WorkManager.

**Core flow:**
1. User starts recording via widget or FAB
2. VoiceRecordingService captures PCM audio at 16kHz
3. On stop, audio saved to cache and TranscriptionWorker queued
4. Service finishes immediately (user can start new recording)
5. TranscriptionWorker runs Whisper, saves note, broadcasts completion

## Architecture

```
VoiceNotesWidget / MainActivity
    │ Intent
    ▼
VoiceRecordingService (Foreground Service)
    │ AudioRecorder (PCM 16kHz mono)
    │ savePcmToFile() → cache/audio_queue/
    │ enqueueTranscription()
    ▼
TranscriptionWorker (WorkManager, sequential via APPEND)
    │ WhisperTranscriber → WhisperContext (whisper.cpp)
    │ FileHelper.saveNote()
    ▼
[Configurable folder]/*.md
```

## Key Classes

| File | Purpose |
|------|---------|
| `MainActivity.java` | Notes list with RecyclerView, FAB, pull-to-refresh |
| `VoiceRecordingService.java` | Foreground service, AudioRecorder, queues transcription |
| `AudioRecorder.java` | PCM audio capture at 16kHz mono |
| `TranscriptionWorker.java` | WorkManager worker for background Whisper transcription |
| `WhisperTranscriber.java` | Java wrapper for Kotlin WhisperContext |
| `WhisperModelManager.java` | Copies model from assets to internal storage |
| `VoiceNotesWidget.java` | Home screen widget with record/stop toggle |
| `FileHelper.java` | File I/O to configurable storage location |

## Whisper Library (lib module)

Native whisper.cpp integration:
- `lib/src/main/cpp/whisper-cpp/` - whisper.cpp source
- `lib/src/main/jni/whisper/` - JNI bindings and CMakeLists.txt
- `lib/src/main/java/com/whispercpp/whisper/` - Kotlin wrapper (LibWhisper, WhisperContext)

Model file: `app/src/main/assets/models/ggml-tiny.bin` (not in git, download from HuggingFace)

## Implementation Details

- **Package:** `com.alex.voicenotes`
- **Audio format:** PCM 16-bit, 16kHz, mono (Whisper requirement)
- **Transcription:** Sequential via WorkManager unique work with APPEND policy
- **File naming:** `yyyy-MM-dd_HH-mm-ss.md` format
- **Storage:** Configurable via Settings, defaults to Documents folder
- **Service broadcasts:** `RECORDING_STARTED`, `RECORDING_STOPPED`, `NOTE_SAVED`, `ERROR`

## Required Permissions

```
RECORD_AUDIO, FOREGROUND_SERVICE, FOREGROUND_SERVICE_MICROPHONE, POST_NOTIFICATIONS, SYSTEM_ALERT_WINDOW
```

## Dependencies

- whisper.cpp (lib module with NDK 25.2.9519653)
- WorkManager for background transcription
- SwipeRefreshLayout for pull-to-refresh
- Biometric for app lock
