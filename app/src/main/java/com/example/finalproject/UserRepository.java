package com.example.finalproject;

import android.content.Context;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserRepository {
    private UserDao userDao;
    private ExecutorService executorService;

    public UserRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        userDao = db.userDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    public void register(User user, Callback<Boolean> callback) {
        executorService.execute(() -> {
            User existing = userDao.getUserByEmail(user.email);
            if (existing == null) {
                userDao.registerUser(user);
                callback.onResult(true);
            } else {
                callback.onResult(false);
            }
        });
    }

    public void updateUser(User user, Runnable onComplete) {
        executorService.execute(() -> {
            userDao.updateUser(user);
            if (onComplete != null) onComplete.run();
        });
    }

    public void getUserByEmail(String email, Callback<User> callback) {
        executorService.execute(() -> {
            User user = userDao.getUserByEmail(email);
            callback.onResult(user);
        });
    }

    public void login(String email, String password, Callback<User> callback) {
        executorService.execute(() -> {
            // Check if it's the admin account and if it exists
            if (email.equals("adminpogi") && password.equals(SecurityUtils.hashPassword("sayolangako"))) {
                User admin = userDao.getUserByEmail("adminpogi");
                if (admin == null) {
                    // Auto-create admin if it doesn't exist
                    admin = new User("Admin Pogi", "adminpogi", password, "000", "01/01/2000", "Male");
                    userDao.registerUser(admin);
                    admin = userDao.getUserByEmail("adminpogi"); // Get the ID
                }
                callback.onResult(admin);
                return;
            }

            User user = userDao.loginUser(email, password);
            callback.onResult(user);
        });
    }

    public interface Callback<T> {
        void onResult(T result);
    }
}