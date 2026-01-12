package com.alex.voicenotes;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import java.io.File;
import java.util.ArrayList;

public class VoiceRecordingService extends Service {

    private static final String TAG = "VoiceRecordingService";

    public static final String ACTION_START_RECORDING = "com.alex.voicenotes.START_RECORDING";
    public static final String ACTION_STOP_RECORDING = "com.alex.voicenotes.STOP_RECORDING";
    public static final String ACTION_PAUSE_RECORDING = "com.alex.voicenotes.PAUSE_RECORDING";
    public static final String ACTION_RESUME_RECORDING = "com.alex.voicenotes.RESUME_RECORDING";

    public static final String BROADCAST_RECORDING_STARTED = "com.alex.voicenotes.RECORDING_STARTED";
    public static final String BROADCAST_RECORDING_STOPPED = "com.alex.voicenotes.RECORDING_STOPPED";
    public static final String BROADCAST_NOTE_SAVED = "com.alex.voicenotes.NOTE_SAVED";
    public static final String BROADCAST_ERROR = "com.alex.voicenotes.ERROR";
    public static final String EXTRA_FILENAME = "filename";
    public static final String EXTRA_ERROR_MESSAGE = "error_message";

    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "voice_notes_recording";

    private static volatile boolean sIsRecording = false;
    private static volatile boolean sIsPaused = false;

    private SpeechRecognizer speechRecognizer;
    private StringBuilder transcriptionBuffer;
    private String lastPartialResult = "";
    private boolean isRecording = false;
    private boolean isPaused = false;
    private Handler mainHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
        Log.d(TAG, "Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Log.d(TAG, "Intent is null");
            return START_NOT_STICKY;
        }
        String action = intent.getAction();
        Log.d(TAG, "Received action: " + action);

        if (ACTION_START_RECORDING.equals(action)) {
            startRecording();
        } else if (ACTION_STOP_RECORDING.equals(action)) {
            stopRecording();
        } else if (ACTION_PAUSE_RECORDING.equals(action)) {
            pauseRecording();
        } else if (ACTION_RESUME_RECORDING.equals(action)) {
            resumeRecording();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service destroyed");
        stopRecording();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startRecording() {
        if (isRecording) {
            Log.d(TAG, "Already recording, ignoring start");
            return;
        }
        Log.d(TAG, "Starting recording");
        isRecording = true;
        isPaused = false;
        sIsRecording = true;
        sIsPaused = false;
        transcriptionBuffer = new StringBuilder();

        startForeground(NOTIFICATION_ID, createNotification());
        initializeSpeechRecognizer();
        startListening();

        sendBroadcast(BROADCAST_RECORDING_STARTED);
        VoiceNotesWidget.updateAllWidgets(this, true);
    }

    private void stopRecording() {
        if (!isRecording) {
            Log.d(TAG, "Not recording, ignoring stop");
            return;
        }
        Log.d(TAG, "Stopping recording");
        isRecording = false;
        isPaused = false;
        sIsRecording = false;
        sIsPaused = false;

        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }

        mainHandler.postDelayed(() -> {
            if (speechRecognizer != null) {
                speechRecognizer.destroy();
                speechRecognizer = null;
            }
            saveTranscription();
            sendBroadcast(BROADCAST_RECORDING_STOPPED);
            VoiceNotesWidget.updateAllWidgets(this, false);
            stopForeground(true);
            stopSelf();
        }, 1000);
    }

    private void pauseRecording() {
        if (!isRecording || isPaused) {
            return;
        }
        Log.d(TAG, "Pausing recording");
        isPaused = true;
        sIsPaused = true;
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }
        updateNotification(true);
    }

    private void resumeRecording() {
        if (!isRecording || !isPaused) {
            return;
        }
        Log.d(TAG, "Resuming recording");
        isPaused = false;
        sIsPaused = false;
        startListening();
        updateNotification(false);
    }

    private void initializeSpeechRecognizer() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        Log.d(TAG, "Initializing speech recognizer");
        if (SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) {
            Log.d(TAG, "Using on-device recognition");
            speechRecognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(this);
        } else {
            Log.d(TAG, "Using cloud recognition");
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        }
        speechRecognizer.setRecognitionListener(new SpeechListener());
    }

    private void startListening() {
        if (!isRecording || isPaused || speechRecognizer == null) {
            Log.d(TAG, "Cannot start listening: recording=" + isRecording + ", paused=" + isPaused + ", recognizer=" + (speechRecognizer != null));
            return;
        }
        Log.d(TAG, "Starting to listen");
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 30000);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 10000);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 10000);
        try {
            speechRecognizer.startListening(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error starting listening", e);
        }
    }

    private void saveTranscription() {
        if (!lastPartialResult.isEmpty()) {
            if (transcriptionBuffer.length() > 0) {
                transcriptionBuffer.append(" ");
            }
            transcriptionBuffer.append(lastPartialResult);
            lastPartialResult = "";
        }
        String content = transcriptionBuffer.toString().trim();
        Log.d(TAG, "Saving transcription, length=" + content.length());
        if (content.isEmpty()) {
            Log.d(TAG, "No content to save");
            mainHandler.post(() -> {
                Intent errorIntent = new Intent(BROADCAST_ERROR);
                errorIntent.putExtra(EXTRA_ERROR_MESSAGE, getString(R.string.no_speech_detected));
                sendBroadcast(errorIntent);
            });
            return;
        }
        File file = FileHelper.saveNote(this, content);
        if (file != null) {
            Log.d(TAG, "Note saved to " + file.getAbsolutePath());
            Intent broadcast = new Intent(BROADCAST_NOTE_SAVED);
            broadcast.putExtra(EXTRA_FILENAME, file.getName());
            sendBroadcast(broadcast);
        } else {
            Log.e(TAG, "Failed to save note");
        }
    }

    private Notification createNotification() {
        return createNotification(false);
    }

    private Notification createNotification(boolean paused) {
        Intent stopIntent = new Intent(this, VoiceRecordingService.class);
        stopIntent.setAction(ACTION_STOP_RECORDING);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent openPendingIntent = PendingIntent.getActivity(
                this, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = paused ? getString(R.string.paused) : getString(R.string.recording);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setSmallIcon(R.drawable.ic_mic)
                .setContentIntent(openPendingIntent)
                .addAction(R.drawable.ic_stop, getString(R.string.stop), stopPendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void updateNotification(boolean paused) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, createNotification(paused));
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(getString(R.string.notification_channel_description));
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void sendBroadcast(String action) {
        Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    public static boolean isRecording() {
        return sIsRecording;
    }

    public static boolean isPaused() {
        return sIsPaused;
    }

    private class SpeechListener implements RecognitionListener {

        @Override
        public void onReadyForSpeech(Bundle params) {
            Log.d(TAG, "Ready for speech");
        }

        @Override
        public void onBeginningOfSpeech() {
            Log.d(TAG, "Beginning of speech");
        }

        @Override
        public void onRmsChanged(float rmsdB) {
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
        }

        @Override
        public void onEndOfSpeech() {
            Log.d(TAG, "End of speech");
        }

        @Override
        public void onError(int error) {
            String errorMsg = getErrorMessage(error);
            Log.d(TAG, "Speech error: " + error + " (" + errorMsg + ")");
            if (!isRecording || isPaused) {
                return;
            }
            if (error == SpeechRecognizer.ERROR_NO_MATCH ||
                    error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                mainHandler.postDelayed(() -> startListening(), 100);
            } else if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                mainHandler.postDelayed(() -> startListening(), 500);
            } else {
                mainHandler.postDelayed(() -> {
                    initializeSpeechRecognizer();
                    startListening();
                }, 500);
            }
        }

        private String getErrorMessage(int error) {
            switch (error) {
                case SpeechRecognizer.ERROR_AUDIO: return "Audio error";
                case SpeechRecognizer.ERROR_CLIENT: return "Client error";
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: return "Insufficient permissions";
                case SpeechRecognizer.ERROR_NETWORK: return "Network error";
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: return "Network timeout";
                case SpeechRecognizer.ERROR_NO_MATCH: return "No match";
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: return "Recognizer busy";
                case SpeechRecognizer.ERROR_SERVER: return "Server error";
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: return "Speech timeout";
                default: return "Unknown error";
            }
        }

        @Override
        public void onResults(Bundle results) {
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                String text = matches.get(0);
                if (transcriptionBuffer.length() > 0) {
                    transcriptionBuffer.append(" ");
                }
                transcriptionBuffer.append(text);
                Log.d(TAG, "Result: " + text);
                Log.d(TAG, "Buffer now: " + transcriptionBuffer.toString());
            }
            if (isRecording && !isPaused) {
                mainHandler.postDelayed(() -> startListening(), 100);
            }
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                String text = matches.get(0);
                Log.d(TAG, "Partial: " + text);
                if (text != null && !text.isEmpty()) {
                    lastPartialResult = text;
                }
            }
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
        }
    }
}
