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

    public void login(String email, String password, Callback<User> callback) {
        executorService.execute(() -> {
            User user = userDao.loginUser(email, password);
            callback.onResult(user);
        });
    }

    public interface Callback<T> {
        void onResult(T result);
    }
}