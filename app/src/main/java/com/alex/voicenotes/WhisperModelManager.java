package com.alex.voicenotes;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class WhisperModelManager {
    private static final String TAG = "WhisperModelManager";
    private static final String MODEL_NAME = "ggml-tiny.bin";
    private static final String ASSETS_PATH = "models/" + MODEL_NAME;

    public static File getModelFile(Context context) {
        return new File(context.getFilesDir(), MODEL_NAME);
    }

    public static boolean isModelAvailable(Context context) {
        File modelFile = getModelFile(context);
        return modelFile.exists() && modelFile.length() > 0;
    }

    public static void copyModelFromAssets(Context context) throws IOException {
        File modelFile = getModelFile(context);
        if (modelFile.exists()) {
            LogHelper.d(TAG, "Model already exists at " + modelFile.getAbsolutePath());
            return;
        }

        LogHelper.d(TAG, "Copying model from assets to " + modelFile.getAbsolutePath());
        try (InputStream is = context.getAssets().open(ASSETS_PATH);
             FileOutputStream fos = new FileOutputStream(modelFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            fos.flush();
        }
        LogHelper.d(TAG, "Model copied successfully, size: " + modelFile.length());
    }

    public static void ensureModelAvailable(Context context) throws IOException {
        if (!isModelAvailable(context)) {
            copyModelFromAssets(context);
        }
    }
}
