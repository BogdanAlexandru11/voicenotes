package com.alex.voicenotes;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LogHelper {
    private static final int MAX_ENTRIES = 500;
    private static final List<String> logs = new ArrayList<>();
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);

    public static synchronized void d(String tag, String message) {
        Log.d(tag, message);
        addEntry("D", tag, message);
    }

    public static synchronized void i(String tag, String message) {
        Log.i(tag, message);
        addEntry("I", tag, message);
    }

    public static synchronized void w(String tag, String message) {
        Log.w(tag, message);
        addEntry("W", tag, message);
    }

    public static synchronized void e(String tag, String message) {
        Log.e(tag, message);
        addEntry("E", tag, message);
    }

    public static synchronized void e(String tag, String message, Throwable t) {
        Log.e(tag, message, t);
        addEntry("E", tag, message + ": " + t.getMessage());
    }

    private static void addEntry(String level, String tag, String message) {
        String timestamp = sdf.format(new Date());
        String entry = timestamp + " " + level + "/" + tag + ": " + message;
        logs.add(entry);
        while (logs.size() > MAX_ENTRIES) {
            logs.remove(0);
        }
    }

    public static synchronized List<String> getLogs() {
        return new ArrayList<>(logs);
    }

    public static synchronized String getLogsAsString() {
        StringBuilder sb = new StringBuilder();
        for (String log : logs) {
            sb.append(log).append("\n");
        }
        return sb.toString();
    }

    public static synchronized void clear() {
        logs.clear();
    }
}
