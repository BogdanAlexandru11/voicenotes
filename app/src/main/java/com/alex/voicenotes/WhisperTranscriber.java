package com.alex.voicenotes;

import android.content.Context;
import android.util.Log;

import com.whispercpp.whisper.WhisperContext;

import java.io.File;

import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.BuildersKt;

public class WhisperTranscriber {
    private static final String TAG = "WhisperTranscriber";
    private WhisperContext whisperContext;

    public void initialize(Context context) throws Exception {
        WhisperModelManager.ensureModelAvailable(context);
        File modelFile = WhisperModelManager.getModelFile(context);
        Log.d(TAG, "Loading model from: " + modelFile.getAbsolutePath());
        whisperContext = WhisperContext.Companion.createContextFromFile(modelFile.getAbsolutePath());
        Log.d(TAG, "Whisper context initialized");
    }

    public String transcribe(float[] audioSamples) throws Exception {
        if (whisperContext == null) {
            throw new IllegalStateException("Whisper context not initialized");
        }

        Log.d(TAG, "Transcribing " + audioSamples.length + " samples");

        final String[] result = new String[1];
        final Throwable[] error = new Throwable[1];

        Thread transcribeThread = new Thread(() -> {
            try {
                Object transcriptionResult = BuildersKt.runBlocking(
                        EmptyCoroutineContext.INSTANCE,
                        (scope, continuation) -> whisperContext.transcribeData(audioSamples, false, continuation)
                );
                result[0] = transcriptionResult != null ? transcriptionResult.toString() : "";
            } catch (Throwable e) {
                error[0] = e;
            }
        });

        transcribeThread.start();
        transcribeThread.join();

        if (error[0] != null) {
            if (error[0] instanceof Exception) {
                throw (Exception) error[0];
            }
            throw new Exception("Transcription error", error[0]);
        }

        return result[0] != null ? result[0].trim() : "";
    }

    public void release() {
        if (whisperContext != null) {
            try {
                BuildersKt.runBlocking(
                        EmptyCoroutineContext.INSTANCE,
                        (scope, continuation) -> whisperContext.release(continuation)
                );
            } catch (Throwable e) {
                Log.e(TAG, "Error releasing context", e);
            }
            whisperContext = null;
        }
    }

    public boolean isInitialized() {
        return whisperContext != null;
    }
}
