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
                        if (SecurityUtils.hashPassword(newPassword).equals(user.password)) {
                            runOnUiThread(() -> Toast.makeText(this, "You cannot use your current password", Toast.LENGTH_SHORT).show());
                        } else {
                            runOnUiThread(() -> showResetVerificationDialog(email, newPassword));
                        }
                    } else {
                        runOnUiThread(() -> Toast.makeText(this, "Email not found", Toast.LENGTH_SHORT).show());
                    }
                });
            }
        });

        tvBackToLogin.setOnClickListener(v -> finish());
    }

    private void showResetVerificationDialog(String email, String newPassword) {
        String code = String.valueOf(100000 + new java.util.Random().nextInt(900000));
        
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("Sending verification code...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        EmailHelper.sendVerificationCode(email, code, (success, error) -> runOnUiThread(() -> {
            progressDialog.dismiss();
            
            if (success) {
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                builder.setTitle("Verify Identity");
                builder.setMessage("A verification code was sent to " + email);

                final EditText input = new EditText(this);
                input.setHint("000000");
                input.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                
                android.widget.LinearLayout container = new android.widget.LinearLayout(this);
                container.setOrientation(android.widget.LinearLayout.VERTICAL);
                android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(64, 32, 64, 32);
                input.setLayoutParams(lp);
                container.addView(input);
                builder.setView(container);

                builder.setPositiveButton("Verify & Reset", (dialog, which) -> {
                    if (input.getText().toString().equals(code)) {
                        String hashed = SecurityUtils.hashPassword(newPassword);
                        userRepository.updatePassword(email, hashed, () -> runOnUiThread(() -> {
                            Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show();
                            finish();
                        }));
                    } else {
                        Toast.makeText(this, "Invalid code", Toast.LENGTH_SHORT).show();
                    }
                });
                builder.setNegativeButton("Cancel", null);
                builder.show();
            } else {
                String msg = "Failed to send code.";
                if (error != null) msg += "\nError: " + error;
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            }
        }));
    }

    private void updatePasswordStrength(String password) {
        if (password.isEmpty()) {
            tvPasswordStrength.setText("Password Strength: N/A");
            tvPasswordStrength.setTextColor(android.graphics.Color.GRAY);
            return;
        }

        int score = calculatePasswordStrength(password);
        
        if (score <= 1) {
            tvPasswordStrength.setText("Password Strength: Very Weak");
            tvPasswordStrength.setTextColor(android.graphics.Color.parseColor("#f44336")); // Red
        } else if (score == 2) {
            tvPasswordStrength.setText("Password Strength: Weak");
            tvPasswordStrength.setTextColor(android.graphics.Color.parseColor("#ff9800")); // Orange
        } else if (score == 3) {
            tvPasswordStrength.setText("Password Strength: Medium");
            tvPasswordStrength.setTextColor(android.graphics.Color.parseColor("#ffc107")); // Amber
        } else if (score == 4) {
            tvPasswordStrength.setText("Password Strength: Strong");
            tvPasswordStrength.setTextColor(android.graphics.Color.parseColor("#4caf50")); // Green
        } else {
            tvPasswordStrength.setText("Password Strength: Very Strong");
            tvPasswordStrength.setTextColor(android.graphics.Color.parseColor("#2e7d32")); // Dark Green
        }
    }

    private int calculatePasswordStrength(String password) {
        if (password.length() < 6) return 1;

        int components = 0;
        if (password.matches(".*[A-Z].*")) components++;
        if (password.matches(".*[a-z].*")) components++;
        if (password.matches(".*[0-9].*")) components++;
        if (password.matches(".*[^A-Za-z0-9].*")) components++;

        if (password.length() >= 10 && components >= 4) return 5;
        if (password.length() >= 8 && components >= 3) return 4;
        if (password.length() >= 8 || components >= 3) return 3;
        if (components >= 2) return 2;
        return 1;
    }

    private boolean isStrongPassword(String password) {
        return calculatePasswordStrength(password) >= 4;
    }
}
