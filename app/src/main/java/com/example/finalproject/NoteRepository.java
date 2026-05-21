package com.example.finalproject;

import android.content.Context;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NoteRepository {
    private final Context context;
    private final NoteDao noteDao;
    private final UserDao userDao;
    private final ExecutorService executorService;
    private final FirestoreHelper firestoreHelper;

    public NoteRepository(Context context) {
        this.context = context.getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(context);
        noteDao = db.noteDao();
        userDao = db.userDao();
        executorService = Executors.newSingleThreadExecutor();
        firestoreHelper = new FirestoreHelper();
    }

    public void addNote(Note note, Runnable onComplete) {
        executorService.execute(() -> {
            long newId = noteDao.insertNote(note);
            note.setId((int) newId); // Set the generated ID for Firestore
            
            // Schedule notification
            ReminderHelper.scheduleReminder(context, note);
            
            User user = userDao.getUserById(note.getUserId());
            if (user != null) {
                firestoreHelper.uploadNote(note, user.email);
            }
            if (onComplete != null) onComplete.run();
        });
    }

    public void updateNote(Note note, Runnable onComplete) {
        executorService.execute(() -> {
            noteDao.updateNote(note);
            
            // Schedule/Reschedule notification
            ReminderHelper.scheduleReminder(context, note);
            
            User user = userDao.getUserById(note.getUserId());
            if (user != null) {
                firestoreHelper.uploadNote(note, user.email);
            }
            if (onComplete != null) onComplete.run();
        });
    }

    public void deleteNote(Note note, Runnable onComplete) {
        executorService.execute(() -> {
            noteDao.deleteNote(note);
            
            // Cancel notification
            ReminderHelper.cancelReminder(context, note.getId());

            User user = userDao.getUserById(note.getUserId());
            if (user != null) {
                firestoreHelper.deleteNote(note, user.email);
            }
            if (onComplete != null) onComplete.run();
        });
    }

    public void getActiveNotes(int userId, final Callback<List<Note>> callback) {
        executorService.execute(() -> {
            List<Note> result = noteDao.getActiveNotesForUser(userId);
            callback.onResult(result);
        });
    }

    public void getFavoriteNotes(int userId, final Callback<List<Note>> callback) {
        executorService.execute(() -> {
            List<Note> result = noteDao.getFavoriteNotesForUser(userId);
            callback.onResult(result);
        });
    }

    public void getArchivedNotes(int userId, final Callback<List<Note>> callback) {
        executorService.execute(() -> {
            List<Note> result = noteDao.getArchivedNotesForUser(userId);
            callback.onResult(result);
        });
    }

    public void getDeletedNotes(int userId, final Callback<List<Note>> callback) {
        executorService.execute(() -> {
            List<Note> result = noteDao.getDeletedNotesForUser(userId);
            callback.onResult(result);
        });
    }

    public void getDoneNotes(int userId, final Callback<List<Note>> callback) {
        executorService.execute(() -> {
            List<Note> result = noteDao.getDoneNotesForUser(userId);
            callback.onResult(result);
        });
    }

    public void getUndoneNotes(int userId, final Callback<List<Note>> callback) {
        executorService.execute(() -> {
            List<Note> result = noteDao.getUndoneNotesForUser(userId);
            callback.onResult(result);
        });
    }

    public void getNotesByCategory(int userId, String category, final Callback<List<Note>> callback) {
        executorService.execute(() -> {
            List<Note> result;
            if (Objects.equals(category, "All")) {
                result = noteDao.getActiveNotesForUser(userId);
            } else {
                result = noteDao.getNotesByCategory(userId, category);
            }
            callback.onResult(result);
        });
    }

    public void getNotesByDate(int userId, long dateMillis, final Callback<List<Note>> callback) {
        executorService.execute(() -> {
            List<Note> allNotes = noteDao.getAllNotesForUser(userId);
            List<Note> filtered = new ArrayList<>();
            Calendar cal1 = Calendar.getInstance();
            cal1.setTimeInMillis(dateMillis);
            Calendar cal2 = Calendar.getInstance();
            for (Note note : allNotes) {
                cal2.setTimeInMillis(note.getDateMillis());
                if (cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                        cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)) {
                    filtered.add(note);
                }
            }
            callback.onResult(filtered);
        });
    }

    public void emptyTrash(int userId, Runnable onComplete) {
        executorService.execute(() -> {
            User user = userDao.getUserById(userId);
            List<Note> deletedNotes = noteDao.getDeletedNotesForUser(userId);
            
            for (Note note : deletedNotes) {
                // Cancel notification
                ReminderHelper.cancelReminder(context, note.getId());
                
                // Delete from Cloud
                if (user != null) {
                    firestoreHelper.deleteNote(note, user.email);
                }
            }
            
            // Delete from Local
            noteDao.emptyTrash(userId);

            if (onComplete != null) onComplete.run();
        });
    }

    public void getNoteById(int noteId, Callback<Note> callback) {
        executorService.execute(() -> {
            Note note = noteDao.getNoteById(noteId);
            callback.onResult(note);
        });
    }

    public void syncNotesFromCloud(int userId, Runnable onComplete) {
        executorService.execute(() -> {
            User user = userDao.getUserById(userId);
            if (user != null) {
                firestoreHelper.getAllNotes(user.email, queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        executorService.execute(() -> {
                            for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                                Note cloudNote = doc.toObject(Note.class);
                                if (cloudNote != null) {
                                    noteDao.insertNote(cloudNote); // insertNote with @Insert(onConflict = OnConflictStrategy.REPLACE)
                                    ReminderHelper.scheduleReminder(context, cloudNote);
                                }
                            }
                            if (onComplete != null) onComplete.run();
                        });
                    } else {
                        if (onComplete != null) onComplete.run();
                    }
                });
            } else {
                if (onComplete != null) onComplete.run();
            }
        });
    }

    public interface Callback<T> {
        void onResult(T result);
    }
}