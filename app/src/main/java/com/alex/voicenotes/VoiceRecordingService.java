package com.alex.voicenotes;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    private AudioRecorder audioRecorder;
    private boolean isRecording = false;
    private boolean isPaused = false;
    private Handler mainHandler;
    private AudioManager audioManager;
    private int originalVolume;
    private FloatingTranscriptionView floatingView;
    private ExecutorService backgroundExecutor;

    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler(Looper.getMainLooper());
        backgroundExecutor = Executors.newSingleThreadExecutor();
        createNotificationChannel();
        LogHelper.d(TAG, "Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            LogHelper.d(TAG, "Intent is null");
            return START_NOT_STICKY;
        }
        String action = intent.getAction();
        LogHelper.d(TAG, "Received action: " + action);

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
        LogHelper.d(TAG, "Service destroyed");
        if (isRecording) {
            stopRecording();
        }
        if (backgroundExecutor != null) {
            backgroundExecutor.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startRecording() {
        if (isRecording) {
            LogHelper.d(TAG, "Already recording, ignoring start");
            return;
        }
        LogHelper.d(TAG, "Starting recording");
        isRecording = true;
        isPaused = false;
        sIsRecording = true;
        sIsPaused = false;

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
        }

        startForeground(NOTIFICATION_ID, createNotification());

        if (FloatingTranscriptionView.canDrawOverlays(this)) {
            floatingView = new FloatingTranscriptionView(this);
            floatingView.show();
            floatingView.updateText(getString(R.string.recording));
        }

        backgroundExecutor.execute(() -> {
            try {
                WhisperModelManager.ensureModelAvailable(this);
                LogHelper.d(TAG, "Whisper model ready");
            } catch (Exception e) {
                LogHelper.e(TAG, "Failed to prepare Whisper model", e);
            }
        });

        audioRecorder = new AudioRecorder();
        audioRecorder.startRecording();

        sendBroadcast(BROADCAST_RECORDING_STARTED);
        VoiceNotesWidget.updateAllWidgets(this, true);
    }

    private void stopRecording() {
        if (!isRecording) {
            LogHelper.d(TAG, "Not recording, ignoring stop");
            return;
        }
        LogHelper.d(TAG, "Stopping recording");
        isRecording = false;
        isPaused = false;
        sIsRecording = false;
        sIsPaused = false;

        if (audioManager != null) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0);
        }

        if (floatingView != null) {
            floatingView.hide();
            floatingView = null;
        }

        byte[] pcmData = audioRecorder.stopRecording();

        if (pcmData.length > 0) {
            try {
                File audioFile = savePcmToFile(pcmData);
                enqueueTranscription(audioFile);
            } catch (IOException e) {
                LogHelper.e(TAG, "Failed to save audio file", e);
                sendErrorBroadcast("Failed to save recording");
            }
        } else {
            sendErrorBroadcast(getString(R.string.no_speech_detected));
        }

        finishService();
    }

    private File savePcmToFile(byte[] pcmData) throws IOException {
        File audioDir = new File(getCacheDir(), "audio_queue");
        if (!audioDir.exists()) {
            audioDir.mkdirs();
        }
        File audioFile = new File(audioDir, "recording_" + System.currentTimeMillis() + ".pcm");
        try (FileOutputStream fos = new FileOutputStream(audioFile)) {
            fos.write(pcmData);
        }
        LogHelper.d(TAG, "Saved audio to " + audioFile.getAbsolutePath() + " (" + pcmData.length + " bytes)");
        return audioFile;
    }

    private void enqueueTranscription(File audioFile) {
        Data inputData = new Data.Builder()
                .putString(TranscriptionWorker.KEY_AUDIO_FILE_PATH, audioFile.getAbsolutePath())
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(TranscriptionWorker.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(this)
                .enqueueUniqueWork("transcription", androidx.work.ExistingWorkPolicy.APPEND, workRequest);
        LogHelper.d(TAG, "Enqueued transcription for " + audioFile.getName());
    }

    private void sendErrorBroadcast(String message) {
        Intent errorIntent = new Intent(BROADCAST_ERROR);
        errorIntent.putExtra(EXTRA_ERROR_MESSAGE, message);
        sendBroadcast(errorIntent);
    }

    private void finishService() {
        sendBroadcast(BROADCAST_RECORDING_STOPPED);
        VoiceNotesWidget.updateAllWidgets(this, false);
        stopForeground(true);
        stopSelf();
    }

    private void pauseRecording() {
        LogHelper.d(TAG, "Pause not supported in audio recording mode");
    }

    private void resumeRecording() {
        LogHelper.d(TAG, "Resume not supported in audio recording mode");
    }

    private Notification createNotification() {
        Intent stopIntent = new Intent(this, VoiceRecordingService.class);
        stopIntent.setAction(ACTION_STOP_RECORDING);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent openPendingIntent = PendingIntent.getActivity(
                this, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.recording))
                .setSmallIcon(R.drawable.ic_mic)
                .setContentIntent(openPendingIntent)
                .addAction(R.drawable.ic_stop, getString(R.string.stop), stopPendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
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
}
