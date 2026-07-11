package com.example.fishertech;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class DashboardAdminActivity extends AppCompatActivity {

    private TextView tvMainTemp, tvWaterQuality, tvWaveHeight, tvWaterTemp;
    private TextView tvUserCount, tvReportCount, tvBuoyCount;
    private LinearLayout layoutActivityLog;

    private ImageView ivNotification;
    private BottomNavigationView bottomNav;
    private CardView cardBuoyPhoto, cardUsers, cardActiveBuoys, cardReports;;

    private DatabaseReference mDatabaseSensors;
    private DatabaseReference mDatabaseWeather;
    private DatabaseReference mRefUsers;
    private DatabaseReference mRefReports;
    private DatabaseReference mRefBuoys;
    private DatabaseReference mRefNotifications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_admin);

        initViews();

        mDatabaseSensors = FirebaseDatabase.getInstance().getReference("FisherTech/sensors");
        mDatabaseWeather = FirebaseDatabase.getInstance().getReference("FisherTech/weather_cache");
        mRefUsers = FirebaseDatabase.getInstance().getReference("FisherTech/Users");
        mRefReports = FirebaseDatabase.getInstance().getReference("reports");
        mRefBuoys = FirebaseDatabase.getInstance().getReference("FisherTech/boya_images");
        mRefNotifications = FirebaseDatabase.getInstance().getReference("FisherTech/notifications");

        fetchRealtimeData();
        setupNotification();
        setupCardClicks();
        setupNavigation();
    }

    private void initViews() {
        tvMainTemp = findViewById(R.id.tvMainTemp);
        tvWaveHeight = findViewById(R.id.tvWaveHeight);
        tvWaterQuality = findViewById(R.id.tvWaterQuality);
        tvWaterTemp = findViewById(R.id.tvWaterTemp);
        tvUserCount = findViewById(R.id.tvUserCount);
        tvReportCount = findViewById(R.id.tvReportCount);
        tvBuoyCount = findViewById(R.id.tvBuoyCount);

        layoutActivityLog = findViewById(R.id.layoutActivityLog);
        bottomNav = findViewById(R.id.bottomNav);
        cardBuoyPhoto = findViewById(R.id.cardBuoyPhoto);
        cardUsers = findViewById(R.id.cardUsers);
        cardActiveBuoys = findViewById(R.id.cardActiveBuoys);
        cardReports = findViewById(R.id.cardReports);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            ivNotification = toolbar.findViewById(R.id.notification);
        }
    }

    private void fetchRealtimeData() {
        mDatabaseSensors.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String height = snapshot.child("taas_tubig").exists() ? snapshot.child("taas_tubig").getValue().toString() : "--";
                    String quality = snapshot.child("kalidad_tubig").exists() ? snapshot.child("kalidad_tubig").getValue().toString() : "--";
                    String temp = snapshot.child("temperatura_tubig").exists() ? snapshot.child("temperatura_tubig").getValue().toString() : "--";

                    if (tvWaveHeight != null) tvWaveHeight.setText(height + " cm");
                    if (tvWaterQuality != null) tvWaterQuality.setText(quality);
                    if (tvWaterTemp != null) tvWaterTemp.setText(temp + "°C");
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        mDatabaseWeather.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && tvMainTemp != null) {
                    String mainTemp = snapshot.child("temperature").exists() ? snapshot.child("temperature").getValue().toString() : "--";
                    tvMainTemp.setText(mainTemp + "°");
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        mRefUsers.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (tvUserCount != null) tvUserCount.setText(String.valueOf(snapshot.getChildrenCount()));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        mRefReports.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (tvReportCount != null) {
                    long count = snapshot.getChildrenCount();
                    tvReportCount.setText(String.valueOf(count));
                    updateActivityLog(count);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        mRefBuoys.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (tvBuoyCount != null) tvBuoyCount.setText(String.valueOf(snapshot.getChildrenCount()));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateActivityLog(long reports) {
        if (layoutActivityLog != null) {
            layoutActivityLog.removeAllViews();
            TextView logEntry = new TextView(this);

            if (reports > 0) {
                logEntry.setText("Kasalukuyan: May " + reports + " aktibong ulat na nakatala sa system.");
                logEntry.setTextColor(ContextCompat.getColor(this, android.R.color.black));
            } else {
                logEntry.setText("Walang natanggap na bagong ulat mula sa dagat.");
                logEntry.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
            }

            logEntry.setPadding(20, 20, 20, 20);
            logEntry.setTextSize(13);
            layoutActivityLog.addView(logEntry);
        }
    }

    private void setupNotification() {
        if (ivNotification != null) {
            ivNotification.setOnClickListener(v -> {
                startActivity(new Intent(DashboardAdminActivity.this, NotificationActivity.class));
            });
        }
    }

    private void setupCardClicks() {
        if (cardActiveBuoys != null) {
            cardActiveBuoys.setOnClickListener(v -> {
                Intent intent = new Intent(DashboardAdminActivity.this, AdminLocationActivity.class);
                startActivity(intent);
            });
        }

        // Navigation para sa Reports (Ulat)
        if (cardReports != null) {
            cardReports.setOnClickListener(v -> {
                Intent intent = new Intent(DashboardAdminActivity.this, AdminReportsActivity.class);
                startActivity(intent);
            });
        }
        if (cardBuoyPhoto != null) {
            cardBuoyPhoto.setOnClickListener(v -> {
                startActivity(new Intent(DashboardAdminActivity.this, BuoyViewActivity.class));
            });
        }
        if (cardUsers != null) {
            cardUsers.setOnClickListener(v -> {
                startActivity(new Intent(DashboardAdminActivity.this, UserManagementActivity.class));
            });
        }
    }

    private void setupNavigation() {
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) return true;

                Intent intent = null;
                if (id == R.id.nav_location) intent = new Intent(this, AdminLocationActivity.class);
                else if (id == R.id.nav_chat) intent = new Intent(this, AdminChatActivity.class);
                else if (id == R.id.nav_report) intent = new Intent(this, AdminReportsActivity.class);
                else if (id == R.id.nav_profile) intent = new Intent(this, AdminProfileActivity.class);

                if (intent != null) {
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                }
                return false;
            });
        }
    }
}