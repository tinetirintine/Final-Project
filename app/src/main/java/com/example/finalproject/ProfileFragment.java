package com.example.finalproject;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.biometric.BiometricPrompt;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import android.text.Editable;
import android.text.TextWatcher;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.Executor;

public class ProfileFragment extends Fragment {

    private int userId;
    private String userEmail = "Not Logged In";
    private String userName = "Guest";
    private String userPhone = "";
    private String userBirthday = "";
    private String userGender = "";
    private String profileImagePath = null;
    
    private ImageView imgProfile;
    private UserRepository userRepository;
    private User currentUser;
    private boolean isEditMode = false;

    private EditText etName, etEmail, etPhone, etGender, etBirth, etAge;
    private TextView tvPhoneVerificationStatus;
    private Button btnVerifyPhone, btnResetDatabases, btnAdminDashboard, btnDeleteAccount;
    private ImageButton btnEditProfile;
    private Button btnLogout, btnChangePassword;
    private SwitchMaterial switchBiometric, switchMFA;

    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    private final ActivityResultLauncher<String> requestSmsPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    Toast.makeText(getContext(), "Permission granted. Please try verifying again.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "SMS permission denied. Cannot send verification code.", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null && currentUser != null && getContext() != null) {
                    try {
                        getContext().getContentResolver().takePersistableUriPermission(uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (SecurityException e) {
                    }

                    profileImagePath = uri.toString();
                    imgProfile.setImageURI(uri);
                    currentUser.profileImage = profileImagePath;
                    userRepository.updateUser(currentUser, () -> {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> 
                                Toast.makeText(getContext(), "Profile Picture Updated", Toast.LENGTH_SHORT).show()
                            );
                        }
                    });
                }
            }
    );

    public static ProfileFragment newInstance(int userId, String email, String name, String phone, String birthday, String gender) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putInt("userId", userId);
        args.putString("email", email);
        args.putString("name", name);
        args.putString("phone", phone);
        args.putString("birthday", birthday);
        args.putString("gender", gender);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getInt("userId");
            userEmail = getArguments().getString("email", "Not Logged In");
            userName = getArguments().getString("name");
            userPhone = getArguments().getString("phone");
            userBirthday = getArguments().getString("birthday");
            userGender = getArguments().getString("gender");
        }
        userRepository = new UserRepository(getContext());
        setupBiometric();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        imgProfile = view.findViewById(R.id.imgProfile);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        etName = view.findViewById(R.id.etName);
        etEmail = view.findViewById(R.id.etEmail);
        etPhone = view.findViewById(R.id.etPhone);
        etGender = view.findViewById(R.id.etGender);
        etBirth = view.findViewById(R.id.etBirth);
        etAge = view.findViewById(R.id.etAge);
        tvPhoneVerificationStatus = view.findViewById(R.id.tvPhoneVerificationStatus);
        btnVerifyPhone = view.findViewById(R.id.btnVerifyPhone);
        btnResetDatabases = view.findViewById(R.id.btnResetDatabases);
        btnAdminDashboard = view.findViewById(R.id.btnAdminDashboard);
        btnDeleteAccount = view.findViewById(R.id.btnDeleteAccount);
        btnChangePassword = view.findViewById(R.id.btnChangePassword);
        switchBiometric = view.findViewById(R.id.switchBiometric);
        switchMFA = view.findViewById(R.id.switchMFA);

        // Biometric Switch initial state
        SharedPreferences bioPrefs = requireContext().getSharedPreferences("BioPrefs", Context.MODE_PRIVATE);
        boolean isBioEnabled = bioPrefs.getBoolean("bio_enabled_" + userEmail, false);
        switchBiometric.setChecked(isBioEnabled);

        switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                biometricPrompt.authenticate(promptInfo);
            } else {
                bioPrefs.edit().putBoolean("bio_enabled_" + userEmail, false).apply();
                Toast.makeText(getContext(), "Biometric login disabled", Toast.LENGTH_SHORT).show();
            }
        });

        switchMFA.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (currentUser != null && currentUser.isMfaEnabled != isChecked) {
                currentUser.isMfaEnabled = isChecked;
                userRepository.updateUser(currentUser, () -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> 
                            Toast.makeText(getContext(), isChecked ? "MFA Enabled" : "MFA Disabled", Toast.LENGTH_SHORT).show()
                        );
                    }
                });
            }
        });

        userRepository.getUserByEmail(userEmail, user -> {
            currentUser = user;
            if (currentUser != null && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    etName.setText(currentUser.fullName);
                    etEmail.setText(currentUser.email);
                    if (etPhone != null) etPhone.setText(currentUser.phone);
                    if (etGender != null) etGender.setText(currentUser.gender);
                    if (etBirth != null) {
                        etBirth.setText(currentUser.birthday);
                        updateAgeDisplay(currentUser.birthday);
                    }
                    
                    updatePhoneVerificationUI(currentUser.isPhoneVerified);
                    switchMFA.setChecked(currentUser.isMfaEnabled);
                    if (currentUser.profileImage != null) {
                        try {
                            imgProfile.setImageURI(Uri.parse(currentUser.profileImage));
                        } catch (SecurityException e) {
                            imgProfile.setImageResource(android.R.drawable.sym_def_app_icon);
                        }
                    }
                });
            }
        });

        imgProfile.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        btnEditProfile.setOnClickListener(v -> toggleEditMode());
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        btnVerifyPhone.setOnClickListener(v -> {
            String phone = etPhone.getText().toString().trim();
            if (!phone.matches("^(09|\\+639)\\d{9}$")) {
                Toast.makeText(getContext(), "Enter a valid PH phone number first", Toast.LENGTH_SHORT).show();
            } else {
                startPhoneVerification(phone, () -> {
                    updatePhoneVerificationUI(true);
                    btnVerifyPhone.setVisibility(View.GONE);
                });
            }
        });

        if ("adminpogi".equals(userEmail)) {
            btnResetDatabases.setVisibility(View.VISIBLE);
            btnResetDatabases.setOnClickListener(v -> {
                new AlertDialog.Builder(requireContext())
                    .setTitle("DANGER: WIPE EVERYTHING?")
                    .setMessage("This will delete ALL USERS and ALL NOTES from both Local DB and Firestore Cloud. This cannot be undone.")
                    .setPositiveButton("YES, NUKE IT", (dialog, which) -> {
                        userRepository.nukeEverything(getContext(), () -> {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(), "DATABASE WIPED. SHUTTING DOWN.", Toast.LENGTH_LONG).show();
                                    new android.os.Handler().postDelayed(() -> System.exit(0), 2000);
                                });
                            }
                        });
                    })
                    .setNegativeButton("CANCEL", null)
                    .show();
            });

            btnAdminDashboard.setVisibility(View.VISIBLE);
            btnAdminDashboard.setOnClickListener(v -> {
                startActivity(new Intent(getActivity(), AdminDashboardActivity.class));
            });

            btnDeleteAccount.setVisibility(View.GONE);
        } else {
            btnDeleteAccount.setVisibility(View.VISIBLE);
            btnDeleteAccount.setOnClickListener(v -> {
                new AlertDialog.Builder(requireContext())
                    .setTitle("Delete Account?")
                    .setMessage("Are you sure you want to delete your account? Your data will be hidden for 30 days before being permanently removed.")
                    .setPositiveButton("Yes, Delete", (dialog, which) -> {
                        userRepository.deleteAccountByUser(userEmail, () -> {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(), "Account scheduled for deletion.", Toast.LENGTH_LONG).show();
                                    // Logout
                                    SharedPreferences loginPrefs = getActivity().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
                                    loginPrefs.edit().clear().apply();
                                    Intent intent = new Intent(getActivity(), LoginAndSignup.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    getActivity().finish();
                                });
                            }
                        });
                    })
                    .setNegativeButton("No", null)
                    .show();
            });
        }

        btnLogout = view.findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    if (getActivity() != null) {
                        SharedPreferences loginPrefs = getActivity().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
                        loginPrefs.edit().clear().apply();
                        Intent intent = new Intent(getActivity(), LoginAndSignup.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        getActivity().finish();
                    }
                })
                .setNegativeButton("No", null)
                .show();
        });

        etEmail.setOnClickListener(v -> {
            if (isEditMode) showEmailChangePrompt();
        });

        etPhone.setOnClickListener(v -> {
            if (isEditMode && currentUser != null && currentUser.isPhoneVerified) showPhoneChangePrompt();
        });

        etBirth.setOnClickListener(v -> {
            if (isEditMode) showDatePicker();
        });

        updateUIForMode();
        return view;
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        String currentBirth = etBirth.getText().toString();
        if (!currentBirth.isEmpty()) {
            try {
                String[] parts = currentBirth.split("/");
                if (parts.length == 3) {
                    cal.set(Calendar.YEAR, Integer.parseInt(parts[2]));
                    cal.set(Calendar.MONTH, Integer.parseInt(parts[1]) - 1);
                    cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parts[0]));
                }
            } catch (Exception ignored) {}
        }
        android.app.DatePickerDialog datePicker = new android.app.DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            String date = String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, month + 1, year);
            etBirth.setText(date);
            updateAgeDisplay(date);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        datePicker.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePicker.show();
    }

    private void setupBiometric() {
        Executor executor = ContextCompat.getMainExecutor(requireContext());
        biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                switchBiometric.setChecked(false);
                Toast.makeText(getContext(), "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                SharedPreferences bioPrefs = requireContext().getSharedPreferences("BioPrefs", Context.MODE_PRIVATE);
                bioPrefs.edit().putBoolean("bio_enabled_" + userEmail, true).apply();
                Toast.makeText(getContext(), "Fingerprint connected successfully!", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getContext(), "Authentication failed", Toast.LENGTH_SHORT).show();
            }
        });
        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Verification")
                .setSubtitle("Authenticate to enable fingerprint login")
                .setNegativeButtonText("Cancel")
                .build();
    }

    private void updateAgeDisplay(String birthday) {
        if (birthday == null || birthday.isEmpty()) {
            etAge.setText("--");
            return;
        }
        try {
            String[] parts = birthday.split("/");
            if (parts.length == 3) {
                int d = Integer.parseInt(parts[0]);
                int m = Integer.parseInt(parts[1]) - 1;
                int y = Integer.parseInt(parts[2]);
                Calendar dob = Calendar.getInstance();
                dob.set(y, m, d);
                Calendar today = Calendar.getInstance();
                int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
                if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) age--;
                etAge.setText(String.valueOf(age));
            }
        } catch (Exception e) {
            etAge.setText("--");
        }
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Change Password");
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(64, 32, 64, 32);
        final TextView tvStrength = new TextView(getContext());
        tvStrength.setText("Password Strength: N/A");
        tvStrength.setTextSize(12);
        tvStrength.setPadding(0, 0, 0, 16);
        layout.addView(tvStrength);
        TextInputLayout tilNew = new TextInputLayout(requireContext());
        tilNew.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        tilNew.setHintEnabled(true);
        tilNew.setHint("New Password");
        tilNew.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        TextInputEditText etNewPass = new TextInputEditText(tilNew.getContext());
        etNewPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        tilNew.addView(etNewPass);
        layout.addView(tilNew);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) tilNew.getLayoutParams();
        params.bottomMargin = 24;
        tilNew.setLayoutParams(params);
        TextInputLayout tilConfirm = new TextInputLayout(requireContext());
        tilConfirm.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        tilConfirm.setHintEnabled(true);
        tilConfirm.setHint("Confirm New Password");
        tilConfirm.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        TextInputEditText etConfirmPass = new TextInputEditText(tilConfirm.getContext());
        etConfirmPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        tilConfirm.addView(etConfirmPass);
        layout.addView(tilConfirm);
        etNewPass.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateDialogPasswordStrength(s.toString(), tvStrength);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        builder.setView(layout);
        builder.setPositiveButton("Change", null);
        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newPass = etNewPass.getText().toString();
            String confirmPass = etConfirmPass.getText().toString();
            tilNew.setError(null);
            tilConfirm.setError(null);
            if (newPass.isEmpty()) tilNew.setError("Password cannot be empty");
            else if (!isStrongPassword(newPass)) tilNew.setError("Password is too weak");
            else if (!newPass.equals(confirmPass)) tilConfirm.setError("Passwords do not match");
            else if (currentUser != null && SecurityUtils.hashPassword(newPass).equals(currentUser.password)) tilNew.setError("You cannot use your current password");
            else {
                dialog.dismiss();
                startEmailVerification(userEmail, "Verify Password Change", "Enter code sent to", () -> {
                    String hashed = SecurityUtils.hashPassword(newPass);
                    userRepository.updatePassword(userEmail, hashed, () -> {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                currentUser.password = hashed;
                                Toast.makeText(getContext(), "Password changed successfully!", Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                });
            }
        });
    }

    private void updateDialogPasswordStrength(String password, TextView tvStrength) {
        if (password.isEmpty()) {
            tvStrength.setText("Password Strength: N/A");
            tvStrength.setTextColor(android.graphics.Color.GRAY);
            return;
        }
        int score = calculatePasswordStrength(password);
        if (score <= 1) {
            tvStrength.setText("Password Strength: Very Weak");
            tvStrength.setTextColor(android.graphics.Color.parseColor("#f44336"));
        } else if (score == 2) {
            tvStrength.setText("Password Strength: Weak");
            tvStrength.setTextColor(android.graphics.Color.parseColor("#ff9800"));
        } else if (score == 3) {
            tvStrength.setText("Password Strength: Medium");
            tvStrength.setTextColor(android.graphics.Color.parseColor("#ffc107"));
        } else if (score == 4) {
            tvStrength.setText("Password Strength: Strong");
            tvStrength.setTextColor(android.graphics.Color.parseColor("#4caf50"));
        } else {
            tvStrength.setText("Password Strength: Very Strong");
            tvStrength.setTextColor(android.graphics.Color.parseColor("#2e7d32"));
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

    private void toggleEditMode() {
        if (currentUser == null) {
            Toast.makeText(getContext(), "Loading user data...", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isEditMode) saveProfileChanges();
        else {
            isEditMode = true;
            updateUIForMode();
            btnEditProfile.setImageResource(android.R.drawable.ic_menu_save);
            Toast.makeText(getContext(), "Editing Enabled", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUIForMode() {
        etName.setEnabled(isEditMode);
        etEmail.setEnabled(isEditMode);
        etEmail.setFocusable(false);
        etEmail.setCursorVisible(false);
        if (currentUser != null && currentUser.isPhoneVerified) {
            etPhone.setEnabled(isEditMode);
            etPhone.setFocusable(false);
            etPhone.setCursorVisible(false);
        } else {
            etPhone.setEnabled(isEditMode);
            etPhone.setFocusableInTouchMode(isEditMode);
            etPhone.setCursorVisible(isEditMode);
        }
        etGender.setEnabled(isEditMode);
        etBirth.setEnabled(isEditMode);
        etBirth.setFocusable(false);
        etBirth.setCursorVisible(false);
        btnChangePassword.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        if (isEditMode && currentUser != null && !currentUser.isPhoneVerified) btnVerifyPhone.setVisibility(View.VISIBLE);
        else btnVerifyPhone.setVisibility(View.GONE);
        if (btnLogout != null) btnLogout.setVisibility(isEditMode ? View.GONE : View.VISIBLE);
    }

    private void saveProfileChanges() {
        if (currentUser == null) return;
        final String newName = etName.getText().toString().trim();
        final String newPhone = etPhone.getText().toString().trim();
        final String newBirth = etBirth.getText().toString().trim();
        if (newName.matches(".*\\d.*")) {
            Toast.makeText(getContext(), "Name cannot contain numbers", Toast.LENGTH_SHORT).show();
            return;
        }
        updateAgeDisplay(newBirth);
        if (!currentUser.isPhoneVerified && !newPhone.equals(currentUser.phone)) {
            if (!newPhone.matches("^(09|\\+639)\\d{9}$")) {
                Toast.makeText(getContext(), "Invalid Philippine phone number (11 digits or +63)", Toast.LENGTH_SHORT).show();
                return;
            }
            userRepository.checkAvailability("", newPhone, error -> {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (error != null) Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                        else startPhoneVerification(newPhone, () -> performUpdate(newName, currentUser.email, newPhone, newBirth, true));
                    });
                }
            });
        } else performUpdate(newName, currentUser.email, currentUser.phone, newBirth, false);
    }

    private void showPhoneChangePrompt() {
        new AlertDialog.Builder(requireContext())
            .setTitle("Change Phone Number?")
            .setMessage("Your phone number is already verified. Changing it will require a new verification.")
            .setPositiveButton("Yes", (dialog, which) -> showNewPhoneInputDialog())
            .setNegativeButton("No", null)
            .show();
    }

    private void showNewPhoneInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Enter New Phone Number");
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(64, 32, 64, 32);
        TextInputLayout til = new TextInputLayout(requireContext());
        til.setHintEnabled(true);
        til.setHint("09xxxxxxxxx");
        til.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        TextInputEditText etInput = new TextInputEditText(til.getContext());
        etInput.setInputType(InputType.TYPE_CLASS_PHONE);
        til.addView(etInput);
        layout.addView(til);
        builder.setView(layout);
        builder.setPositiveButton("Verify", null);
        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newPhone = etInput.getText().toString().trim();
            til.setError(null);
            if (currentUser != null && newPhone.equals(currentUser.phone)) til.setError("You cannot use your current phone number");
            else if (!newPhone.matches("^(09|\\+639)\\d{9}$")) til.setError("Invalid PH phone number format");
            else {
                userRepository.checkAvailability("", newPhone, error -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (error != null) til.setError(error);
                            else {
                                dialog.dismiss();
                                startPhoneVerification(newPhone, () -> {
                                    currentUser.phone = newPhone;
                                    currentUser.isPhoneVerified = true;
                                    etPhone.setText(newPhone);
                                    userRepository.updateUser(currentUser, () -> {
                                        if (getActivity() != null) {
                                            getActivity().runOnUiThread(() -> {
                                                updatePhoneVerificationUI(true);
                                                Toast.makeText(getContext(), "Phone number updated and verified!", Toast.LENGTH_SHORT).show();
                                            });
                                        }
                                    });
                                });
                            }
                        });
                    }
                });
            }
        });
    }

    private void showEmailChangePrompt() {
        new AlertDialog.Builder(requireContext())
            .setTitle("Change Email?")
            .setMessage("Are you sure you want to change your email address? You will need to verify the new email.")
            .setPositiveButton("Yes", (dialog, which) -> showNewEmailInputDialog())
            .setNegativeButton("No", null)
            .show();
    }

    private void showNewEmailInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Enter New Email");
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(64, 32, 64, 32);
        TextInputLayout til = new TextInputLayout(requireContext());
        til.setHintEnabled(true);
        til.setHint("example@email.com");
        til.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        TextInputEditText etInput = new TextInputEditText(til.getContext());
        etInput.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        til.addView(etInput);
        layout.addView(til);
        builder.setView(layout);
        builder.setPositiveButton("Verify", null);
        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newEmail = etInput.getText().toString().trim();
            til.setError(null);
            if (currentUser != null && newEmail.equalsIgnoreCase(currentUser.email)) til.setError("You cannot use your current email");
            else if (!newEmail.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.(com|net|org|edu|gov|ph|info|biz)$")) til.setError("Invalid email format");
            else {
                userRepository.checkAvailability(newEmail, "", error -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (error != null) til.setError(error);
                            else {
                                dialog.dismiss();
                                startEmailVerification(newEmail, "Verify New Email", "Enter code sent to", () -> {
                                    currentUser.email = newEmail;
                                    userEmail = newEmail;
                                    if (getArguments() != null) getArguments().putString("email", newEmail);
                                    etEmail.setText(newEmail);
                                    userRepository.updateUser(currentUser, () -> {
                                        if (getActivity() != null) {
                                            getActivity().runOnUiThread(() -> 
                                                Toast.makeText(getContext(), "Email updated successfully!", Toast.LENGTH_SHORT).show()
                                            );
                                        }
                                    });
                                });
                            }
                        });
                    }
                });
            }
        });
    }

    private void updatePhoneVerificationUI(boolean isVerified) {
        if (tvPhoneVerificationStatus == null) return;
        if (isVerified) {
            tvPhoneVerificationStatus.setText("(Verified)");
            tvPhoneVerificationStatus.setTextColor(android.graphics.Color.parseColor("#4caf50")); 
        } else {
            tvPhoneVerificationStatus.setText("(Not Verified)");
            tvPhoneVerificationStatus.setTextColor(android.graphics.Color.parseColor("#f44336")); 
        }
    }

    private void startEmailVerification(String email, String title, String messagePrefix, Runnable onSuccess) {
        if (getActivity() == null) return;
        String code = String.valueOf(100000 + new java.util.Random().nextInt(900000));
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(getContext());
        progressDialog.setMessage("Sending verification code to " + email + "...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        EmailHelper.sendVerificationCode(email, code, (success, error) -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    if (success) showCodeInputDialog(title, messagePrefix + " " + email, code, onSuccess);
                    else {
                        String msg = "Failed to send code.";
                        if (error != null) msg += "\nError: " + error;
                        Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void startPhoneVerification(String phone, Runnable onSuccess) {
        if (getActivity() == null) return;
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestSmsPermissionLauncher.launch(Manifest.permission.SEND_SMS);
            return;
        }
        String phoneCode = String.valueOf(100000 + new java.util.Random().nextInt(900000));
        android.app.ProgressDialog pd = new android.app.ProgressDialog(getContext());
        pd.setMessage("Sending verification code to " + phone + "...");
        pd.setCancelable(false);
        pd.show();
        SmsHelper.sendVerificationSMS(requireContext(), phone, phoneCode, (success, error) -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    pd.dismiss();
                    if (success) {
                        Toast.makeText(getContext(), "Verification code sent to " + phone, Toast.LENGTH_SHORT).show();
                        showCodeInputDialog("Verify Phone Number", "Please enter the 6-digit code sent to " + phone, phoneCode, () -> {
                                currentUser.isPhoneVerified = true;
                                onSuccess.run();
                            });
                    } else {
                        String msg = "Failed to send SMS.";
                        if (error != null) msg += "\nError: " + error;
                        Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void showCodeInputDialog(String title, String message, String correctCode, Runnable onSuccess) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle(title);
        builder.setMessage(message);
        final EditText input = new EditText(getContext());
        input.setHint("000000");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(64, 32, 64, 32);
        input.setLayoutParams(lp);
        android.widget.LinearLayout container = new android.widget.LinearLayout(getContext());
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.addView(input);
        builder.setView(container);
        builder.setPositiveButton("Verify", null);
        builder.setNegativeButton("Cancel", null);
        android.app.AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (input.getText().toString().equals(correctCode)) {
                dialog.dismiss();
                onSuccess.run();
            } else Toast.makeText(getContext(), "Invalid code", Toast.LENGTH_SHORT).show();
        });
    }

    private void performUpdate(String newName, String newEmail, String newPhone, String newBirth, boolean phoneJustVerified) {
        currentUser.fullName = formatName(newName);
        currentUser.email = newEmail;
        currentUser.phone = newPhone;
        currentUser.gender = etGender.getText().toString().trim();
        currentUser.birthday = newBirth;
        if (phoneJustVerified) currentUser.isPhoneVerified = true;
        userRepository.updateUser(currentUser, () -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Profile Updated Successfully", Toast.LENGTH_SHORT).show();
                    etName.setText(currentUser.fullName);
                    updatePhoneVerificationUI(currentUser.isPhoneVerified);
                    isEditMode = false;
                    updateUIForMode();
                    btnEditProfile.setImageResource(android.R.drawable.ic_menu_edit);
                });
            }
        });
    }

    private String formatName(String str) {
        if (str == null || str.isEmpty()) return str;
        String[] words = str.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1).toLowerCase())
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }
}
