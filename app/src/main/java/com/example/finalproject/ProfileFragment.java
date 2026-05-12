package com.example.finalproject;


import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;


public class ProfileFragment extends Fragment {


    private String userEmail = "Not Logged In";
    private String userName = "Guest";
    private String userPhone = "";
    private String userBirthday = "";
    private String userGender = "";


    public static ProfileFragment newInstance(String email, String name, String phone, String birthday, String gender) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
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
            userEmail = getArguments().getString("email");
            userName = getArguments().getString("name");
            userPhone = getArguments().getString("phone");
            userBirthday = getArguments().getString("birthday");
            userGender = getArguments().getString("gender");
        }
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        EditText etName = view.findViewById(R.id.etName);
        EditText etEmail = view.findViewById(R.id.etEmail);
        EditText etPhone = view.findViewById(R.id.etPhone);
        EditText etGender = view.findViewById(R.id.etGender);
        EditText etBirth = view.findViewById(R.id.etBirth);

        etName.setText(userName);
        etEmail.setText(userEmail);
        if (etPhone != null) etPhone.setText(userPhone);
        if (etGender != null) etGender.setText(userGender);
        if (etBirth != null) etBirth.setText(userBirthday);

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
}