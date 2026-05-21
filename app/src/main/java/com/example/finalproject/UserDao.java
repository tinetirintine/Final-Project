package com.example.finalproject;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface UserDao {
    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    void registerUser(User user);

    @Update
    void updateUser(User user);

    @Query("SELECT * FROM users WHERE email = :email AND password = :password LIMIT 1")
    User loginUser(String email, String password);

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User getUserByEmail(String email);

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    User getUserById(int id);

    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    User getUserByPhone(String phone);

    @Query("UPDATE users SET password = :newHashedPassword WHERE email = :email")
    void updatePassword(String email, String newHashedPassword);

    @Query("SELECT * FROM users WHERE email != 'adminpogi'")
    java.util.List<User> getAllUsers();

    @Query("DELETE FROM users WHERE email = :email")
    void deleteUserByEmail(String email);

    @Query("DELETE FROM users")
    void nukeUsers();

    @Query("DELETE FROM users WHERE email != 'adminpogi'")
    void nukeUsersExceptAdmin();

    @Query("DELETE FROM users WHERE isUserDeleted = 1 AND deletedAt < :threshold")
    void permanentlyDeleteOldAccounts(long threshold);
}