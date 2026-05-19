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


        if (userEmail != null && tvWelcomeSubtitle != null) {
            tvWelcomeSubtitle.setText("Login Successful: " + userEmail);
        }

        if (tvUserDetails != null) {
            StringBuilder details = new StringBuilder();
            if (userPhone != null) details.append("Phone: ").append(userPhone).append("\n");
            if (userBirthday != null) details.append("Birthday: ").append(userBirthday).append("\n");
            if (userGender != null) details.append("Gender: ").append(userGender);
            tvUserDetails.setText(details.toString());
        }


        if (btnContinue != null) {
            btnContinue.setOnClickListener(v -> {
                Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
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