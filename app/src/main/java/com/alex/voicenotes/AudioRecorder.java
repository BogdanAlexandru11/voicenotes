package com.alex.voicenotes;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AudioRecorder {
    private static final String TAG = "AudioRecorder";
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private Thread recordingThread;
    private ByteArrayOutputStream audioData;

    public void startRecording() {
        if (isRecording) {
            Log.w(TAG, "Already recording");
            return;
        }

        int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        final int bufferSize = Math.max(minBufferSize, SAMPLE_RATE * 2);

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
        );

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord failed to initialize");
            return;
        }

        audioData = new ByteArrayOutputStream();
        isRecording = true;
        audioRecord.startRecording();

        recordingThread = new Thread(() -> {
            byte[] buffer = new byte[bufferSize];
            while (isRecording) {
                int bytesRead = audioRecord.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    synchronized (audioData) {
                        audioData.write(buffer, 0, bytesRead);
                    }
                }
            }
        }, "AudioRecorder");
        recordingThread.start();

        Log.d(TAG, "Recording started");
    }

    public byte[] stopRecording() {
        if (!isRecording) {
            Log.w(TAG, "Not recording");
            return new byte[0];
        }

        isRecording = false;

        try {
            recordingThread.join(1000);
        } catch (InterruptedException e) {
            Log.e(TAG, "Error waiting for recording thread", e);
        }

        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }

        byte[] result;
        synchronized (audioData) {
            result = audioData.toByteArray();
        }
        Log.d(TAG, "Recording stopped, bytes: " + result.length);
        return result;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public static float[] pcmToFloat(byte[] pcmData) {
        int numSamples = pcmData.length / 2;
        float[] floatData = new float[numSamples];
        ByteBuffer byteBuffer = ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < numSamples; i++) {
            short sample = byteBuffer.getShort();
            floatData[i] = sample / 32768.0f;
        }
        return floatData;
    }

    public static void saveWavFile(byte[] pcmData, File outputFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            int dataLength = pcmData.length;
            int totalLength = dataLength + 36;

            fos.write(new byte[]{'R', 'I', 'F', 'F'});
            fos.write(intToBytes(totalLength));
            fos.write(new byte[]{'W', 'A', 'V', 'E'});
            fos.write(new byte[]{'f', 'm', 't', ' '});
            fos.write(intToBytes(16));
            fos.write(shortToBytes((short) 1));
            fos.write(shortToBytes((short) 1));
            fos.write(intToBytes(SAMPLE_RATE));
            fos.write(intToBytes(SAMPLE_RATE * 2));
            fos.write(shortToBytes((short) 2));
            fos.write(shortToBytes((short) 16));
            fos.write(new byte[]{'d', 'a', 't', 'a'});
            fos.write(intToBytes(dataLength));
            fos.write(pcmData);
        }
    }

    private static byte[] intToBytes(int value) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
    }

    private static byte[] shortToBytes(short value) {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array();
    }
}
