package com.example.finalproject;


import android.content.Intent;
import android.content.res.ColorStateList;
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
import com.google.android.material.navigation.NavigationView;
import android.view.View;


public class MainActivity extends AppCompatActivity {


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
            loadFragment(NotesFragment.newInstance("All"));
        });
        btnPersonal.setOnClickListener(v -> {
            updateButtonSelection(btnPersonal);
            loadFragment(NotesFragment.newInstance("Personal"));
        });
        btnSchool.setOnClickListener(v -> {
            updateButtonSelection(btnSchool);
            loadFragment(NotesFragment.newInstance("School"));
        });
        btnWork.setOnClickListener(v -> {
            updateButtonSelection(btnWork);
            loadFragment(NotesFragment.newInstance("Work"));
        });


        // --- ACTION BUTTON ---
        ImageButton btnAdd = findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(v -> showAddNoteMenu(v));


        // --- MENU BUTTON (Opens Drawer) ---
        ImageButton btnMenu = findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));


        // --- DRAWER NAVIGATION ---
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_profile) {
                loadFragment(ProfileFragment.newInstance(userEmail, userName, userPhone, userBirthday, userGender));
            } else if (itemId == R.id.nav_notes) {
                updateButtonSelection(btnAll);
                loadFragment(NotesFragment.newInstance("All"));
            } else if (itemId == R.id.nav_calendar) {
                loadFragment(new CalendarFragment());
            }


            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });


        // Set default fragment to Notes
        if (savedInstanceState == null) {
            updateButtonSelection(btnAll);
            loadFragment(NotesFragment.newInstance("All"));
        }
    }


    private void updateButtonSelection(Button selectedButton) {
        for (Button btn : categoryButtons) {
            if (btn == selectedButton) {
                btn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.purple_500)));
                btn.setTextColor(ContextCompat.getColor(this, R.color.white));
            } else {
                btn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.nav_bg)));
                btn.setTextColor(ContextCompat.getColor(this, R.color.black));
            }
        }
    }


    private void loadFragment(Fragment fragment) {
        // Show/Hide category buttons based on the fragment
        View topCategories = findViewById(R.id.topCategories);
        if (fragment instanceof NotesFragment) {
            topCategories.setVisibility(View.VISIBLE);
        } else {
            topCategories.setVisibility(View.GONE);
        }

        // Show/Hide Add button (hide in Profile)
        ImageButton btnAdd = findViewById(R.id.btnAdd);
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
