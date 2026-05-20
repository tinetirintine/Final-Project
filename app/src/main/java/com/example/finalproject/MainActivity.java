package com.example.finalproject;


import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import android.view.View;
import androidx.appcompat.app.AppCompatDelegate;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;


public class MainActivity extends AppCompatActivity {


    private int userId;
    private String userEmail;
    private String userName;
    private String userPhone;
    private String userBirthday;
    private String userGender;
    private DrawerLayout drawerLayout;
    private Button[] categoryButtons;
    private ConnectivityManager.NetworkCallback networkCallback;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Load theme preference before super.onCreate
        SharedPreferences prefs = getSharedPreferences("ThemePrefs", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("isDarkMode", true); // Default to dark as per original app
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        userId = getIntent().getIntExtra("user_id", -1);
        userEmail = getIntent().getStringExtra("email");
        userName = getIntent().getStringExtra("name");
        userPhone = getIntent().getStringExtra("phone");
        userBirthday = getIntent().getStringExtra("birthday");
        userGender = getIntent().getStringExtra("gender");


        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);


        // Handle Back Press to close drawer
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    finish();
                }
            }
        });


        // --- TOP CATEGORY BUTTONS ---
        Button btnAll = findViewById(R.id.btnAll);
        Button btnPersonal = findViewById(R.id.btnPersonal);
        Button btnSchool = findViewById(R.id.btnSchool);
        Button btnWork = findViewById(R.id.btnWork);


        categoryButtons = new Button[]{btnAll, btnPersonal, btnSchool, btnWork};


        btnAll.setOnClickListener(v -> {
            updateButtonSelection(btnAll);
            loadFragment(NotesFragment.newInstance(userId, "All"));
        });
        btnPersonal.setOnClickListener(v -> {
            updateButtonSelection(btnPersonal);
            loadFragment(NotesFragment.newInstance(userId, "Personal"));
        });
        btnSchool.setOnClickListener(v -> {
            updateButtonSelection(btnSchool);
            loadFragment(NotesFragment.newInstance(userId, "School"));
        });
        btnWork.setOnClickListener(v -> {
            updateButtonSelection(btnWork);
            loadFragment(NotesFragment.newInstance(userId, "Work"));
        });


        // --- ACTION BUTTON ---
        FloatingActionButton btnAdd = findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(this::showAddNoteMenu);


        // --- MENU BUTTON (Opens Drawer) ---
        ImageButton btnMenu = findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // --- THEME TOGGLE BUTTON (In Navigation Header) ---
        View headerView = navigationView.getHeaderView(0);
        ImageButton btnThemeToggle = headerView.findViewById(R.id.btnThemeToggleHeader);
        updateThemeIcon(btnThemeToggle, isDarkMode);

        btnThemeToggle.setOnClickListener(v -> {
            boolean currentMode = prefs.getBoolean("isDarkMode", true);
            boolean newMode = !currentMode;
            
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("isDarkMode", newMode);
            editor.apply();

            if (newMode) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
            recreate(); // Recreate to apply theme changes
        });


        // --- DRAWER NAVIGATION ---
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_profile) {
                loadFragment(ProfileFragment.newInstance(userId, userEmail, userName, userPhone, userBirthday, userGender));
            } else if (itemId == R.id.nav_notes) {
                updateButtonSelection(findViewById(R.id.btnAll));
                loadFragment(NotesFragment.newInstance(userId, "All"));
            } else if (itemId == R.id.nav_favorites) {
                loadFragment(NotesFragment.newInstance(userId, "Favorites"));
            } else if (itemId == R.id.nav_checklist) {
                loadFragment(NotesFragment.newInstance(userId, "Checklist"));
            } else if (itemId == R.id.nav_archive) {
                loadFragment(NotesFragment.newInstance(userId, "Archive"));
            } else if (itemId == R.id.nav_trash) {
                loadFragment(NotesFragment.newInstance(userId, "Trash"));
            } else if (itemId == R.id.nav_calendar) {
                loadFragment(CalendarFragment.newInstance(userId));
            }


            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });


        // Set default fragment to Notes
        if (savedInstanceState == null) {
            updateButtonSelection(findViewById(R.id.btnAll));
            loadFragment(NotesFragment.newInstance(userId, "All"));
        } else {
            // Restore visibility logic for FAB and Categories after recreation
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
            if (currentFragment != null) {
                loadFragment(currentFragment);
            }
        }

        setupNetworkMonitoring();
    }


    private void setupNetworkMonitoring() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@androidx.annotation.NonNull android.net.Network network) {
                super.onAvailable(network);
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(MainActivity.this, "Back Online! Syncing notes...", Toast.LENGTH_SHORT).show();
                    // Simulate sync
                    simulateSync();
                });
            }

            @Override
            public void onLost(@androidx.annotation.NonNull android.net.Network network) {
                super.onLost(network);
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(MainActivity.this, "Offline. Changes will be saved locally.", Toast.LENGTH_SHORT).show();
                });
            }
        };

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        cm.registerNetworkCallback(request, networkCallback);
    }

    private void simulateSync() {
        // Here you would normally call an API to sync Room data to a server
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Toast.makeText(MainActivity.this, "Sync Complete!", Toast.LENGTH_SHORT).show();
        }, 2000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkCallback != null) {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            cm.unregisterNetworkCallback(networkCallback);
        }
    }


    private void updateButtonSelection(Button selectedButton) {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        getTheme().resolveAttribute(R.attr.colorCategoryUnselected, typedValue, true);
        int unselectedBg = typedValue.data;
        getTheme().resolveAttribute(R.attr.colorOnCategoryUnselected, typedValue, true);
        int unselectedText = typedValue.data;

        for (Button btn : categoryButtons) {
            if (btn == selectedButton) {
                btn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.brand_purple)));
                btn.setTextColor(ContextCompat.getColor(this, R.color.white));
            } else {
                btn.setBackgroundTintList(ColorStateList.valueOf(unselectedBg));
                btn.setTextColor(unselectedText);
            }
        }
    }


    private void loadFragment(Fragment fragment) {
        if (isFinishing() || isDestroyed()) return;

        // Show/Hide category buttons based on the fragment
        View topCategories = findViewById(R.id.topCategories);
        FloatingActionButton btnAdd = findViewById(R.id.btnAdd);
        
        boolean isMainNotesSection = false;

        if (fragment instanceof NotesFragment) {
            Bundle args = fragment.getArguments();
            String cat = args != null ? args.getString("category") : "All";
            
            // "Main Notes" are All, Personal, School, Work
            if ("Favorites".equals(cat) || "Archive".equals(cat) || "Trash".equals(cat) || "Checklist".equals(cat)) {
                topCategories.setVisibility(View.GONE);
                isMainNotesSection = false;
            } else {
                topCategories.setVisibility(View.VISIBLE);
                isMainNotesSection = true;
            }
        } else {
            topCategories.setVisibility(View.GONE);
            isMainNotesSection = false;
        }

        // Show Add button ONLY in the main notes section
        if (isMainNotesSection) {
            btnAdd.show();
        } else {
            btnAdd.hide();
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commitAllowingStateLoss();
    }

    private void showAddNoteMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.getMenu().add("New");
        popupMenu.getMenu().add("Personal Routine");
        popupMenu.getMenu().add("School Planner");
        popupMenu.getMenu().add("Work Notes");

        popupMenu.setOnMenuItemClickListener(item -> {
            CharSequence titleChar = item.getTitle();
            if (titleChar == null) return false;
            String title = titleChar.toString();
            Intent intent = new Intent(MainActivity.this, AddNoteActivity.class);
            intent.putExtra("user_id", userId);

            if (title.equals("New")) {
                startActivity(intent);
            } else {
                intent.putExtra("is_template", true);
                intent.putExtra("template_name", title);
                startActivity(intent);
            }
            return true;
        });
        popupMenu.show();
    }

    private void updateThemeIcon(ImageButton btn, boolean isDarkMode) {
        if (isDarkMode) {
            btn.setImageResource(R.drawable.ic_sun); // Show sun to switch to light
        } else {
            btn.setImageResource(R.drawable.ic_moon); // Show moon to switch to dark
        }
    }
}
