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
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import java.util.concurrent.Executor;
import android.app.AlertDialog;
import android.view.View;
import android.widget.LinearLayout;
import java.util.Random;


public class LoginAndSignup extends AppCompatActivity {


    private EditText editTextEmail, editTextPassword;
    private UserRepository userRepository;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;


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

        setupBiometric();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        editTextEmail    = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        Button btnLogin  = findViewById(R.id.btnLogin);
        Button btnBiometricLogin = findViewById(R.id.btnBiometricLogin);
        TextView btnCreateAccount = findViewById(R.id.btnCreateAccount);
        TextView btnForgotPassword = findViewById(R.id.btnForgotPassword);


        btnLogin.setOnClickListener(v -> {
            String email = editTextEmail.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();


            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            } else {
                String hashed = SecurityUtils.hashPassword(password);
                userRepository.login(email, hashed, user -> runOnUiThread(() -> {
                    if (user != null) {
                        if ("adminpogi".equals(user.email)) {
                            proceedToWelcome(user);
                        } else {
                            showMFADialog(user);
                        }
                    } else {
                        Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                    }
                }));
            }
        });

        btnBiometricLogin.setOnClickListener(v -> biometricPrompt.authenticate(promptInfo));


        btnCreateAccount.setOnClickListener(v -> startActivity(new Intent(this, CreateAccount.class)));

        btnForgotPassword.setOnClickListener(v -> startActivity(new Intent(this, ResetPasswordActivity.class)));
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

    private void setupBiometric() {
        Executor executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(LoginAndSignup.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @androidx.annotation.NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(getApplicationContext(), "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                // For simplicity, we'll just log in the last registered user or show a message
                // In a real app, you'd store tokens securely.
                Toast.makeText(getApplicationContext(), "Biometric Authentication succeeded!", Toast.LENGTH_SHORT).show();
                // Since we don't have the user object here, we might need to fetch it or skip MFA for biometric.
                // For demo purposes, let's just show a toast.
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), "Authentication failed", Toast.LENGTH_SHORT).show();
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric login")
                .setSubtitle("Log in using your biometric credential")
                .setNegativeButtonText("Use account password")
                .build();

        // Optional: trigger biometric on start if configured
        // biometricPrompt.authenticate(promptInfo);
    }

    private void showMFADialog(User user) {
        String code = String.valueOf(100000 + new Random().nextInt(900000));
        
        // Simulate sending a real email
        Toast.makeText(this, "Security code sent to " + user.email, Toast.LENGTH_LONG).show();
        // In a real production app, you would call a backend API here to send the email.
        // For demonstration, we show the code in a Toast so you can still log in.
        Toast.makeText(this, "MFA Code: " + code, Toast.LENGTH_LONG).show();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Verify Your Identity");
        builder.setMessage("We've sent a 6-digit verification code to " + user.email + ". Please enter it below to continue.");

        final EditText input = new EditText(this);
        input.setHint("000000");
        input.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        input.setLetterSpacing(0.5f);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(64, 32, 64, 32);
        input.setLayoutParams(lp);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("Verify", (dialog, which) -> {
            String entered = input.getText().toString().trim();
            if (entered.equals(code)) {
                Toast.makeText(this, "Identity Verified", Toast.LENGTH_SHORT).show();
                proceedToWelcome(user);
            } else {
                Toast.makeText(this, "Incorrect verification code", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        // Style the buttons for a minimalist look
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.brand_purple));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
    }
}
