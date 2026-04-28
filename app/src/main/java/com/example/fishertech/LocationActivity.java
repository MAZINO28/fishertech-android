package com.example.fishertech;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView; // Idinagdag para sa notification icon
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class LocationActivity extends AppCompatActivity {

    Toolbar toolbar;
    BottomNavigationView bottomNav;
    TextView tvLatitude, tvLongitude;
    Button btnMarkLocation;
    ImageView notification; // Idinagdag na variable

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        // Toolbar setup
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Initialize Views
        tvLatitude = findViewById(R.id.tvLatitude);
        tvLongitude = findViewById(R.id.tvLongitude);
        btnMarkLocation = findViewById(R.id.btnMarkLocation);

        // 1. Hanapin ang notification icon sa toolbar
        notification = findViewById(R.id.notification);

        // 2. Click logic para sa notification
        if (notification != null) {
            notification.setOnClickListener(v -> {
                Intent intent = new Intent(LocationActivity.this, NotificationActivity.class);
                startActivity(intent);
            });
        }

        btnMarkLocation.setOnClickListener(v -> {
            Toast.makeText(this, "Lokasyon ay na-save sa system.", Toast.LENGTH_SHORT).show();
        });

        setupNavigation();
    }

    private void setupNavigation() {
        bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_location);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_location) return true;

                Intent intent = null;
                if (id == R.id.nav_home) intent = new Intent(this, DashboardActivity.class);
                else if (id == R.id.nav_chat) intent = new Intent(this, ChatActivity.class);
                else if (id == R.id.nav_report) intent = new Intent(this, ReportActivity.class);
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
    }
}