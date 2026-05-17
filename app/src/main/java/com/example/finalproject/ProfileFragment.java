package com.example.finalproject;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;


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

    private EditText etName, etEmail, etPhone, etGender, etBirth;
    private ImageButton btnEditProfile;
    private Button btnLogout;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null && currentUser != null && getContext() != null) {
                    try {
                        // Request persistable permission
                        getContext().getContentResolver().takePersistableUriPermission(uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (SecurityException e) {
                        // Fallback or log if persistable permission is not supported for this URI
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
            userEmail = getArguments().getString("email");
            userName = getArguments().getString("name");
            userPhone = getArguments().getString("phone");
            userBirthday = getArguments().getString("birthday");
            userGender = getArguments().getString("gender");
        }
        userRepository = new UserRepository(getContext());
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

        etName.setText(userName);
        etEmail.setText(userEmail);
        if (etPhone != null) etPhone.setText(userPhone);
        if (etGender != null) etGender.setText(userGender);
        if (etBirth != null) etBirth.setText(userBirthday);

        // Fetch user from DB to get the profile image path
        userRepository.getUserByEmail(userEmail, user -> {
            currentUser = user;
            if (currentUser != null && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (currentUser.profileImage != null) {
                        try {
                            imgProfile.setImageURI(Uri.parse(currentUser.profileImage));
                        } catch (SecurityException e) {
                            // If we lost permission, just show the default icon
                            imgProfile.setImageResource(android.R.drawable.sym_def_app_icon);
                        }
                    }
                });
            }
        });

        imgProfile.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        btnEditProfile.setOnClickListener(v -> toggleEditMode());

        btnLogout = view.findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            if (getActivity() != null) {
                Intent intent = new Intent(getActivity(), LoginAndSignup.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                getActivity().finish();
            }
        });

        return view;
    }

    private void toggleEditMode() {
        if (isEditMode) {
            // We are trying to SAVE. Check validation first.
            saveProfileChanges();
        } else {
            // We are starting to EDIT.
            isEditMode = true;
            updateUIForMode();
            btnEditProfile.setImageResource(android.R.drawable.ic_menu_save);
            Toast.makeText(getContext(), "Editing Enabled", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUIForMode() {
        etName.setEnabled(isEditMode);
        etEmail.setEnabled(isEditMode);
        etPhone.setEnabled(isEditMode);
        etGender.setEnabled(isEditMode);
        etBirth.setEnabled(isEditMode);
        
        // Hide logout button when editing
        if (btnLogout != null) {
            btnLogout.setVisibility(isEditMode ? View.GONE : View.VISIBLE);
        }
    }

    private void saveProfileChanges() {
        if (currentUser == null) return;

        String newName = etName.getText().toString().trim();
        String newEmail = etEmail.getText().toString().trim();
        String newPhone = etPhone.getText().toString().trim();

        // 1. Validate Name (no numbers)
        if (newName.matches(".*\\d.*")) {
            Toast.makeText(getContext(), "Name cannot contain numbers", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Validate Email format
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            Toast.makeText(getContext(), "Invalid email format", Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. Validate phone number (Philippine format)
        if (!newPhone.matches("^(09|\\+639)\\d{9}$")) {
            Toast.makeText(getContext(), "Invalid Philippine phone number (11 digits or +63)", Toast.LENGTH_SHORT).show();
            return;
        }

        // 4. If email is changing, check if the new one is already taken
        if (!newEmail.equalsIgnoreCase(currentUser.email)) {
            userRepository.getUserByEmail(newEmail, existingUser -> {
                if (existingUser != null) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Email already in use by another account", Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    performUpdate(newName, newEmail, newPhone);
                }
            });
        } else {
            performUpdate(newName, newEmail, newPhone);
        }
    }

    private void performUpdate(String newName, String newEmail, String newPhone) {
        currentUser.fullName = newName;
        currentUser.email = newEmail;
        currentUser.phone = newPhone;
        currentUser.gender = etGender.getText().toString().trim();
        currentUser.birthday = etBirth.getText().toString().trim();

        userRepository.updateUser(currentUser, () -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Profile Updated Successfully", Toast.LENGTH_SHORT).show();
                    // SUCCESS: Turn off edit mode and lock fields
                    isEditMode = false;
                    updateUIForMode();
                    btnEditProfile.setImageResource(android.R.drawable.ic_menu_edit);
                });
            }
        });
    }
}