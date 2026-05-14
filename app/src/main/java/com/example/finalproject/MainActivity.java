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


public class MainActivity extends AppCompatActivity {


    private int userId;
    private String userEmail;
    private String userName;
    private String userPhone;
    private String userBirthday;
    private String userGender;
    private DrawerLayout drawerLayout;
    private Button[] categoryButtons;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
                    setEnabled(false);
                    onBackPressed();
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


        // --- DRAWER NAVIGATION ---
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_profile) {
                loadFragment(ProfileFragment.newInstance(userEmail, userName, userPhone, userBirthday, userGender));
            } else if (itemId == R.id.nav_notes) {
                updateButtonSelection(findViewById(R.id.btnAll));
                loadFragment(NotesFragment.newInstance(userId, "All"));
            } else if (itemId == R.id.nav_favorites) {
                loadFragment(NotesFragment.newInstance(userId, "Favorites"));
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
        }
    }


    private void updateButtonSelection(Button selectedButton) {
        for (Button btn : categoryButtons) {
            if (btn == selectedButton) {
                btn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.brand_purple)));
                btn.setTextColor(ContextCompat.getColor(this, R.color.white));
            } else {
                btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#222222")));
                btn.setTextColor(ContextCompat.getColor(this, R.color.white));
            }
        }
    }


    private void loadFragment(Fragment fragment) {
        // Show/Hide category buttons based on the fragment
        View topCategories = findViewById(R.id.topCategories);
        if (fragment instanceof NotesFragment) {
            Bundle args = fragment.getArguments();
            String cat = args != null ? args.getString("category") : "All";
            if ("Favorites".equals(cat) || "Archive".equals(cat) || "Trash".equals(cat)) {
                topCategories.setVisibility(View.GONE);
            } else {
                topCategories.setVisibility(View.VISIBLE);
            }
        } else {
            topCategories.setVisibility(View.GONE);
        }

        // Show/Hide Add button (hide in Profile)
        FloatingActionButton btnAdd = findViewById(R.id.btnAdd);
        if (fragment instanceof ProfileFragment) {
            btnAdd.setVisibility(View.GONE);
        } else {
            btnAdd.setVisibility(View.VISIBLE);
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    private void showAddNoteMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.getMenu().add("New");
        popupMenu.getMenu().add("Personal Routine");
        popupMenu.getMenu().add("School Planner");
        popupMenu.getMenu().add("Work Notes");

        popupMenu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
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
}
