package com.example.finalproject;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "users", indices = {
        @Index(value = {"email"}, unique = true),
        @Index(value = {"phone"}, unique = true)
})
public class User {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String fullName;
    public String email;
    public String password;
    public String phone;
    public String birthday;
    public String gender;
    public String profileImage;
    @com.google.firebase.firestore.PropertyName("isPhoneVerified")
    public boolean isPhoneVerified = false;
    @com.google.firebase.firestore.PropertyName("isMfaEnabled")
    public boolean isMfaEnabled = true;
    @com.google.firebase.firestore.PropertyName("isMfaVerified")
    public boolean isMfaVerified = false;
    public long lastMfaVerifiedAt = 0;
    
    @com.google.firebase.firestore.PropertyName("isAdminDeleted")
    public boolean isAdminDeleted = false; // New field for Admin Soft Delete
    
    @com.google.firebase.firestore.PropertyName("isUserDeleted")
    public boolean isUserDeleted = false; // User-initiated soft delete
    
    public long deletedAt = 0; // Timestamp for deletion logic (30-day window)

    public User() {} // Required for Firestore

    public User(String fullName, String email, String password, String phone, String birthday, String gender) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.birthday = birthday;
        this.gender = gender;
    }

    @com.google.firebase.firestore.PropertyName("isPhoneVerified")
    public boolean isPhoneVerified() { return isPhoneVerified; }
    @com.google.firebase.firestore.PropertyName("isPhoneVerified")
    public void setPhoneVerified(boolean verified) { isPhoneVerified = verified; }

    @com.google.firebase.firestore.PropertyName("isMfaEnabled")
    public boolean isMfaEnabled() { return isMfaEnabled; }
    @com.google.firebase.firestore.PropertyName("isMfaEnabled")
    public void setMfaEnabled(boolean enabled) { isMfaEnabled = enabled; }

    @com.google.firebase.firestore.PropertyName("isMfaVerified")
    public boolean isMfaVerified() { return isMfaVerified; }
    @com.google.firebase.firestore.PropertyName("isMfaVerified")
    public void setMfaVerified(boolean verified) { isMfaVerified = verified; }

    @com.google.firebase.firestore.PropertyName("isAdminDeleted")
    public boolean isAdminDeleted() {
        return isAdminDeleted;
    }

    @com.google.firebase.firestore.PropertyName("isAdminDeleted")
    public void setAdminDeleted(boolean adminDeleted) {
        isAdminDeleted = adminDeleted;
    }

    @com.google.firebase.firestore.PropertyName("isUserDeleted")
    public boolean isUserDeleted() {
        return isUserDeleted;
    }

    @com.google.firebase.firestore.PropertyName("isUserDeleted")
    public void setUserDeleted(boolean userDeleted) {
        isUserDeleted = userDeleted;
    }
}