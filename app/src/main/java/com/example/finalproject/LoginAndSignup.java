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
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import java.util.Random;


public class LoginAndSignup extends AppCompatActivity {


    private EditText editTextEmail, editTextPassword;
    private UserRepository userRepository;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    
    // Trust device for 30 days
    private static final long TRUST_EXPIRATION_MILLIS = 30L * 24 * 60 * 60 * 1000;


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

        // Check if biometric is enabled for the last user
        SharedPreferences bioPrefs = getSharedPreferences("BioPrefs", MODE_PRIVATE);
        String lastEmail = bioPrefs.getString("last_email", "");
        boolean isBioEnabled = bioPrefs.getBoolean("bio_enabled_" + lastEmail, false);

        if (isBioEnabled) {
            btnBiometricLogin.setVisibility(View.VISIBLE);
        } else {
            btnBiometricLogin.setVisibility(View.GONE);
        }

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
                        } else if (!isOnline()) {
                            // Offline check: Only allow if MFA was previously verified and NOT EXPIRED
                            boolean isTrustValid = user.isMfaVerified && 
                                (System.currentTimeMillis() - user.lastMfaVerifiedAt < TRUST_EXPIRATION_MILLIS);

                            if (isTrustValid) {
                                Toast.makeText(this, "Offline Mode: Verified device", Toast.LENGTH_SHORT).show();
                                proceedToWelcome(user);
                            } else {
                                String msg = user.isMfaVerified ? "Security trust expired. Please go online to verify." : "First-time login requires internet.";
                                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                            }
                        } else {
                            // Online: Check if trust is still valid to potentially skip MFA
                            boolean isTrustValid = user.isMfaVerified && 
                                (System.currentTimeMillis() - user.lastMfaVerifiedAt < TRUST_EXPIRATION_MILLIS);
                                
                            if (isTrustValid) {
                                proceedToWelcome(user);
                            } else {
                                showMFADialog(user);
                            }
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

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }
        return false;
    }

    private void proceedToWelcome(User user) {
        // Save session for Auto-Login
        SharedPreferences loginPrefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        loginPrefs.edit()
                .putInt("user_id", user.id)
                .putString("email", user.email)
                .putString("name", user.fullName)
                .putString("phone", user.phone)
                .putString("birthday", user.birthday)
                .putString("gender", user.gender)
                .putBoolean("isLoggedIn", true)
                .apply();

        // Save last email for biometric check
        SharedPreferences bioPrefs = getSharedPreferences("BioPrefs", MODE_PRIVATE);
        bioPrefs.edit().putString("last_email", user.email).apply();

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
                
                SharedPreferences bioPrefs = getSharedPreferences("BioPrefs", MODE_PRIVATE);
                String lastEmail = bioPrefs.getString("last_email", "");
                
                if (!lastEmail.isEmpty()) {
                    userRepository.getUserByEmail(lastEmail, user -> {
                        if (user != null) {
                            runOnUiThread(() -> {
                                Toast.makeText(getApplicationContext(), "Biometric login successful!", Toast.LENGTH_SHORT).show();
                                proceedToWelcome(user);
                            });
                        }
                    });
                } else {
                    Toast.makeText(getApplicationContext(), "No user linked to biometrics", Toast.LENGTH_SHORT).show();
                }
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
        
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("Sending security code...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        EmailHelper.sendVerificationCode(user.email, code, (success, error) -> runOnUiThread(() -> {
            progressDialog.dismiss();
            
            if (success) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Verify Your Identity");
                builder.setMessage("We've sent a 6-digit verification code to " + user.email);

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
                        // Mark device as trusted and update timestamp
                        user.isMfaVerified = true;
                        user.lastMfaVerifiedAt = System.currentTimeMillis();
                        userRepository.updateUser(user, () -> runOnUiThread(() -> proceedToWelcome(user)));
                    } else {
                        Toast.makeText(this, "Incorrect verification code", Toast.LENGTH_SHORT).show();
                    }
                });
                builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
                
                AlertDialog dialog = builder.create();
                dialog.show();
                
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.brand_purple));
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            } else {
                String msg = "Failed to send security code.";
                if (error != null) msg += "\nError: " + error;
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            }
        }));
    }
}
