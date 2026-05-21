package com.example.finalproject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import java.util.List;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Reschedule all active reminders
            new Thread(() -> {
                AppDatabase db = AppDatabase.getInstance(context);
                List<Note> activeNotes = db.noteDao().getAllActiveNotesAcrossUsers();
                for (Note note : activeNotes) {
                    ReminderHelper.scheduleReminder(context, note);
                }
            }).start();
        }
    }
}
