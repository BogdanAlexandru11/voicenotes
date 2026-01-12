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

VoiceNotes is a minimal Android app for capturing voice notes using on-device speech-to-text. Optimized for Pixel devices with on-device speech recognition (Android 8.0+).

**Core flow:**
- Home screen widget triggers VoiceRecordingService via Intent
- Service uses SpeechRecognizer for on-device transcription
- Transcribed text saved as timestamped `.md` files to configurable folder
- MainActivity displays notes grouped by month with sticky headers

## Architecture

```
VoiceNotesWidget (AppWidgetProvider)
    │ Intent
    ▼
VoiceRecordingService (Foreground Service)
    │ SpeechRecognizer
    │ FileHelper.saveNote()
    ▼
[Configurable folder]/*.md
    │
    ▼
MainActivity (RecyclerView with month grouping)
```

## Key Classes (to be implemented in `com.alex.voicenotes`)

| File | Purpose |
|------|---------|
| `MainActivity.java` | Notes list with RecyclerView, FAB to record |
| `VoiceRecordingService.java` | Foreground service managing SpeechRecognizer |
| `VoiceNotesWidget.java` | Home screen widget with record/stop toggle |
| `FileHelper.java` | File I/O to configurable storage location |
| `Note.java` | Data class for notes |
| `ListItem.java` | Wrapper for grouped list items (headers + notes) |
| `StickyHeaderDecoration.java` | RecyclerView decoration for sticky month headers |

## Implementation Details

- **Package:** `com.alex.voicenotes` (not `com.example.voicenotes`)
- **Speech recognition:** Use `SpeechRecognizer.createOnDeviceSpeechRecognizer()` for offline Pixel model
- **File naming:** `yyyy-MM-dd_HH-mm-ss.md` format
- **Storage:** Configurable via Settings, defaults to Documents folder
- **Service broadcasts:** `RECORDING_STARTED`, `RECORDING_STOPPED`, `NOTE_SAVED`, `ERROR`

## Required Permissions

```
RECORD_AUDIO, FOREGROUND_SERVICE, FOREGROUND_SERVICE_MICROPHONE, POST_NOTIFICATIONS
```

## Implementation Order

1. Project setup + permissions in AndroidManifest
2. FileHelper and Note classes
3. VoiceRecordingService with SpeechRecognizer
4. MainActivity UI with RecyclerView
5. VoiceNotesWidget
6. Polish and error handling

## Reference

See `instructions.md` for complete technical specification including layouts, component specs, and testing checklist.
