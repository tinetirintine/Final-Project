package com.example.finalproject;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import java.util.Calendar;

public class ReminderHelper {

    public static void scheduleReminder(Context context, Note note) {
        // Only notify if an actual time is set
        if (note.getTime() == null || note.getTime().equals("No Time")) {
            cancelReminder(context, note.getId());
            return;
        }

        // Only notify if not done and not deleted
        if (note.isDone() || note.isDeleted()) {
            cancelReminder(context, note.getId());
            return;
        }

        long triggerTime = calculateMillis(note.getDateMillis(), note.getTime());
        
        if (triggerTime <= System.currentTimeMillis()) {
            // Already past due.
            triggerNotification(context, note);
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        // Check for exact alarm permission on Android 12+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // If we can't schedule exact, fallback to inexact or just notify now if it's very close
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, getPendingIntent(context, note));
                return;
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, getPendingIntent(context, note));
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, getPendingIntent(context, note));
        }
    }

    public static void cancelReminder(Context context, int noteId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(getPendingIntentById(context, noteId));
        }
    }

    private static PendingIntent getPendingIntent(Context context, Note note) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("title", note.getTitle());
        intent.putExtra("content", "Task due: " + note.getTitle());
        intent.putExtra("noteId", note.getId());

        return PendingIntent.getBroadcast(
                context,
                note.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static PendingIntent getPendingIntentById(Context context, int noteId) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        return PendingIntent.getBroadcast(
                context,
                noteId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static void triggerNotification(Context context, Note note) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("title", note.getTitle());
        intent.putExtra("content", "This task is past due!");
        intent.putExtra("noteId", note.getId());
        context.sendBroadcast(intent);
    }

    private static long calculateMillis(long dateMillis, String time) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(dateMillis);
        
        try {
            String[] parts = time.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, minute);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
        } catch (Exception e) {
            // Fallback to end of day if time format is weird
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
        }
        
        return cal.getTimeInMillis();
    }
}
