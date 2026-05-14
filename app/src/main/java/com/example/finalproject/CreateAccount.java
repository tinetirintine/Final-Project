package com.example.finalproject;


import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Patterns;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

public class CreateAccount extends AppCompatActivity {


    private EditText editTextFullName, editTextSignupEmail, editTextPhone,
            editTextBirthday, editTextSignupPassword, editTextConfirmPassword;
    private AutoCompleteTextView autoCompleteGender;
    private UserRepository userRepository;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
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


        btnRegister.setOnClickListener(v -> {
            String name = editTextFullName.getText().toString().trim();
            String email = editTextSignupEmail.getText().toString().trim();
            String phone = editTextPhone.getText().toString().trim();
            String birthday = editTextBirthday.getText().toString().trim();
            String gender = autoCompleteGender.getText().toString().trim();
            String password = editTextSignupPassword.getText().toString();
            String confirm = editTextConfirmPassword.getText().toString();


            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || phone.isEmpty() || birthday.isEmpty() || gender.isEmpty()) {
                Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
            } else if (name.matches(".*\\d.*")) {
                Toast.makeText(this, "Full name cannot contain numbers", Toast.LENGTH_SHORT).show();
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show();
            } else if (password.length() < 6) {
                Toast.makeText(this, "Password too short!", Toast.LENGTH_SHORT).show();
            } else if (!Objects.equals(password, confirm)) {
                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show();
            } else {
                String hashed = SecurityUtils.hashPassword(password);
                User user = new User(name, email, hashed, phone, birthday, gender);
                userRepository.register(user, success -> runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Email already exists", Toast.LENGTH_SHORT).show();
                    }
                }));
            }
        });


        tvBackToLogin.setOnClickListener(v -> finish());
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String date = String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, month + 1, year);
            editTextBirthday.setText(date);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }
}
