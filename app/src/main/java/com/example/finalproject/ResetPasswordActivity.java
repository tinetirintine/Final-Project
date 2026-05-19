package com.example.finalproject;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import android.content.SharedPreferences;
import java.util.Objects;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextNewPassword, editTextConfirmPassword;
    private TextView tvPasswordStrength;
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
        setContentView(R.layout.activity_reset_password);

        userRepository = new UserRepository(this);

        editTextEmail = findViewById(R.id.editTextResetEmail);
        editTextNewPassword = findViewById(R.id.editTextNewPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmResetPassword);
        tvPasswordStrength = findViewById(R.id.tvResetPasswordStrength);
        Button btnReset = findViewById(R.id.btnResetPassword);
        TextView tvBackToLogin = findViewById(R.id.tvBackToLoginFromReset);

        editTextNewPassword.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePasswordStrength(s.toString());
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        btnReset.setOnClickListener(v -> {
            String email = editTextEmail.getText().toString().trim();
            String newPassword = editTextNewPassword.getText().toString();
            String confirm = editTextConfirmPassword.getText().toString();

            if (email.isEmpty() || newPassword.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            } else if (!isStrongPassword(newPassword)) {
                Toast.makeText(this, "Password is too weak", Toast.LENGTH_SHORT).show();
            } else if (!Objects.equals(newPassword, confirm)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            } else {
                userRepository.getUserByEmail(email, user -> {
                    if (user != null) {
                        String hashed = SecurityUtils.hashPassword(newPassword);
                        userRepository.updatePassword(email, hashed, () -> runOnUiThread(() -> {
                            Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show();
                            finish();
                        }));
                    } else {
                        runOnUiThread(() -> Toast.makeText(this, "Email not found", Toast.LENGTH_SHORT).show());
                    }
                });
            }
        });

        tvBackToLogin.setOnClickListener(v -> finish());
    }

    private void updatePasswordStrength(String password) {
        if (password.isEmpty()) {
            tvPasswordStrength.setText("Password Strength: N/A");
            tvPasswordStrength.setTextColor(android.graphics.Color.GRAY);
            return;
        }
        if (isStrongPassword(password)) {
            tvPasswordStrength.setText("Password Strength: Strong");
            tvPasswordStrength.setTextColor(android.graphics.Color.GREEN);
        } else {
            tvPasswordStrength.setText("Password Strength: Weak");
            tvPasswordStrength.setTextColor(android.graphics.Color.RED);
        }
    }

    private boolean isStrongPassword(String password) {
        if (password.length() < 8) return false;
        boolean hasUpper = false, hasLower = false, hasDigit = false, hasSpecial = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if (!Character.isLetterOrDigit(c)) hasSpecial = true;
        }
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
}
