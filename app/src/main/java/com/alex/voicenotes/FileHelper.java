package com.alex.voicenotes;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FileHelper {

    private static final String TAG = "FileHelper";
    private static final String FOLDER_NAME = "VoiceNotes";
    private static final String PREFS_NAME = "VoiceNotesPrefs";
    private static final String PREF_SAVE_URI = "save_uri";

    public static File getNotesDirectory() {
        File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File notesDir = new File(documentsDir, FOLDER_NAME);
        if (!notesDir.exists()) {
            if (!notesDir.mkdirs()) {
                Log.e(TAG, "Failed to create notes directory");
                return null;
            }
        }
        return notesDir;
    }

    public static String getSavedUri(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(PREF_SAVE_URI, null);
    }

    public static void setSavedUri(Context context, String uri) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(PREF_SAVE_URI, uri).apply();
    }

    public static File saveNote(Context context, String content) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }

        String savedUri = getSavedUri(context);
        if (savedUri != null) {
            return saveNoteToUri(context, Uri.parse(savedUri), content);
        }

        return saveNoteToDefaultFolder(content);
    }

    private static File saveNoteToDefaultFolder(String content) {
        File dir = getNotesDirectory();
        if (dir == null) {
            return null;
        }
        String filename = generateFilename();
        File file = new File(dir, filename);
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")) {
            writer.write(generateHeader());
            writer.write(content);
            return file;
        } catch (Exception e) {
            Log.e(TAG, "Failed to save note", e);
            return null;
        }
    }

    private static File saveNoteToUri(Context context, Uri treeUri, String content) {
        try {
            DocumentFile dir = DocumentFile.fromTreeUri(context, treeUri);
            if (dir == null || !dir.canWrite()) {
                Log.e(TAG, "Cannot write to URI");
                return saveNoteToDefaultFolder(content);
            }

            String filename = generateFilename();
            DocumentFile newFile = dir.createFile("text/markdown", filename.replace(".md", ""));
            if (newFile == null) {
                Log.e(TAG, "Failed to create file");
                return saveNoteToDefaultFolder(content);
            }

            try (OutputStream os = context.getContentResolver().openOutputStream(newFile.getUri());
                 OutputStreamWriter writer = new OutputStreamWriter(os, "UTF-8")) {
                writer.write(generateHeader());
                writer.write(content);
                return new File(newFile.getUri().getPath());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to save to URI", e);
            return saveNoteToDefaultFolder(content);
        }
    }

    public static List<Note> getAllNotes(Context context) {
        List<Note> notes = new ArrayList<>();

        String savedUri = getSavedUri(context);
        if (savedUri != null) {
            notes.addAll(getNotesFromUri(context, Uri.parse(savedUri)));
        }

        notes.addAll(getNotesFromDefaultFolder());

        Collections.sort(notes, (a, b) -> Long.compare(b.getLastModified(), a.getLastModified()));
        return notes;
    }

    private static List<Note> getNotesFromDefaultFolder() {
        List<Note> notes = new ArrayList<>();
        File dir = getNotesDirectory();
        if (dir == null || !dir.exists()) {
            return notes;
        }
        File[] files = dir.listFiles((d, name) -> name.endsWith(".md") || name.endsWith(".txt"));
        if (files == null) {
            return notes;
        }
        for (File file : files) {
            notes.add(new Note(file));
        }
        return notes;
    }

    private static List<Note> getNotesFromUri(Context context, Uri treeUri) {
        List<Note> notes = new ArrayList<>();
        try {
            DocumentFile dir = DocumentFile.fromTreeUri(context, treeUri);
            if (dir == null) {
                return notes;
            }
            for (DocumentFile file : dir.listFiles()) {
                if (file.isFile() && file.getName() != null && (file.getName().endsWith(".md") || file.getName().endsWith(".txt"))) {
                    notes.add(new Note(new File(file.getUri().toString()), file.getName(), file.lastModified(), context, file.getUri()));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to read from URI", e);
        }
        return notes;
    }

    public static String readNoteContent(File file) {
        if (file == null || !file.exists()) {
            return "";
        }
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (content.length() > 0) {
                    content.append("\n");
                }
                content.append(line);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to read note", e);
        }
        return content.toString();
    }

    public static String readNoteContent(Context context, Uri uri) {
        StringBuilder content = new StringBuilder();
        try (InputStream is = context.getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (content.length() > 0) {
                    content.append("\n");
                }
                content.append(line);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to read note from URI", e);
        }
        return content.toString();
    }

    public static boolean deleteNote(File file) {
        if (file == null || !file.exists()) {
            return false;
        }
        return file.delete();
    }

    public static boolean deleteNote(Context context, Uri uri) {
        try {
            DocumentFile file = DocumentFile.fromSingleUri(context, uri);
            if (file != null) {
                return file.delete();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to delete note from URI", e);
        }
        return false;
    }

    private static final SimpleDateFormat FILENAME_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.UK);
    private static final SimpleDateFormat HEADER_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.UK);

    private static String generateFilename() {
        return FILENAME_FORMAT.format(new Date()) + ".md";
    }

    private static String generateHeader() {
        return "# " + HEADER_FORMAT.format(new Date()) + "\n\n";
    }

    public static String getDisplayPath(Context context, Uri uri) {
        try {
            DocumentFile dir = DocumentFile.fromTreeUri(context, uri);
            if (dir != null) {
                return dir.getName();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get display path", e);
        }
        return uri.getLastPathSegment();
    }
}
