# VoiceNotes

A simple Android app for capturing voice notes using on-device speech-to-text.

## Features

- **On-device transcription** - Uses Android's built-in speech recognizer (works offline on Pixel devices)
- **Continuous recording** - Handles pauses and breaks in speech automatically
- **Home screen widget** - Quick access to start/stop recording
- **Configurable save location** - Choose where to save your notes
- **Markdown output** - Notes saved as `.md` files with timestamp headers

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

## Building

```bash
./gradlew assembleDebug
```

## Installation

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Usage

1. Tap the microphone button or use the home screen widget to start recording
2. Speak naturally - the app handles pauses automatically
3. Tap "Done" when finished
4. Notes appear in the list and are saved to your configured folder

## Settings

- **Save location** - Configure a custom folder for saving notes (useful for syncing with Obsidian, Syncthing, etc.)

## License

MIT
