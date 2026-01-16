package com.alex.voicenotes;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private TextView textCurrentFolder;
    private View resetFolder;
    private ActivityResultLauncher<Uri> folderPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        textCurrentFolder = findViewById(R.id.textCurrentFolder);
        resetFolder = findViewById(R.id.resetFolder);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.folderSetting).setOnClickListener(v -> openFolderPicker());
        resetFolder.setOnClickListener(v -> resetToDefault());
        findViewById(R.id.viewLogs).setOnClickListener(v -> startActivity(new Intent(this, LogViewerActivity.class)));

        folderPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocumentTree(),
                uri -> {
                    if (uri != null) {
                        getContentResolver().takePersistableUriPermission(uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        FileHelper.setSavedUri(this, uri.toString());
                        updateUI();
                    }
                }
        );

        updateUI();
    }

    private void openFolderPicker() {
        folderPickerLauncher.launch(null);
    }

    private void resetToDefault() {
        FileHelper.setSavedUri(this, null);
        updateUI();
    }

    private void updateUI() {
        String savedUri = FileHelper.getSavedUri(this);
        if (savedUri != null) {
            String displayPath = FileHelper.getDisplayPath(this, Uri.parse(savedUri));
            textCurrentFolder.setText(displayPath);
            resetFolder.setVisibility(View.VISIBLE);
        } else {
            textCurrentFolder.setText(R.string.default_folder);
            resetFolder.setVisibility(View.GONE);
        }
    }
}
