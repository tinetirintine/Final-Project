package com.example.finalproject;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AdminDashboardActivity extends AppCompatActivity {

    private RecyclerView rvUsers;
    private UserAdapter adapter;
    private UserRepository userRepository;
    private List<User> userList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        rvUsers = findViewById(R.id.rvUsers);
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        
        userRepository = new UserRepository(this);
        
        adapter = new UserAdapter(userList);
        rvUsers.setAdapter(adapter);
        
        loadUsers();
    }

    private void loadUsers() {
        userRepository.getAllUsers(users -> {
            runOnUiThread(() -> {
                userList.clear();
                userList.addAll(users);
                adapter.notifyDataSetChanged();
            });
        });
    }

    private class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
        private List<User> items;

        UserAdapter(List<User> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new UserViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_admin, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            User user = items.get(position);
            holder.tvName.setText(user.fullName);
            holder.tvEmail.setText(user.email);
            
            if (user.isAdminDeleted) {
                holder.tvStatus.setText("Status: Admin Disabled");
                holder.tvStatus.setTextColor(android.graphics.Color.RED);
                holder.btnRestore.setVisibility(View.VISIBLE);
                holder.btnDelete.setVisibility(View.GONE);
                holder.itemView.setAlpha(0.6f);
            } else if (user.isUserDeleted) {
                long daysRemaining = 30 - ((System.currentTimeMillis() - user.deletedAt) / (1000 * 60 * 60 * 24));
                holder.tvStatus.setText("Status: User Deleted (" + daysRemaining + " days left)");
                holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#FF5722")); // Orange
                holder.btnRestore.setVisibility(View.GONE);
                holder.btnDelete.setVisibility(View.VISIBLE); // Admin can still force delete or manage
                holder.itemView.setAlpha(0.7f);
            } else {
                holder.tvStatus.setText("Status: Active");
                holder.tvStatus.setTextColor(ContextCompat.getColor(AdminDashboardActivity.this, R.color.brand_purple));
                holder.btnRestore.setVisibility(View.GONE);
                holder.btnDelete.setVisibility(View.VISIBLE);
                holder.itemView.setAlpha(1.0f);
            }

            holder.btnRestore.setOnClickListener(v -> {
                userRepository.restoreUserByAdmin(user.email, () -> {
                    runOnUiThread(() -> {
                        Toast.makeText(AdminDashboardActivity.this, "User Restored", Toast.LENGTH_SHORT).show();
                        loadUsers();
                    });
                });
            });
            
            holder.btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(AdminDashboardActivity.this)
                    .setTitle("Delete User?")
                    .setMessage("Temporarily disable " + user.email + "? You can restore them later.")
                    .setPositiveButton("Yes, Delete", (dialog, which) -> {
                        userRepository.deleteUserPermanently(AdminDashboardActivity.this, user.email, () -> {
                            runOnUiThread(() -> {
                                Toast.makeText(AdminDashboardActivity.this, "User Disabled", Toast.LENGTH_SHORT).show();
                                loadUsers();
                            });
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class UserViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvEmail, tvStatus;
            ImageButton btnDelete, btnRestore;

            UserViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvUserName);
                tvEmail = itemView.findViewById(R.id.tvUserEmail);
                tvStatus = itemView.findViewById(R.id.tvStatus);
                btnDelete = itemView.findViewById(R.id.btnDeleteUser);
                btnRestore = itemView.findViewById(R.id.btnRestoreUser);
            }
        }
    }
}