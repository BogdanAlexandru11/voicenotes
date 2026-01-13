# VoiceNotes

A simple Android app for capturing voice notes using local Whisper transcription.

## Features

- **Local Whisper transcription** - Uses whisper.cpp for fully offline, accurate speech-to-text
- **No speech loss** - Records audio first, then transcribes (handles pauses perfectly)
- **Background transcription** - Queue multiple recordings while transcription happens in background
- **Home screen widget** - Quick access to start/stop recording
- **Configurable save location** - Choose where to save your notes
- **Markdown output** - Notes saved as `.md` files with timestamp headers
- **Month grouping** - Notes organized by month with sticky headers
- **Pull-to-refresh** - Manually refresh notes list

## Requirements

- Android 8.0 (API 26) or higher
- ARM device (arm64-v8a or armeabi-v7a)
- ~75MB storage for Whisper model

## Setup

### Download Whisper Model

Before building, download the Whisper tiny model:

```bash
mkdir -p app/src/main/assets/models
curl -L -o app/src/main/assets/models/ggml-tiny.bin \
  https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin
```

## File Format

Notes are saved as markdown files with the format:
- Filename: `YYYY-MM-DD_HH-mm-ss.md`
- Content:
  ```markdown
  # YYYY-MM-DD HH:mm:ss

  [transcribed text]
  ```

## Building & Installation

### Prerequisites

- Java 17 or higher
- Android SDK with NDK 25.2.9519653
- CMake (via Android Studio SDK Manager)
- USB debugging enabled on your device

### Build Debug APK

```bash
./gradlew assembleDebug
```

APK location: `app/build/outputs/apk/debug/app-debug.apk`

### Build & Install (connected device)

```bash
./gradlew installDebug
```

## Usage

1. Tap the microphone button or use the home screen widget to start recording
2. Speak naturally - pauses don't affect transcription quality
3. Tap "Done" when finished
4. Recording stops immediately; transcription happens in background
5. Pull down to refresh the notes list once transcription completes

## Architecture

```
VoiceNotesWidget / MainActivity
    │ Intent
    ▼
VoiceRecordingService (Foreground Service)
    │ AudioRecorder (PCM 16kHz)
    │ Saves to cache/audio_queue/
    ▼
TranscriptionWorker (WorkManager)
    │ WhisperTranscriber (whisper.cpp)
    │ FileHelper.saveNote()
    ▼
[Configurable folder]/*.md
```

## Settings

- **Save location** - Configure a custom folder for saving notes (useful for syncing with Obsidian, Syncthing, etc.)
- **App lock** - Biometric authentication

## License

MIT
