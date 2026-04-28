package com.example.fishertech;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class DashboardActivity extends AppCompatActivity {

    private TextView tvMainTemp, tvWaterQuality, tvWaveHeight, tvWaterTemp;
    private BottomNavigationView bottomNav;
    private CardView cardBuoyPhoto;
    private ImageView notification;

    // Database reference para sa Firebase
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        initViews();

        // 1. I-initialize ang Firebase Database
        mDatabase = FirebaseDatabase.getInstance().getReference("FisherTech/sensors");

        // 2. Tawagin ang function para sa real-time updates
        fetchRealtimeData();

        if (cardBuoyPhoto != null) {
            cardBuoyPhoto.setOnClickListener(v -> {
                Intent intent = new Intent(DashboardActivity.this, BuoyViewActivity.class);
                startActivity(intent);
            });
        }

        if (notification != null) {
            notification.setOnClickListener(v -> {
                Intent intent = new Intent(DashboardActivity.this, NotificationActivity.class);
                startActivity(intent);
            });
        }

        setupNavigation();
    }

    private void initViews() {
        tvMainTemp = findViewById(R.id.tvTemperature);
        tvWaveHeight = findViewById(R.id.tvWaveHeight); // Taas ng tubig
        tvWaterQuality = findViewById(R.id.tvWindSpeed); // Kalidad ng tubig
        tvWaterTemp = findViewById(R.id.tvHumidity);    // Temperatura ng tubig
        cardBuoyPhoto = findViewById(R.id.cardBuoyPhoto);
        bottomNav = findViewById(R.id.bottomNav);
        notification = findViewById(R.id.notification);
    }

    private void fetchRealtimeData() {
        // Siguraduhin na ang path ay tugma sa Firebase Console mo
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    try {
                        // Mas safe na kunin muna bilang Object para iwas NullPointerException
                        Object heightVal = snapshot.child("taas_tubig").getValue();
                        Object qualityVal = snapshot.child("kalidad_tubig").getValue();
                        Object tempVal = snapshot.child("temperatura_tubig").getValue();

                        // I-convert sa String; kung null ang data, gamitin ang "--"
                        String height = (heightVal != null) ? heightVal.toString() : "--";
                        String quality = (qualityVal != null) ? qualityVal.toString() : "--";
                        String temp = (tempVal != null) ? tempVal.toString() : "--";

                        // I-update ang UI nang live
                        tvWaveHeight.setText(height + " m");
                        tvWaterQuality.setText(quality);
                        tvWaterTemp.setText(temp + "°C");
                        tvMainTemp.setText(temp + "°");

                    } catch (Exception e) {
                        Log.e("FirebaseError", "Error parsing data: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Database error: " + error.getMessage());
            }
        });
    }

    private void setupNavigation() {
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) return true;

                Intent intent = null;
                if (id == R.id.nav_location) {
                    intent = new Intent(this, LocationActivity.class);
                } else if (id == R.id.nav_chat) {
                    intent = new Intent(this, ChatActivity.class);
                } else if (id == R.id.nav_report) {
                    intent = new Intent(this, ReportActivity.class);
                } else if (id == R.id.nav_profile) {
                    intent = new Intent(this, ProfileActivity.class);
                }

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