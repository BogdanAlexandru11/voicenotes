package com.alex.voicenotes;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

public class VoiceNotesWidget extends AppWidgetProvider {

    public static final String ACTION_TOGGLE_RECORDING = "com.alex.voicenotes.TOGGLE_RECORDING";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, VoiceRecordingService.isRecording());
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (ACTION_TOGGLE_RECORDING.equals(intent.getAction())) {
            Intent serviceIntent = new Intent(context, VoiceRecordingService.class);
            if (VoiceRecordingService.isRecording()) {
                serviceIntent.setAction(VoiceRecordingService.ACTION_STOP_RECORDING);
            } else {
                serviceIntent.setAction(VoiceRecordingService.ACTION_START_RECORDING);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        }
    }

    private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                        int appWidgetId, boolean isRecording) {
        RemoteViews views = getRemoteViews(context, isRecording);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static RemoteViews getRemoteViews(Context context, boolean isRecording) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

        if (isRecording) {
            views.setInt(R.id.widgetContainer, "setBackgroundResource", R.drawable.widget_background_recording);
            views.setImageViewResource(R.id.widgetIcon, R.drawable.ic_stop);
        } else {
            views.setInt(R.id.widgetContainer, "setBackgroundResource", R.drawable.widget_background);
            views.setImageViewResource(R.id.widgetIcon, R.drawable.ic_mic);
        }

        views.setOnClickPendingIntent(R.id.widgetContainer, getTogglePendingIntent(context));

        return views;
    }

    private static PendingIntent getTogglePendingIntent(Context context) {
        Intent intent = new Intent(context, VoiceNotesWidget.class);
        intent.setAction(ACTION_TOGGLE_RECORDING);
        return PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void updateAllWidgets(Context context, boolean isRecording) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName widgetComponent = new ComponentName(context, VoiceNotesWidget.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(widgetComponent);

        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, isRecording);
        }
    }
}
