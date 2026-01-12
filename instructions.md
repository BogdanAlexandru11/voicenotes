# VoiceNotes Android App - Technical Specification

## Overview

A minimal Android app for capturing voice notes using on-device speech-to-text, saving them as plain text files. Designed for personal use on a Pixel 7 Pro, not for Play Store distribution.

### Core Requirements
- Record voice and transcribe to text using Android's on-device SpeechRecognizer
- Save transcriptions as `.txt` files with timestamps to a local folder
- Home screen widget with Record/Stop toggle button
- Simple main activity to view list of saved notes
- No cloud services, no accounts, no internet required

### Target Device
- Google Pixel 7 Pro
- Android 13+ (API 33+)
- Minimum SDK: 26

---

## Project Setup

### Android Studio Configuration
- **Project name:** VoiceNotes
- **Package name:** com.alex.voicenotes
- **Language:** Java
- **Minimum SDK:** 26
- **Target SDK:** 34
- **Build system:** Gradle with Kotlin DSL or Groovy

### Dependencies (build.gradle)
```groovy
dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
}
```

No additional dependencies required - uses native Android APIs only.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    VoiceNotesWidget                          │
│         (AppWidgetProvider on home screen)                   │
│         [  ⏺ Record  ]  ←──toggles──→  [  ⏹ Stop  ]         │
└─────────────────────────┬───────────────────────────────────┘
                          │ sends Intent to start/stop
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                 VoiceRecordingService                        │
│            (Foreground Service with notification)            │
│                                                              │
│  - Manages SpeechRecognizer lifecycle                        │
│  - Handles continuous listening until stopped                │
│  - Saves transcribed text via FileHelper                     │
│  - Broadcasts state changes to widget                        │
└─────────────────────────┬───────────────────────────────────┘
                          │ saves files to
                          ▼
┌─────────────────────────────────────────────────────────────┐
│    /storage/emulated/0/Documents/VoiceNotes/                 │
│                                                              │
│    2025-01-10_15-30-45.txt                                   │
│    2025-01-10_16-22-10.txt                                   │
│    ...                                                       │
└─────────────────────────────────────────────────────────────┘
                          │ reads files from
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    MainActivity                              │
│                                                              │
│  - Displays list of all saved notes                          │
│  - Shows timestamp and preview of each note                  │
│  - Tap to view full note content                             │
│  - Can also trigger recording from here                      │
└─────────────────────────────────────────────────────────────┘
```

---

## File Structure

```
app/src/main/
├── java/com/alex/voicenotes/
│   ├── MainActivity.java           # Main UI with notes list
│   ├── NoteDetailActivity.java     # View single note (optional, can use dialog)
│   ├── VoiceRecordingService.java  # Foreground service for speech recognition
│   ├── VoiceNotesWidget.java       # Home screen widget provider
│   ├── FileHelper.java             # Utility for file operations
│   └── Note.java                   # Simple data class for notes
│
├── res/
│   ├── layout/
│   │   ├── activity_main.xml       # Main screen layout
│   │   ├── note_list_item.xml      # RecyclerView item layout
│   │   ├── widget_layout.xml       # Widget layout (single button)
│   │   └── dialog_note_detail.xml  # Note detail dialog (optional)
│   │
│   ├── drawable/
│   │   ├── ic_mic.xml              # Microphone icon for record
│   │   ├── ic_stop.xml             # Stop icon
│   │   └── widget_background.xml   # Widget button background
│   │
│   ├── xml/
│   │   └── voice_notes_widget_info.xml  # Widget metadata
│   │
│   └── values/
│       ├── strings.xml
│       ├── colors.xml
│       └── themes.xml
│
└── AndroidManifest.xml
```

---

## Component Specifications

### 1. AndroidManifest.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- Permissions -->
    <uses-permission android:name="android.permission.RECORD_AUDIO"/>
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE"/>
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
    
    <!-- For saving to Documents folder on Android 10+ -->
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
        android:maxSdkVersion="28"/>
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32"/>

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.VoiceNotes"
        android:requestLegacyExternalStorage="true">

        <!-- Main Activity -->
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>

        <!-- Foreground Service for Recording -->
        <service
            android:name=".VoiceRecordingService"
            android:enabled="true"
            android:exported="false"
            android:foregroundServiceType="microphone"/>

        <!-- Home Screen Widget -->
        <receiver
            android:name=".VoiceNotesWidget"
            android:exported="true">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE"/>
            </intent-filter>
            <meta-data
                android:name="android.appwidget.provider"
                android:resource="@xml/voice_notes_widget_info"/>
        </receiver>

    </application>
</manifest>
```

---

### 2. FileHelper.java

Handles all file operations for saving and loading notes.

```java
package com.alex.voicenotes;

/**
 * Utility class for file operations.
 * 
 * Storage location: /storage/emulated/0/Documents/VoiceNotes/
 * This location is accessible to Syncthing and file managers.
 * 
 * File naming format: yyyy-MM-dd_HH-mm-ss.txt
 */
public class FileHelper {

    private static final String FOLDER_NAME = "VoiceNotes";
    
    /**
     * Get or create the VoiceNotes directory in Documents folder.
     * @return File object for the directory, or null if creation failed
     */
    public static File getNotesDirectory();
    
    /**
     * Save text content to a new note file.
     * @param content The transcribed text to save
     * @return The created File, or null if save failed
     */
    public static File saveNote(String content);
    
    /**
     * Get all saved notes, sorted by date descending (newest first).
     * @return List of Note objects
     */
    public static List<Note> getAllNotes();
    
    /**
     * Read the content of a specific note file.
     * @param file The note file to read
     * @return The text content, or empty string if read failed
     */
    public static String readNoteContent(File file);
    
    /**
     * Delete a note file.
     * @param file The note file to delete
     * @return true if deletion succeeded
     */
    public static boolean deleteNote(File file);
    
    /**
     * Generate a timestamp-based filename.
     * Format: yyyy-MM-dd_HH-mm-ss.txt
     */
    private static String generateFilename();
}
```

**Implementation notes:**
- Use `Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)` for the base path
- Create the VoiceNotes subdirectory if it doesn't exist
- Use `SimpleDateFormat` with pattern `"yyyy-MM-dd_HH-mm-ss"` and `Locale.UK` for filenames
- Handle IOExceptions gracefully, log errors

---

### 3. Note.java

Simple data class representing a voice note.

```java
package com.alex.voicenotes;

/**
 * Data class representing a single voice note.
 */
public class Note {
    private File file;
    private String filename;
    private Date timestamp;
    private String preview;  // First 100 chars of content
    
    // Constructor from File
    public Note(File file);
    
    // Getters
    public File getFile();
    public String getFilename();
    public Date getTimestamp();
    public String getPreview();
    public String getFormattedDate();  // Returns "Jan 10 · 3:30 PM" format
    
    // Parse timestamp from filename
    private Date parseTimestampFromFilename(String filename);
}
```

---

### 4. VoiceRecordingService.java

Foreground service that handles speech recognition.

```java
package com.alex.voicenotes;

/**
 * Foreground service for continuous speech recognition.
 * 
 * Uses Android's on-device SpeechRecognizer for offline transcription.
 * Runs as a foreground service to continue recording when app is backgrounded.
 * 
 * Intent Actions:
 * - ACTION_START_RECORDING: Begin listening
 * - ACTION_STOP_RECORDING: Stop and save
 * 
 * Broadcasts:
 * - BROADCAST_RECORDING_STARTED: Sent when recording begins
 * - BROADCAST_RECORDING_STOPPED: Sent when recording ends
 * - BROADCAST_NOTE_SAVED: Sent when a note is successfully saved (includes filename in extra)
 * - BROADCAST_ERROR: Sent on recognition errors (includes error message in extra)
 */
public class VoiceRecordingService extends Service {

    // Intent actions
    public static final String ACTION_START_RECORDING = "com.alex.voicenotes.START_RECORDING";
    public static final String ACTION_STOP_RECORDING = "com.alex.voicenotes.STOP_RECORDING";
    
    // Broadcast actions
    public static final String BROADCAST_RECORDING_STARTED = "com.alex.voicenotes.RECORDING_STARTED";
    public static final String BROADCAST_RECORDING_STOPPED = "com.alex.voicenotes.RECORDING_STOPPED";
    public static final String BROADCAST_NOTE_SAVED = "com.alex.voicenotes.NOTE_SAVED";
    public static final String BROADCAST_ERROR = "com.alex.voicenotes.ERROR";
    
    // Notification
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "voice_notes_recording";
    
    private SpeechRecognizer speechRecognizer;
    private StringBuilder transcriptionBuffer;  // Accumulates partial results
    private boolean isRecording = false;
    
    @Override
    public void onCreate();
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId);
    
    @Override
    public void onDestroy();
    
    @Override
    public IBinder onBind(Intent intent);  // Return null, not a bound service
    
    /**
     * Initialize the SpeechRecognizer with on-device recognition.
     * Use SpeechRecognizer.createOnDeviceSpeechRecognizer() for Pixel's offline model.
     */
    private void initializeSpeechRecognizer();
    
    /**
     * Start listening for speech input.
     * Creates recognition intent with:
     * - LANGUAGE_MODEL_FREE_FORM
     * - EXTRA_PARTIAL_RESULTS = true
     * - EXTRA_MAX_RESULTS = 1
     */
    private void startListening();
    
    /**
     * Stop listening and save accumulated transcription.
     */
    private void stopListening();
    
    /**
     * Save the current transcription buffer to a file.
     */
    private void saveTranscription();
    
    /**
     * Create and show the foreground notification.
     * Notification should have:
     * - "Recording..." title
     * - Stop action button
     * - Tap opens MainActivity
     */
    private Notification createNotification();
    
    /**
     * Create notification channel (required for Android 8+).
     */
    private void createNotificationChannel();
    
    /**
     * Send a local broadcast to update widget and activity.
     */
    private void sendBroadcast(String action);
    
    /**
     * Inner class implementing RecognitionListener.
     */
    private class SpeechListener implements RecognitionListener {
        @Override
        public void onReadyForSpeech(Bundle params);
        
        @Override
        public void onBeginningOfSpeech();
        
        @Override
        public void onRmsChanged(float rmsdB);  // Can use for visual feedback
        
        @Override
        public void onBufferReceived(byte[] buffer);
        
        @Override
        public void onEndOfSpeech();
        
        @Override
        public void onError(int error);
        // Handle errors gracefully:
        // ERROR_NO_MATCH - restart listening
        // ERROR_SPEECH_TIMEOUT - restart listening
        // Other errors - log and notify user
        
        @Override
        public void onResults(Bundle results);
        // Get results from SpeechRecognizer.RESULTS_RECOGNITION
        // Append to transcriptionBuffer
        // Restart listening for continuous mode
        
        @Override
        public void onPartialResults(Bundle partialResults);
        // Optional: can update notification with partial text
        
        @Override
        public void onEvent(int eventType, Bundle params);
    }
    
    /**
     * Static helper to check if service is currently recording.
     * Store state in a static variable or use SharedPreferences.
     */
    public static boolean isRecording();
}
```

**Implementation notes:**
- Use `SpeechRecognizer.createOnDeviceSpeechRecognizer(context)` for offline recognition on Pixel
- Handle `ERROR_NO_MATCH` and `ERROR_SPEECH_TIMEOUT` by restarting the listener (for continuous recording)
- Accumulate results in `transcriptionBuffer` since recognition may come in chunks
- Always run recognition on the main thread (SpeechRecognizer requirement)
- When stopping, save whatever is in the buffer even if partial

---

### 5. VoiceNotesWidget.java

Home screen widget with Record/Stop button.

```java
package com.alex.voicenotes;

/**
 * Home screen widget provider.
 * 
 * Displays a single button that toggles between Record and Stop states.
 * Communicates with VoiceRecordingService via Intents.
 */
public class VoiceNotesWidget extends AppWidgetProvider {

    public static final String ACTION_TOGGLE_RECORDING = "com.alex.voicenotes.TOGGLE_RECORDING";
    
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds);
    // Called when widget is first placed and on update intervals
    // Set up click listener for the button
    
    @Override
    public void onReceive(Context context, Intent intent);
    // Handle ACTION_TOGGLE_RECORDING
    // Check current recording state
    // Start or stop VoiceRecordingService accordingly
    // Update widget appearance
    
    /**
     * Update the widget UI to reflect current state.
     * @param isRecording true if currently recording
     */
    public static void updateWidget(Context context, boolean isRecording);
    // When recording: show Stop icon, red background
    // When idle: show Record icon, dark background
    
    /**
     * Get RemoteViews configured for the widget.
     */
    private static RemoteViews getRemoteViews(Context context, boolean isRecording);
    
    /**
     * Create a PendingIntent for the button click.
     */
    private static PendingIntent getTogglePendingIntent(Context context);
}
```

---

### 6. MainActivity.java

Main screen showing list of all saved notes.

```java
package com.alex.voicenotes;

/**
 * Main activity displaying list of saved voice notes.
 * 
 * Features:
 * - RecyclerView list of all notes
 * - Each item shows formatted date and content preview
 * - Tap item to view full content in dialog
 * - Long press to delete (with confirmation)
 * - FAB to start recording (alternative to widget)
 * - Receives broadcasts to refresh list when new note saved
 */
public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NotesAdapter adapter;
    private FloatingActionButton fabRecord;
    private BroadcastReceiver noteReceiver;
    
    @Override
    protected void onCreate(Bundle savedInstanceState);
    // Set up RecyclerView with adapter
    // Set up FAB click listener
    // Request permissions if needed (RECORD_AUDIO, POST_NOTIFICATIONS)
    
    @Override
    protected void onResume();
    // Refresh notes list
    // Register broadcast receiver for NOTE_SAVED
    
    @Override
    protected void onPause();
    // Unregister broadcast receiver
    
    /**
     * Request necessary permissions.
     * Required: RECORD_AUDIO
     * For Android 13+: POST_NOTIFICATIONS
     */
    private void requestPermissions();
    
    /**
     * Load all notes and update the adapter.
     */
    private void refreshNotesList();
    
    /**
     * Show dialog with full note content.
     */
    private void showNoteDetail(Note note);
    
    /**
     * Show confirmation dialog and delete note.
     */
    private void deleteNoteWithConfirmation(Note note);
    
    /**
     * Toggle recording state (start or stop service).
     */
    private void toggleRecording();
    
    /**
     * Update FAB appearance based on recording state.
     */
    private void updateFabState(boolean isRecording);
    
    /**
     * Inner adapter class for RecyclerView.
     */
    private class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.ViewHolder> {
        private List<Note> notes;
        
        public void setNotes(List<Note> notes);
        
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType);
        
        @Override
        public void onBindViewHolder(ViewHolder holder, int position);
        
        @Override
        public int getItemCount();
        
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textDate;
            TextView textPreview;
            // Bind click and long-click listeners
        }
    }
}
```

---

### 7. Layout Files

#### res/layout/activity_main.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout>
    
    <!-- Header with app title -->
    <TextView "Voice Notes" - large title at top />
    
    <!-- RecyclerView for notes list -->
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/recyclerNotes"
        fills remaining space />
    
    <!-- Empty state text shown when no notes -->
    <TextView 
        android:id="@+id/textEmpty"
        "No notes yet. Tap record to create one." 
        centered, shown when list empty />
    
    <!-- FAB for recording -->
    <com.google.android.material.floatingactionbutton.FloatingActionButton
        android:id="@+id/fabRecord"
        bottom right corner
        microphone icon />

</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

#### res/layout/note_list_item.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout vertical padding 16dp>
    
    <!-- Date/time -->
    <TextView 
        android:id="@+id/textDate"
        style="caption" gray color
        "Jan 10 · 3:30 PM" format />
    
    <!-- Content preview -->
    <TextView
        android:id="@+id/textPreview"
        style="body" 
        maxLines 3
        ellipsize end />

</LinearLayout>
```

#### res/layout/widget_layout.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout 
    android:background="@drawable/widget_background"
    android:padding="8dp">
    
    <ImageView
        android:id="@+id/widgetIcon"
        android:src="@drawable/ic_mic"
        centered />
    
    <TextView
        android:id="@+id/widgetText"
        android:text="Record"
        below icon, centered />

</FrameLayout>
```

#### res/xml/voice_notes_widget_info.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<appwidget-provider 
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="80dp"
    android:minHeight="80dp"
    android:updatePeriodMillis="0"
    android:initialLayout="@layout/widget_layout"
    android:resizeMode="none"
    android:widgetCategory="home_screen"
    android:previewImage="@drawable/ic_mic"
    android:description="@string/widget_description">
</appwidget-provider>
```

---

### 8. Drawable Resources

#### res/drawable/ic_mic.xml
Standard microphone icon - use Android Studio's Vector Asset tool to import Material icon "mic"

#### res/drawable/ic_stop.xml  
Standard stop icon - use Android Studio's Vector Asset tool to import Material icon "stop"

#### res/drawable/widget_background.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#1a1a1a"/>
    <corners android:radius="16dp"/>
</shape>
```

Also create `widget_background_recording.xml` with red color (#d32f2f) for recording state.

---

## Implementation Order

Build and test in this order:

### Step 1: Project setup and permissions
1. Create new Android Studio project with settings above
2. Add all permissions to AndroidManifest.xml
3. Create basic MainActivity that just requests permissions
4. Build and run - verify permissions work

### Step 2: FileHelper and Note classes
1. Implement FileHelper.java
2. Implement Note.java  
3. Test by creating a dummy note file and reading it back
4. Verify the folder is created at correct location

### Step 3: VoiceRecordingService (core functionality)
1. Implement service with notification channel
2. Add SpeechRecognizer initialization
3. Implement RecognitionListener
4. Test by starting service from MainActivity
5. Verify transcription is saved to file

### Step 4: MainActivity UI
1. Implement RecyclerView with adapter
2. Add FAB that starts/stops service
3. Implement note detail dialog
4. Add delete functionality
5. Register broadcast receiver for updates

### Step 5: Widget
1. Create widget layout and info XML
2. Implement VoiceNotesWidget provider
3. Add to manifest
4. Test adding widget to home screen
5. Verify it toggles recording correctly

### Step 6: Polish
1. Add proper error handling
2. Add visual feedback (recording indicator)
3. Handle edge cases (empty transcription, permissions denied)
4. Test with Syncthing

---

## Testing Checklist

- [ ] App installs on Pixel 7 Pro
- [ ] Permissions requested and granted
- [ ] Recording starts from MainActivity FAB
- [ ] Recording starts from home screen widget
- [ ] Speech is transcribed to text
- [ ] Text is saved to Documents/VoiceNotes/
- [ ] Filenames have correct timestamp format
- [ ] Notes list displays all saved notes
- [ ] Tapping note shows full content
- [ ] Long press deletes note (with confirmation)
- [ ] Widget updates to show recording state
- [ ] Notification shown during recording
- [ ] Recording continues when app backgrounded
- [ ] Syncthing can access the VoiceNotes folder
- [ ] On-device recognition works offline

---

## Known Limitations

1. **SpeechRecognizer timeout**: Android's SpeechRecognizer may stop after 10-15 seconds of silence. The service handles this by restarting the listener automatically.

2. **Maximum recording length**: Continuous recording works by restarting the recognizer on results/timeout. Very long recordings may have small gaps.

3. **On-device recognition quality**: Pixel's offline model is good but not as accurate as cloud-based. Acceptable for quick notes.

4. **Storage location**: Using Documents folder for easy Syncthing access. If this causes issues with Android 11+ scoped storage, fall back to app-specific external storage.

---

## Syncthing Configuration

Point Syncthing at this folder:
```
/storage/emulated/0/Documents/VoiceNotes/
```

Or if using app-specific storage:
```
/storage/emulated/0/Android/data/com.alex.voicenotes/files/VoiceNotes/
```

Notes will sync as plain .txt files to your server/laptop automatically.