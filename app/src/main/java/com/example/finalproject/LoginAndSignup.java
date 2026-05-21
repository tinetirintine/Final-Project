package com.example.finalproject;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import android.content.SharedPreferences;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import android.text.InputFilter;
import android.view.WindowManager;
import java.util.concurrent.Executor;
import android.app.AlertDialog;
import android.view.View;
import android.widget.LinearLayout;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import java.util.Random;

public class LoginAndSignup extends AppCompatActivity {

    private TextInputEditText editTextEmail, editTextPassword;
    private UserRepository userRepository;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    
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
        setContentView(R.layout.activity_login);

        userRepository = new UserRepository(this);
        setupBiometric();

        editTextEmail    = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        Button btnLogin  = findViewById(R.id.btnLogin);
        Button btnBiometricLogin = findViewById(R.id.btnBiometricLogin);
        TextView btnCreateAccount = findViewById(R.id.btnCreateAccount);
        TextView btnForgotPassword = findViewById(R.id.btnForgotPassword);

        SharedPreferences bioPrefs = getSharedPreferences("BioPrefs", MODE_PRIVATE);
        String lastEmail = bioPrefs.getString("last_email", "");
        boolean isBioEnabled = bioPrefs.getBoolean("bio_enabled_" + lastEmail, false);

        btnBiometricLogin.setVisibility(isBioEnabled ? View.VISIBLE : View.GONE);

        btnLogin.setOnClickListener(v -> {
            String email = editTextEmail.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            String hashed = SecurityUtils.hashPassword(password);
            userRepository.login(email, hashed, user -> runOnUiThread(() -> {
                if (user != null) {
                    if (user.isAdminDeleted) {
                        Toast.makeText(this, "Account Disabled.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    if ("adminpogi".equals(user.email)) {
                        proceedToWelcome(user);
                        return;
                    }

                    if (user.isUserDeleted) {
                        showRestoreAccountDialog(user);
                        return;
                    }

                    handleNormalLogin(user);
                } else {
                    userRepository.getUserByEmail(email, foundUser -> runOnUiThread(() -> {
                        if (foundUser == null || foundUser.isUserDeleted) {
                            Toast.makeText(this, "No account exists for this email", Toast.LENGTH_SHORT).show();
                        } else if (foundUser.isAdminDeleted) {
                            Toast.makeText(this, "Account Disabled.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show();
                        }
                    }));
                }
            }));
        });

        btnBiometricLogin.setOnClickListener(v -> biometricPrompt.authenticate(promptInfo));
        btnCreateAccount.setOnClickListener(v -> startActivity(new Intent(this, CreateAccount.class)));
        btnForgotPassword.setOnClickListener(v -> startActivity(new Intent(this, ResetPasswordActivity.class)));

        findViewById(R.id.layoutLogo).setOnLongClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Emergency Reset")
                .setMessage("Wipe all Local and Cloud databases?")
                .setPositiveButton("Wipe", (dialog, which) -> {
                    userRepository.nukeEverything(this, () -> runOnUiThread(() -> {
                        Toast.makeText(this, "SYSTEM RESET SUCCESSFUL", Toast.LENGTH_LONG).show();
                        new android.os.Handler().postDelayed(() -> System.exit(0), 2000);
                    }));
                })
                .setNegativeButton("Cancel", null)
                .show();
            return true;
        });
    }

    private void handleNormalLogin(User user) {
        SharedPreferences trustPrefs = getSharedPreferences("DeviceTrust", MODE_PRIVATE);
        long lastVerified = trustPrefs.getLong("trust_" + user.email, 0);
        boolean isTrustValid = (System.currentTimeMillis() - lastVerified < TRUST_EXPIRATION_MILLIS);
        boolean isNewDevice = (lastVerified == 0);

        if (!isOnline()) {
            if (isTrustValid || (!user.isMfaEnabled && !isNewDevice)) {
                proceedToWelcome(user);
            } else {
                Toast.makeText(this, "Verification required. Please go online.", Toast.LENGTH_LONG).show();
            }
        } else {
            if (isNewDevice || (user.isMfaEnabled && !isTrustValid)) {
                showMFADialog(user);
            } else {
                proceedToWelcome(user);
            }
        }
    }

    private void showRestoreAccountDialog(User user) {
        new AlertDialog.Builder(this)
            .setTitle("Restore Account?")
            .setMessage("This account was scheduled for deletion. Would you like to restore it now?")
            .setPositiveButton("Yes, Restore", (dialog, which) -> {
                showMFADialogForRestore(user);
            })
            .setNegativeButton("No", null)
            .show();
    }

    private void showMFADialogForRestore(User user) {
        String code = String.valueOf(100000 + new Random().nextInt(900000));
        
        android.app.ProgressDialog pd = new android.app.ProgressDialog(this);
        pd.setMessage("Sending restoration code...");
        pd.show();

        EmailHelper.sendVerificationCode(user.email, code, (success, error) -> runOnUiThread(() -> {
            pd.dismiss();
            if (success) {
                showCodeInputDialog("Verify Account Restoration", "Enter code sent to " + user.email, code, () -> {
                    userRepository.restoreAccountByUser(user.email, () -> runOnUiThread(() -> {
                        Toast.makeText(this, "Account Restored Successfully!", Toast.LENGTH_SHORT).show();
                        user.isUserDeleted = false;
                        handleNormalLogin(user);
                    }));
                });
            } else {
                Toast.makeText(this, "Failed to send code", Toast.LENGTH_SHORT).show();
            }
        }));
    }

    private void showCodeInputDialog(String title, String message, String correctCode, Runnable onSuccess) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);

        final EditText input = new EditText(this);
        input.setHint("000000");
        input.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
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
            if (input.getText().toString().equals(correctCode)) {
                onSuccess.run();
            } else {
                Toast.makeText(this, "Invalid code", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
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
        biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
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
                                if (user.isUserDeleted) {
                                    showRestoreAccountDialog(user);
                                } else {
                                    proceedToWelcome(user);
                                }
                            });
                        }
                    });
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

                TextInputLayout textInputLayout = new TextInputLayout(this);
                textInputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
                textInputLayout.setHintEnabled(false);

                final TextInputEditText input = new TextInputEditText(this);
                input.setHint("000000");
                input.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                input.setLetterSpacing(0.2f);
                input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
                
                textInputLayout.addView(input);

                LinearLayout container = new LinearLayout(this);
                container.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(64, 32, 64, 8);
                textInputLayout.setLayoutParams(lp);
                container.addView(textInputLayout);
                builder.setView(container);

                builder.setPositiveButton("Verify", (dialog, which) -> {
                    if (input.getText() != null) {
                        String entered = input.getText().toString().trim();
                        if (entered.equals(code)) {
                            SharedPreferences trustPrefs = getSharedPreferences("DeviceTrust", MODE_PRIVATE);
                            trustPrefs.edit().putLong("trust_" + user.email, System.currentTimeMillis()).apply();
                            user.isMfaVerified = true;
                            user.lastMfaVerifiedAt = System.currentTimeMillis();
                            userRepository.updateUser(user, null);
                            proceedToWelcome(user);
                        } else {
                            Toast.makeText(this, "Incorrect verification code", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                builder.setNegativeButton("Cancel", null);
                
                AlertDialog dialog = builder.create();
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                }
                dialog.show();
                input.requestFocus();
            } else {
                Toast.makeText(this, "Failed to send code", Toast.LENGTH_LONG).show();
            }
        }));
    }
}