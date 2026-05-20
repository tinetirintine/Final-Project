package com.example.finalproject;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface NoteDao {
    @Insert
    void insertNote(Note note);

    @Update
    void updateNote(Note note);

    @Delete
    void deleteNote(Note note);

    @Query("SELECT * FROM notes WHERE userId = :userId AND isDeleted = 0 AND isArchived = 0 ORDER BY isPinned DESC, dateMillis DESC")
    List<Note> getActiveNotesForUser(int userId);

    @Query("SELECT * FROM notes WHERE userId = :userId AND isDeleted = 0")
    List<Note> getAllNotesForUser(int userId);

    @Query("SELECT * FROM notes WHERE userId = :userId AND isDone = 1 AND isDeleted = 0 AND isArchived = 0 ORDER BY isPinned DESC, dateMillis DESC")
    List<Note> getDoneNotesForUser(int userId);

    @Query("SELECT * FROM notes WHERE userId = :userId AND isDone = 0 AND isDeleted = 0 AND isArchived = 0 ORDER BY isPinned DESC, dateMillis DESC")
    List<Note> getUndoneNotesForUser(int userId);

    @Query("SELECT * FROM notes WHERE userId = :userId AND isFavorite = 1 AND isDeleted = 0 AND isArchived = 0 ORDER BY isPinned DESC, dateMillis DESC")
    List<Note> getFavoriteNotesForUser(int userId);

    @Query("SELECT * FROM notes WHERE userId = :userId AND isArchived = 1 AND isDeleted = 0 ORDER BY isPinned DESC, archivedAt DESC")
    List<Note> getArchivedNotesForUser(int userId);

    @Query("SELECT * FROM notes WHERE userId = :userId AND isDeleted = 1")
    List<Note> getDeletedNotesForUser(int userId);

    @Query("SELECT * FROM notes WHERE userId = :userId AND category = :category AND isDeleted = 0 AND isArchived = 0 ORDER BY isPinned DESC, dateMillis DESC")
    List<Note> getNotesByCategory(int userId, String category);

    @Query("DELETE FROM notes WHERE userId = :userId AND isDeleted = 1")
    void emptyTrash(int userId);
}