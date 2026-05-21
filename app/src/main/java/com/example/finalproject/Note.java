package com.example.finalproject;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity(tableName = "notes")
public class Note {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int userId;
    private String title;
    private String content;
    private String category;
    private long dateMillis;
    private String time;
    private boolean isFavorite;
    private boolean isPinned;
    private boolean isArchived;
    private boolean isDeleted;
    private boolean isDone;
    private long archivedAt;

    // Semicolon separated paths
    private String imagePaths = ""; 
    private String filePaths = "";
    private String fileNames = "";

    public Note() {} // Required for Firestore

    public Note(int userId, String title, String content, String category, long dateMillis, String time) {
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.category = category;
        this.dateMillis = dateMillis;
        this.time = time;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public long getDateMillis() { return dateMillis; }
    public void setDateMillis(long dateMillis) { this.dateMillis = dateMillis; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    @com.google.firebase.firestore.PropertyName("isFavorite")
    public boolean isFavorite() { return isFavorite; }
    @com.google.firebase.firestore.PropertyName("isFavorite")
    public void setFavorite(boolean favorite) { isFavorite = favorite; }

    @com.google.firebase.firestore.PropertyName("isPinned")
    public boolean isPinned() { return isPinned; }
    @com.google.firebase.firestore.PropertyName("isPinned")
    public void setPinned(boolean pinned) { isPinned = pinned; }

    @com.google.firebase.firestore.PropertyName("isArchived")
    public boolean isArchived() { return isArchived; }
    @com.google.firebase.firestore.PropertyName("isArchived")
    public void setArchived(boolean archived) { isArchived = archived; }

    @com.google.firebase.firestore.PropertyName("isDeleted")
    public boolean isDeleted() { return isDeleted; }
    @com.google.firebase.firestore.PropertyName("isDeleted")
    public void setDeleted(boolean deleted) { isDeleted = deleted; }

    @com.google.firebase.firestore.PropertyName("isDone")
    public boolean isDone() { return isDone; }
    @com.google.firebase.firestore.PropertyName("isDone")
    public void setDone(boolean done) { isDone = done; }

    public long getArchivedAt() { return archivedAt; }
    public void setArchivedAt(long archivedAt) { this.archivedAt = archivedAt; }

    // Multi-attachment helpers
    public String getImagePaths() { return imagePaths; }
    public void setImagePaths(String imagePaths) { this.imagePaths = imagePaths; }

    public String getFilePaths() { return filePaths; }
    public void setFilePaths(String filePaths) { this.filePaths = filePaths; }

    public String getFileNames() { return fileNames; }
    public void setFileNames(String fileNames) { this.fileNames = fileNames; }

    // List logic for UI
    public List<String> getImagePathsList() {
        if (imagePaths == null || imagePaths.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(imagePaths.split("\\|")));
    }

    public void setImagePathsList(List<String> list) {
        if (list == null || list.isEmpty()) this.imagePaths = "";
        else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                sb.append(list.get(i));
                if (i < list.size() - 1) sb.append("|");
            }
            this.imagePaths = sb.toString();
        }
    }

    public List<String> getFilePathsList() {
        if (filePaths == null || filePaths.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(filePaths.split("\\|")));
    }

    public void setFilePathsList(List<String> list) {
        if (list == null || list.isEmpty()) this.filePaths = "";
        else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                sb.append(list.get(i));
                if (i < list.size() - 1) sb.append("|");
            }
            this.filePaths = sb.toString();
        }
    }

    public List<String> getFileNamesList() {
        if (fileNames == null || fileNames.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(fileNames.split("\\|")));
    }

    public void setFileNamesList(List<String> list) {
        if (list == null || list.isEmpty()) this.fileNames = "";
        else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                sb.append(list.get(i));
                if (i < list.size() - 1) sb.append("|");
            }
            this.fileNames = sb.toString();
        }
    }
}
