package com.alex.voicenotes;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Note {

    private static final int PREVIEW_LENGTH = 100;
    private static final SimpleDateFormat FILENAME_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.UK);
    private static final SimpleDateFormat DISPLAY_FORMAT = new SimpleDateFormat("MMM d · h:mm a", Locale.UK);

    private final File file;
    private final String filename;
    private final Date timestamp;
    private final long lastModified;
    private String preview;
    private Context context;
    private Uri uri;

    public Note(File file) {
        this.file = file;
        this.filename = file.getName();
        this.timestamp = parseTimestampFromFilename(filename);
        this.lastModified = file.lastModified();
        this.context = null;
        this.uri = null;
        loadPreview();
    }

    public Note(File file, String filename, long lastModified, Context context, Uri uri) {
        this.file = file;
        this.filename = filename;
        this.timestamp = parseTimestampFromFilename(filename);
        this.lastModified = lastModified;
        this.context = context;
        this.uri = uri;
        loadPreviewFromUri();
    }

    private void loadPreview() {
        String content = FileHelper.readNoteContent(file);
        if (content.length() > PREVIEW_LENGTH) {
            preview = content.substring(0, PREVIEW_LENGTH) + "…";
        } else {
            preview = content;
        }
    }

    private void loadPreviewFromUri() {
        if (context != null && uri != null) {
            String content = FileHelper.readNoteContent(context, uri);
            if (content.length() > PREVIEW_LENGTH) {
                preview = content.substring(0, PREVIEW_LENGTH) + "…";
            } else {
                preview = content;
            }
        } else {
            preview = "";
        }
    }

    private Date parseTimestampFromFilename(String filename) {
        try {
            String dateStr = filename.replace(".txt", "").replace(".md", "");
            return FILENAME_FORMAT.parse(dateStr);
        } catch (ParseException e) {
            return new Date(lastModified);
        }
    }

    public File getFile() {
        return file;
    }

    public String getFilename() {
        return filename;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getPreview() {
        return preview;
    }

    public String getFormattedDate() {
        return DISPLAY_FORMAT.format(timestamp);
    }

    public Uri getUri() {
        return uri;
    }

    public boolean hasUri() {
        return uri != null;
    }

    public long getLastModified() {
        return lastModified;
    }
}
