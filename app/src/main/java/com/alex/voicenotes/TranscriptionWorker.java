package com.alex.voicenotes;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class TranscriptionWorker extends Worker {
    private static final String TAG = "TranscriptionWorker";
    public static final String KEY_AUDIO_FILE_PATH = "audio_file_path";

    public TranscriptionWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String audioFilePath = getInputData().getString(KEY_AUDIO_FILE_PATH);
        if (audioFilePath == null) {
            Log.e(TAG, "No audio file path provided");
            return Result.failure();
        }

        File audioFile = new File(audioFilePath);
        if (!audioFile.exists()) {
            Log.e(TAG, "Audio file does not exist: " + audioFilePath);
            return Result.failure();
        }

        try {
            byte[] pcmData = readFile(audioFile);
            if (pcmData.length == 0) {
                Log.e(TAG, "Audio file is empty");
                audioFile.delete();
                return Result.failure();
            }

            float[] audioSamples = pcmToFloat(pcmData);
            Log.d(TAG, "Transcribing " + audioSamples.length + " samples (" + (audioSamples.length / 16000.0) + " seconds)");

            WhisperTranscriber transcriber = new WhisperTranscriber();
            transcriber.initialize(getApplicationContext());
            String transcription = transcriber.transcribe(audioSamples);
            transcriber.release();

            audioFile.delete();

            if (transcription == null || transcription.isEmpty()) {
                Log.d(TAG, "No transcription result");
                sendErrorBroadcast("No speech detected");
                return Result.success();
            }

            Log.d(TAG, "Transcription result: " + transcription);
            File noteFile = FileHelper.saveNote(getApplicationContext(), transcription);

            if (noteFile != null) {
                Intent broadcast = new Intent(VoiceRecordingService.BROADCAST_NOTE_SAVED);
                broadcast.putExtra(VoiceRecordingService.EXTRA_FILENAME, noteFile.getName());
                getApplicationContext().sendBroadcast(broadcast);
            }

            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Transcription failed", e);
            audioFile.delete();
            sendErrorBroadcast("Transcription failed: " + e.getMessage());
            return Result.failure();
        }
    }

    private void sendErrorBroadcast(String message) {
        Intent errorIntent = new Intent(VoiceRecordingService.BROADCAST_ERROR);
        errorIntent.putExtra(VoiceRecordingService.EXTRA_ERROR_MESSAGE, message);
        getApplicationContext().sendBroadcast(errorIntent);
    }

    private byte[] readFile(File file) throws IOException {
        byte[] data = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            fis.read(data);
        }
        return data;
    }

    private float[] pcmToFloat(byte[] pcmData) {
        int numSamples = pcmData.length / 2;
        float[] floatData = new float[numSamples];
        ByteBuffer byteBuffer = ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < numSamples; i++) {
            short sample = byteBuffer.getShort();
            floatData[i] = sample / 32768.0f;
        }
        return floatData;
    }
}
