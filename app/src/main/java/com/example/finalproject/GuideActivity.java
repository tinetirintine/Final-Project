package com.example.finalproject;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import java.util.ArrayList;
import java.util.List;

public class GuideActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private Button btnNext;
    private TextView btnSkip;
    private List<GuideItem> guideItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);

        viewPager = findViewById(R.id.viewPager);
        btnNext = findViewById(R.id.btnNext);
        btnSkip = findViewById(R.id.btnSkip);
        TabLayout tabLayout = findViewById(R.id.tabLayout);

        setupGuideItems();

        GuideAdapter adapter = new GuideAdapter(guideItems);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {}).attach();

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == guideItems.size() - 1) {
                    btnNext.setText("Get Started");
                } else {
                    btnNext.setText("Next");
                }
            }
        });

        btnNext.setOnClickListener(v -> {
            if (viewPager.getCurrentItem() < guideItems.size() - 1) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
            } else {
                finishGuide();
            }
        });

        btnSkip.setOnClickListener(v -> finishGuide());
    }

    private void setupGuideItems() {
        guideItems = new ArrayList<>();
        guideItems.add(new GuideItem("Welcome to Noteable", "Your ultimate companion for capturing thoughts and organizing your life.", R.drawable.ic_launcher_foreground));
        guideItems.add(new GuideItem("Easy Note Taking", "Create, edit, and organize notes with ease. Pin important ones to the top!", R.drawable.ic_pin_filled));
        guideItems.add(new GuideItem("Calendar View", "Manage your schedule and never miss a deadline with the integrated calendar.", R.drawable.ic_launcher_foreground));
        guideItems.add(new GuideItem("Secure & Dark Mode", "Keep your notes safe with biometric login and enjoy a beautiful dark theme.", R.drawable.ic_moon));
    }

    private void finishGuide() {
        String email = getIntent().getStringExtra("email");
        if (email != null) {
            SharedPreferences prefs = getSharedPreferences("GuidePrefs", MODE_PRIVATE);
            prefs.edit().putBoolean("hasSeenGuide_" + email, true).apply();
        }

        Intent intent = new Intent(GuideActivity.this, MainActivity.class);
        // Pass all extras from previous activity
        intent.putExtras(getIntent());
        startActivity(intent);
        finish();
    }

    private static class GuideItem {
        String title;
        String description;
        int imageRes;

        GuideItem(String title, String description, int imageRes) {
            this.title = title;
            this.description = description;
            this.imageRes = imageRes;
        }
    }

    private class GuideAdapter extends RecyclerView.Adapter<GuideAdapter.GuideViewHolder> {
        private List<GuideItem> items;

        GuideAdapter(List<GuideItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public GuideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new GuideViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_guide, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull GuideViewHolder holder, int position) {
            GuideItem item = items.get(position);
            holder.tvTitle.setText(item.title);
            holder.tvDescription.setText(item.description);
            holder.ivImage.setImageResource(item.imageRes);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class GuideViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvDescription;
            ImageView ivImage;

            GuideViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvGuideTitle);
                tvDescription = itemView.findViewById(R.id.tvGuideDescription);
                ivImage = itemView.findViewById(R.id.ivGuideImage);
            }
        }
    }
}
