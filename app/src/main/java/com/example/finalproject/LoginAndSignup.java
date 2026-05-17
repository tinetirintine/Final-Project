package com.example.finalproject;


import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.SharedPreferences;


public class LoginAndSignup extends AppCompatActivity {


    private EditText editTextEmail, editTextPassword;
    private UserRepository userRepository;


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
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);


        userRepository = new UserRepository(this);


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        editTextEmail    = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        Button btnLogin  = findViewById(R.id.btnLogin);
        TextView btnCreateAccount = findViewById(R.id.btnCreateAccount);


        btnLogin.setOnClickListener(v -> {
            String email = editTextEmail.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();


            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            } else {
                String hashed = SecurityUtils.hashPassword(password);
                userRepository.login(email, hashed, user -> runOnUiThread(() -> {
                    if (user != null) {
                        proceedToWelcome(user);
                    } else {
                        Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                    }
                }));
            }
        });


        btnCreateAccount.setOnClickListener(v -> startActivity(new Intent(this, CreateAccount.class)));
    }

    private void proceedToWelcome(User user) {
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.putExtra("user_id", user.id);
        intent.putExtra("email", user.email);
        intent.putExtra("name", user.fullName);
        intent.putExtra("phone", user.phone);
        intent.putExtra("birthday", user.birthday);
        intent.putExtra("gender", user.gender);
        startActivity(intent);
        finish();
    }
}
