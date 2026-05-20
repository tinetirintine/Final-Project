package com.example.finalproject;


import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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
import com.google.android.material.textfield.TextInputLayout;
import android.content.SharedPreferences;

import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

public class CreateAccount extends AppCompatActivity {


    private EditText editTextFullName, editTextSignupEmail, editTextPhone,
            editTextBirthday, editTextSignupPassword, editTextConfirmPassword, editTextOtherGender;
    private TextView tvPasswordStrength;
    private AutoCompleteTextView autoCompleteGender;
    private TextInputLayout layoutOtherGender;
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
        setContentView(R.layout.activity_signup);


        userRepository = new UserRepository(this);


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainSignup), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        editTextFullName        = findViewById(R.id.editTextFullName);
        editTextSignupEmail     = findViewById(R.id.editTextSignupEmail);
        editTextPhone           = findViewById(R.id.editTextPhone);
        editTextBirthday        = findViewById(R.id.editTextBirthday);
        autoCompleteGender      = findViewById(R.id.autoCompleteGender);
        layoutOtherGender       = findViewById(R.id.layoutOtherGender);
        editTextOtherGender     = findViewById(R.id.editTextOtherGender);
        tvPasswordStrength      = findViewById(R.id.tvPasswordStrength);
        editTextSignupPassword  = findViewById(R.id.editTextSignupPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        Button btnRegister      = findViewById(R.id.btnRegister);
        TextView tvBackToLogin  = findViewById(R.id.tvBackToLogin);


        // Filter to prevent numbers in Full Name
        editTextFullName.setFilters(new InputFilter[]{(source, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++) {
                if (Character.isDigit(source.charAt(i))) {
                    return "";
                }
            }
            return null;
        }});

        // Birthday DatePicker
        editTextBirthday.setOnClickListener(v -> showDatePicker());

        // Gender Dropdown
        String[] genders = {"Male", "Female", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, genders);
        autoCompleteGender.setAdapter(adapter);

        autoCompleteGender.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            if ("Other".equals(selected)) {
                layoutOtherGender.setVisibility(View.VISIBLE);
            } else {
                layoutOtherGender.setVisibility(View.GONE);
                editTextOtherGender.setText("");
            }
        });

        editTextSignupPassword.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePasswordStrength(s.toString());
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });


        btnRegister.setOnClickListener(v -> {
            String name = editTextFullName.getText().toString().trim();
            String email = editTextSignupEmail.getText().toString().trim();
            String phone = editTextPhone.getText().toString().trim();
            String birthday = editTextBirthday.getText().toString().trim();
            String gender = autoCompleteGender.getText().toString().trim();
            
            if ("Other".equals(gender)) {
                String other = editTextOtherGender.getText().toString().trim();
                if (other.isEmpty()) {
                    Toast.makeText(this, "Please specify your gender", Toast.LENGTH_SHORT).show();
                    return;
                }
                gender = other;
            }

            String password = editTextSignupPassword.getText().toString();
            String confirm = editTextConfirmPassword.getText().toString();


            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || phone.isEmpty() || birthday.isEmpty() || gender.isEmpty()) {
                Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
            } else if (name.matches(".*\\d.*")) {
                Toast.makeText(this, "Full name cannot contain numbers", Toast.LENGTH_SHORT).show();
            } else if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.(com|net|org|edu|gov|ph|info|biz)$")) {
                Toast.makeText(this, "Invalid email format (e.g., .com, .ph)", Toast.LENGTH_SHORT).show();
            } else if (!phone.matches("^(09|\\+639)\\d{9}$")) {
                Toast.makeText(this, "Invalid Philippine phone number (11 digits or +63)", Toast.LENGTH_SHORT).show();
            } else if (!isStrongPassword(password)) {
                Toast.makeText(this, "Password must be at least 8 characters, include uppercase, lowercase, number, and special character", Toast.LENGTH_LONG).show();
            } else if (!Objects.equals(password, confirm)) {
                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show();
            } else {
                String formattedName = formatName(name);
                String hashed = SecurityUtils.hashPassword(password);
                User user = new User(formattedName, email, hashed, phone, birthday, gender);
                
                showVerificationDialog(user);
            }
        });


        tvBackToLogin.setOnClickListener(v -> finish());
    }

    private String formatName(String str) {
        if (str == null || str.isEmpty()) return str;
        String[] words = str.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1).toLowerCase())
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }

    private void showVerificationDialog(User user) {
        String code = String.valueOf(100000 + new java.util.Random().nextInt(900000));
        
        // Show progress dialog or loading state
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("Sending verification code...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        EmailHelper.sendVerificationCode(user.email, code, (success, error) -> runOnUiThread(() -> {
            progressDialog.dismiss();
            
            if (success) {
                Toast.makeText(this, "Verification code sent to " + user.email, Toast.LENGTH_LONG).show();
                
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                builder.setTitle("Verify Email Address");
                builder.setMessage("Please enter the 6-digit verification code sent to " + user.email);

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

                builder.setPositiveButton("Complete Registration", (dialog, which) -> {
                    if (input.getText().toString().equals(code)) {
                        user.isMfaVerified = true; // Trust this device after registration
                        user.lastMfaVerifiedAt = System.currentTimeMillis();
                        userRepository.register(user, registerSuccess -> runOnUiThread(() -> {
                            if (registerSuccess) {
                                Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(this, "Email already exists", Toast.LENGTH_SHORT).show();
                            }
                        }));
                    } else {
                        Toast.makeText(this, "Invalid code", Toast.LENGTH_SHORT).show();
                    }
                });
                builder.setNegativeButton("Cancel", null);
                builder.show();
            } else {
                String msg = "Failed to send verification code.";
                if (error != null) msg += "\nError: " + error;
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            }
        }));
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog datePicker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String date = String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, month + 1, year);
            editTextBirthday.setText(date);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        
        datePicker.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePicker.show();
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
            tvPasswordStrength.setTextColor(android.graphics.Color.parseColor("#f44336"));
        } else if (score == 2) {
            tvPasswordStrength.setText("Password Strength: Weak");
            tvPasswordStrength.setTextColor(android.graphics.Color.parseColor("#ff9800"));
        } else if (score == 3) {
            tvPasswordStrength.setText("Password Strength: Medium");
            tvPasswordStrength.setTextColor(android.graphics.Color.parseColor("#ffc107"));
        } else if (score == 4) {
            tvPasswordStrength.setText("Password Strength: Strong");
            tvPasswordStrength.setTextColor(android.graphics.Color.parseColor("#4caf50"));
        } else {
            tvPasswordStrength.setText("Password Strength: Very Strong");
            tvPasswordStrength.setTextColor(android.graphics.Color.parseColor("#2e7d32"));
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
