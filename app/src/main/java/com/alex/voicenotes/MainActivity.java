package com.alex.voicenotes;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.material.button.MaterialButton;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;

    private RecyclerView recyclerView;
    private NotesAdapter adapter;
    private FloatingActionButton fabRecord;
    private TextView textEmpty;
    private ImageButton btnSettings;
    private View dimBackground;
    private View recordingSheet;
    private MaterialButton btnTimer;
    private MaterialButton btnDone;
    private BroadcastReceiver noteReceiver;

    private Handler timerHandler;
    private Runnable timerRunnable;
    private int elapsedSeconds = 0;
    private boolean isPaused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerNotes);
        fabRecord = findViewById(R.id.fabRecord);
        textEmpty = findViewById(R.id.textEmpty);
        btnSettings = findViewById(R.id.btnSettings);
        dimBackground = findViewById(R.id.dimBackground);
        recordingSheet = findViewById(R.id.recordingSheet);
        btnTimer = findViewById(R.id.btnTimer);
        btnDone = findViewById(R.id.btnDone);

        adapter = new NotesAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        timerHandler = new Handler(Looper.getMainLooper());

        fabRecord.setOnClickListener(v -> toggleRecording());
        btnSettings.setOnClickListener(v -> openSettings());
        btnTimer.setOnClickListener(v -> togglePause());
        btnDone.setOnClickListener(v -> stopRecording());

        noteReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (VoiceRecordingService.BROADCAST_NOTE_SAVED.equals(action)) {
                    refreshNotesList();
                    Toast.makeText(MainActivity.this, R.string.note_saved, Toast.LENGTH_SHORT).show();
                } else if (VoiceRecordingService.BROADCAST_RECORDING_STOPPED.equals(action)) {
                    refreshNotesList();
                    hideRecordingSheet();
                } else if (VoiceRecordingService.BROADCAST_RECORDING_STARTED.equals(action)) {
                    showRecordingSheet();
                } else if (VoiceRecordingService.BROADCAST_ERROR.equals(action)) {
                    String error = intent.getStringExtra(VoiceRecordingService.EXTRA_ERROR_MESSAGE);
                    if (error != null) {
                        Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                    }
                }
                updateFabState(VoiceRecordingService.isRecording());
            }
        };

        requestPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshNotesList();
        updateFabState(VoiceRecordingService.isRecording());

        if (VoiceRecordingService.isRecording()) {
            showRecordingSheet();
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(VoiceRecordingService.BROADCAST_NOTE_SAVED);
        filter.addAction(VoiceRecordingService.BROADCAST_RECORDING_STARTED);
        filter.addAction(VoiceRecordingService.BROADCAST_RECORDING_STOPPED);
        filter.addAction(VoiceRecordingService.BROADCAST_ERROR);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(noteReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(noteReceiver, filter);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(noteReceiver);
    }

    private void requestPermissions() {
        List<String> permissions = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void refreshNotesList() {
        List<Note> notes = FileHelper.getAllNotes(this);
        adapter.setNotes(notes);
        textEmpty.setVisibility(notes.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(notes.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void showNoteDetail(Note note) {
        String content;
        if (note.hasUri()) {
            content = FileHelper.readNoteContent(this, note.getUri());
        } else {
            content = FileHelper.readNoteContent(note.getFile());
        }
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_note_detail, null);
        TextView textContent = dialogView.findViewById(R.id.textContent);
        textContent.setText(content);

        new AlertDialog.Builder(this)
                .setTitle(note.getFormattedDate())
                .setView(dialogView)
                .setPositiveButton(R.string.close, null)
                .show();
    }

    private void deleteNoteWithConfirmation(Note note) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_note)
                .setMessage(R.string.delete_confirmation)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    if (note.hasUri()) {
                        FileHelper.deleteNote(this, note.getUri());
                    } else {
                        FileHelper.deleteNote(note.getFile());
                    }
                    refreshNotesList();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void toggleRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions();
            return;
        }

        if (VoiceRecordingService.isRecording()) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    private void startRecording() {
        Intent intent = new Intent(this, VoiceRecordingService.class);
        intent.setAction(VoiceRecordingService.ACTION_START_RECORDING);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        showRecordingSheet();
        updateFabState(true);
    }

    private void stopRecording() {
        Intent intent = new Intent(this, VoiceRecordingService.class);
        intent.setAction(VoiceRecordingService.ACTION_STOP_RECORDING);
        startService(intent);
        hideRecordingSheet();
        updateFabState(false);
        timerHandler.postDelayed(this::refreshNotesList, 1500);
    }

    private void togglePause() {
        Intent intent = new Intent(this, VoiceRecordingService.class);
        if (isPaused) {
            intent.setAction(VoiceRecordingService.ACTION_RESUME_RECORDING);
            isPaused = false;
            btnTimer.setIconResource(R.drawable.ic_pause);
            startTimer();
        } else {
            intent.setAction(VoiceRecordingService.ACTION_PAUSE_RECORDING);
            isPaused = true;
            btnTimer.setIconResource(R.drawable.ic_play);
            stopTimer();
        }
        startService(intent);
    }

    private void showRecordingSheet() {
        dimBackground.setVisibility(View.VISIBLE);
        recordingSheet.setVisibility(View.VISIBLE);
        fabRecord.setVisibility(View.GONE);
        elapsedSeconds = 0;
        isPaused = false;
        btnTimer.setText("00:00");
        btnTimer.setIconResource(R.drawable.ic_pause);
        startTimer();
    }

    private void hideRecordingSheet() {
        dimBackground.setVisibility(View.GONE);
        recordingSheet.setVisibility(View.GONE);
        fabRecord.setVisibility(View.VISIBLE);
        stopTimer();
    }

    private void startTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                elapsedSeconds++;
                int minutes = elapsedSeconds / 60;
                int seconds = elapsedSeconds % 60;
                btnTimer.setText(String.format(Locale.UK, "%02d:%02d", minutes, seconds));
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private void stopTimer() {
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void updateFabState(boolean isRecording) {
        if (isRecording) {
            fabRecord.setImageResource(R.drawable.ic_stop);
            fabRecord.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.destructive));
        } else {
            fabRecord.setImageResource(R.drawable.ic_mic);
            fabRecord.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.primary));
        }
    }

    private class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.ViewHolder> {

        private List<Note> notes = new ArrayList<>();

        public void setNotes(List<Note> notes) {
            this.notes = notes;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.note_list_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Note note = notes.get(position);
            holder.textDate.setText(note.getFormattedDate());
            holder.textPreview.setText(note.getPreview());

            holder.itemView.setOnClickListener(v -> showNoteDetail(note));
            holder.itemView.setOnLongClickListener(v -> {
                deleteNoteWithConfirmation(note);
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return notes.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textDate;
            TextView textPreview;

            ViewHolder(View itemView) {
                super(itemView);
                textDate = itemView.findViewById(R.id.textDate);
                textPreview = itemView.findViewById(R.id.textPreview);
            }
        }
    }
}
