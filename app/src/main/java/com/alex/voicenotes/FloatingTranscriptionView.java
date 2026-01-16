package com.alex.voicenotes;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public class FloatingTranscriptionView {

    private final Context context;
    private final WindowManager windowManager;
    private View floatingView;
    private TextView transcriptionText;
    private boolean isShowing = false;

    public FloatingTranscriptionView(Context context) {
        this.context = context;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    public static boolean canDrawOverlays(Context context) {
        return Settings.canDrawOverlays(context);
    }

    public void show() {
        if (isShowing || !canDrawOverlays(context)) {
            return;
        }

        floatingView = LayoutInflater.from(context).inflate(R.layout.floating_transcription, null);
        transcriptionText = floatingView.findViewById(R.id.transcription_text);

        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.y = 100;

        floatingView.setAlpha(0f);
        windowManager.addView(floatingView, params);
        floatingView.animate().alpha(1f).setDuration(200);
        isShowing = true;
    }

    public void updateText(String text) {
        if (transcriptionText != null && text != null) {
            transcriptionText.setText(text.isEmpty() ? context.getString(R.string.listening) : text);
        }
    }

    public void hide() {
        if (!isShowing || floatingView == null) {
            return;
        }

        isShowing = false;
        final View viewToRemove = floatingView;
        floatingView = null;
        transcriptionText = null;

        viewToRemove.animate()
                .alpha(0f)
                .setDuration(150)
                .withEndAction(() -> {
                    try {
                        windowManager.removeView(viewToRemove);
                    } catch (Exception ignored) {
                    }
                });
    }

    public boolean isShowing() {
        return isShowing;
    }
}
