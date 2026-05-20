package com.example.finalproject;


import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import android.content.SharedPreferences;


public class WelcomeActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("ThemePrefs", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("isDarkMode", true);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);


        Button btnContinue = findViewById(R.id.btnContinue);
        TextView tvWelcomeTitle = findViewById(R.id.tvWelcomeTitle);
        TextView tvWelcomeSubtitle = findViewById(R.id.tvWelcomeSubtitle);
        TextView tvUserDetails = findViewById(R.id.tvUserDetails);


        int userId = getIntent().getIntExtra("user_id", -1);
        String userEmail = getIntent().getStringExtra("email");
        String userName = getIntent().getStringExtra("name");
        String userPhone = getIntent().getStringExtra("phone");
        String userBirthday = getIntent().getStringExtra("birthday");
        String userGender = getIntent().getStringExtra("gender");

        if (userName != null) {
            tvWelcomeTitle.setText("Welcome, " + userName + "!");
        }

        if (tvWelcomeSubtitle != null) {
            tvWelcomeSubtitle.setText("Login Successful");
        }

        if (userEmail != null && tvUserDetails != null) {
            tvUserDetails.setText(userEmail);
        }


        if (btnContinue != null) {
            btnContinue.setOnClickListener(v -> {
                SharedPreferences guidePrefs = getSharedPreferences("GuidePrefs", MODE_PRIVATE);
                String guideKey = "hasSeenGuide_" + (userEmail != null ? userEmail : "default");
                boolean hasSeenGuide = guidePrefs.getBoolean(guideKey, false);

                Intent intent;
                if (!hasSeenGuide) {
                    intent = new Intent(WelcomeActivity.this, GuideActivity.class);
                } else {
                    intent = new Intent(WelcomeActivity.this, MainActivity.class);
                }

                intent.putExtra("user_id", userId);
                intent.putExtra("email", userEmail);
                intent.putExtra("name", userName);
                intent.putExtra("phone", userPhone);
                intent.putExtra("birthday", userBirthday);
                intent.putExtra("gender", userGender);
                startActivity(intent);
                finish();
            });
        }
    }
}