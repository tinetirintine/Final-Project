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
            editTextBirthday, editTextSignupPassword, editTextConfirmPassword, editTextOtherGender, editTextAge;
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
        editTextAge             = findViewById(R.id.editTextAge);
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
            String ageStr = editTextAge.getText().toString().trim();
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


            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || phone.isEmpty() || birthday.isEmpty() || gender.isEmpty() || ageStr.isEmpty()) {
                Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
            } else if (name.matches(".*\\d.*")) {
                Toast.makeText(this, "Full name cannot contain numbers", Toast.LENGTH_SHORT).show();
            } else if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.(com|net|org|edu|gov|ph|info|biz)$")) {
                Toast.makeText(this, "Invalid email format (e.g., .com, .ph)", Toast.LENGTH_SHORT).show();
            } else if (!phone.matches("^(09|\\+639)\\d{9}$")) {
                Toast.makeText(this, "Invalid Philippine phone number (11 digits or +63)", Toast.LENGTH_SHORT).show();
            } else if (!isValidAge(ageStr)) {
                Toast.makeText(this, "Age must be between 10 and 125", Toast.LENGTH_SHORT).show();
            } else if (!isStrongPassword(password)) {
                Toast.makeText(this, "Password must be at least 8 characters, include uppercase, lowercase, number, and special character", Toast.LENGTH_LONG).show();
            } else if (!Objects.equals(password, confirm)) {
                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show();
            } else {
                String hashed = SecurityUtils.hashPassword(password);
                User user = new User(name, email, hashed, phone, birthday, gender);
                
                showVerificationDialog(user);
            }
        });


        tvBackToLogin.setOnClickListener(v -> finish());
    }

    private void showVerificationDialog(User user) {
        String code = String.valueOf(100000 + new java.util.Random().nextInt(900000));
        
        Toast.makeText(this, "Verification code sent to " + user.email, Toast.LENGTH_LONG).show();
        Toast.makeText(this, "Code: " + code, Toast.LENGTH_LONG).show();

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
                userRepository.register(user, success -> runOnUiThread(() -> {
                    if (success) {
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
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog datePicker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String date = String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, month + 1, year);
            editTextBirthday.setText(date);
            
            // Auto-calculate and set age
            int age = calculateAge(year, month, dayOfMonth);
            editTextAge.setText(String.valueOf(age));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        
        datePicker.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePicker.show();
    }

    private int calculateAge(int year, int month, int day) {
        Calendar dob = Calendar.getInstance();
        dob.set(year, month, day);
        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }
        return age;
    }

    private boolean isValidAge(String ageStr) {
        try {
            int age = Integer.parseInt(ageStr);
            if (age < 10 || age > 125) return false;
            
            // Verify it matches birthday if birthday is set
            String dobStr = editTextBirthday.getText().toString();
            if (!dobStr.isEmpty()) {
                String[] parts = dobStr.split("/");
                if (parts.length == 3) {
                    int d = Integer.parseInt(parts[0]);
                    int m = Integer.parseInt(parts[1]) - 1;
                    int y = Integer.parseInt(parts[2]);
                    int calculated = calculateAge(y, m, d);
                    return age == calculated;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
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
