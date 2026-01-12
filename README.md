# VoiceNotes

A simple Android app for capturing voice notes using on-device speech-to-text.

## Features

- **On-device transcription** - Uses Android's built-in speech recognizer (works offline on Pixel devices)
- **Continuous recording** - Handles pauses and breaks in speech automatically
- **Home screen widget** - Quick access to start/stop recording
- **Configurable save location** - Choose where to save your notes
- **Markdown output** - Notes saved as `.md` files with timestamp headers
- **Month grouping** - Notes organized by month with sticky headers

## Requirements

- Android 8.0 (API 26) or higher
- Microphone permission
- For offline transcription: Pixel device with on-device speech recognition

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
- Android SDK (via Android Studio or command line tools)
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

### Manual Installation via ADB

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Build Release APK

```bash
./gradlew assembleRelease
```

Note: Release builds require signing configuration in `app/build.gradle`.

## Usage

1. Tap the microphone button or use the home screen widget to start recording
2. Speak naturally - the app handles pauses automatically
3. Tap "Done" when finished
4. Notes appear in the list and are saved to your configured folder

## Settings

- **Save location** - Configure a custom folder for saving notes (useful for syncing with Obsidian, Syncthing, etc.)

## License

MIT
