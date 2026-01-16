package com.alex.voicenotes;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LogViewerActivity extends AppCompatActivity {

    private TextView textLogs;
    private ScrollView scrollView;
    private Handler handler;
    private Runnable refreshRunnable;
    private boolean autoScroll = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_viewer);

        textLogs = findViewById(R.id.textLogs);
        scrollView = findViewById(R.id.scrollView);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnCopy).setOnClickListener(v -> copyLogs());
        findViewById(R.id.btnClear).setOnClickListener(v -> clearLogs());

        handler = new Handler(Looper.getMainLooper());
        refreshRunnable = this::refreshLogs;

        refreshLogs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.postDelayed(refreshRunnable, 1000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(refreshRunnable);
    }

    private void refreshLogs() {
        String logs = LogHelper.getLogsAsString();
        if (logs.isEmpty()) {
            textLogs.setText(R.string.no_logs);
        } else {
            textLogs.setText(logs);
            if (autoScroll) {
                scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
            }
        }
        handler.postDelayed(refreshRunnable, 1000);
    }

    private void copyLogs() {
        String logs = LogHelper.getLogsAsString();
        if (!logs.isEmpty()) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("VoiceNotes Logs", logs));
            Toast.makeText(this, R.string.logs_copied, Toast.LENGTH_SHORT).show();
        }
    }

    private void clearLogs() {
        LogHelper.clear();
        refreshLogs();
    }
}
