package com.example.finalproject;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "users", indices = {@Index(value = {"email"}, unique = true)})
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

    public User(String fullName, String email, String password, String phone, String birthday, String gender) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.birthday = birthday;
        this.gender = gender;
    }
}