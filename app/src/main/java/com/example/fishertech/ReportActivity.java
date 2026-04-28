package com.example.fishertech;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView; // Import para sa notification bell
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.ArrayList;
import java.util.List;

public class ReportActivity extends AppCompatActivity {

    private Spinner spinnerCategory;
    private EditText etDescription;
    private Button btnSubmitReport;
    private RecyclerView rvReportFeed;
    private BottomNavigationView bottomNav;
    private ImageView notification;

    private static List<ReportPost> postList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        spinnerCategory = findViewById(R.id.spinnerCategory);
        etDescription = findViewById(R.id.etDescription);
        btnSubmitReport = findViewById(R.id.btnSubmitReport);
        rvReportFeed = findViewById(R.id.rvReportFeed);
        bottomNav = findViewById(R.id.bottomNav);

        notification = findViewById(R.id.notification);
        if (notification != null) {
            notification.setOnClickListener(v -> {
                Intent intent = new Intent(ReportActivity.this, NotificationActivity.class);
                startActivity(intent);
            });
        }

        String[] categories = {"Illegal Fishing", "Weather Alert", "Sea Rescue", "Equipment Lost"};
        spinnerCategory.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories));

        rvReportFeed.setLayoutManager(new LinearLayoutManager(this));
        updateFeed();

        btnSubmitReport.setOnClickListener(v -> {
            String desc = etDescription.getText().toString().trim();
            String cat = spinnerCategory.getSelectedItem().toString();

            if (!desc.isEmpty()) {
                postList.add(0, new ReportPost("Juan Dela Cruz", cat, desc));
                etDescription.setText("");
                updateFeed();
                Toast.makeText(this, "Nai-post na!", Toast.LENGTH_SHORT).show();
            }
        });

        setupNavigation();
    }

    private void updateFeed() {
        ReportAdapter adapter = new ReportAdapter(postList);
        rvReportFeed.setAdapter(adapter);
    }

    private void setupNavigation() {
        bottomNav.setSelectedItemId(R.id.nav_report);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_report) return true;

            Intent intent = null;
            if (id == R.id.nav_home) intent = new Intent(this, DashboardActivity.class);
            else if (id == R.id.nav_chat) intent = new Intent(this, ChatActivity.class);
            else if (id == R.id.nav_profile) intent = new Intent(this, ProfileActivity.class);

            if (intent != null) {
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }

    public static class ReportPost {
        String name, category, description;
        public ReportPost(String name, String category, String description) {
            this.name = name; this.category = category; this.description = description;
        }
    }
}