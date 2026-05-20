package com.example.finalproject;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before super.onCreate
        SharedPreferences prefs = getSharedPreferences("ThemePrefs", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("isDarkMode", true);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Delay for 2 seconds then transition
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            SharedPreferences loginPrefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
            boolean isLoggedIn = loginPrefs.getBoolean("isLoggedIn", false);

            if (isLoggedIn) {
                // Auto-login: skip login screen and go to Welcome or MainActivity
                Intent intent = new Intent(SplashActivity.this, WelcomeActivity.class);
                intent.putExtra("user_id", loginPrefs.getInt("user_id", -1));
                intent.putExtra("email", loginPrefs.getString("email", ""));
                intent.putExtra("name", loginPrefs.getString("name", ""));
                intent.putExtra("phone", loginPrefs.getString("phone", ""));
                intent.putExtra("birthday", loginPrefs.getString("birthday", ""));
                intent.putExtra("gender", loginPrefs.getString("gender", ""));
                startActivity(intent);
            } else {
                startActivity(new Intent(SplashActivity.this, LoginAndSignup.class));
            }
            finish();
        }, 2000);
    }
}