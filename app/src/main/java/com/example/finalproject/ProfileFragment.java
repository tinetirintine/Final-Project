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

        Button btnLogout = view.findViewById(R.id.btnLogout);
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
        isEditMode = !isEditMode;

        etName.setEnabled(isEditMode);
        etEmail.setEnabled(isEditMode);
        etPhone.setEnabled(isEditMode);
        etGender.setEnabled(isEditMode);
        etBirth.setEnabled(isEditMode);

        if (isEditMode) {
            btnEditProfile.setImageResource(android.R.drawable.ic_menu_save);
            Toast.makeText(getContext(), "Editing Enabled", Toast.LENGTH_SHORT).show();
        } else {
            btnEditProfile.setImageResource(android.R.drawable.ic_menu_edit);
            saveProfileChanges();
        }
    }

    private void saveProfileChanges() {
        if (currentUser == null) return;

        String newEmail = etEmail.getText().toString().trim();
        
        // If email is changing, check if the new one is already taken
        if (!newEmail.equalsIgnoreCase(currentUser.email)) {
            userRepository.getUserByEmail(newEmail, existingUser -> {
                if (existingUser != null) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Email already in use by another account", Toast.LENGTH_SHORT).show();
                            etEmail.setText(currentUser.email); // Revert to old email
                        });
                    }
                } else {
                    performUpdate(newEmail);
                }
            });
        } else {
            performUpdate(newEmail);
        }
    }

    private void performUpdate(String newEmail) {
        currentUser.fullName = etName.getText().toString().trim();
        currentUser.email = newEmail;
        currentUser.phone = etPhone.getText().toString().trim();
        currentUser.gender = etGender.getText().toString().trim();
        currentUser.birthday = etBirth.getText().toString().trim();

        userRepository.updateUser(currentUser, () -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> 
                    Toast.makeText(getContext(), "Profile Updated Successfully. Use your new email to login next time.", Toast.LENGTH_LONG).show()
                );
            }
        });
    }
}