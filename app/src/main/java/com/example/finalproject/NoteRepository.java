package com.example.finalproject;

import android.content.Context;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NoteRepository {
    private final NoteDao noteDao;
    private final ExecutorService executorService;

    public NoteRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        noteDao = db.noteDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    public void addNote(Note note, Runnable onComplete) {
        executorService.execute(() -> {
            noteDao.insertNote(note);
            if (onComplete != null) onComplete.run();
        });
    }

    public void updateNote(Note note, Runnable onComplete) {
        executorService.execute(() -> {
            noteDao.updateNote(note);
            if (onComplete != null) onComplete.run();
        });
    }

    public void deleteNote(Note note, Runnable onComplete) {
        executorService.execute(() -> {
            noteDao.deleteNote(note);
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
            noteDao.emptyTrash(userId);
            if (onComplete != null) onComplete.run();
        });
    }

    public interface Callback<T> {
        void onResult(T result);
    }
}