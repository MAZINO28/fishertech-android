package com.example.fishertech;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
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
    private LinearLayout btnNotification;

    private DatabaseReference mDatabaseSensors;
    private DatabaseReference mDatabaseWeather;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        initViews();

        mDatabaseSensors = FirebaseDatabase.getInstance().getReference("FisherTech/sensors");
        mDatabaseWeather = FirebaseDatabase.getInstance().getReference("FisherTech/weather_cache");

        fetchRealtimeData();


        if (cardBuoyPhoto != null) {
            cardBuoyPhoto.setOnClickListener(v -> {
                startActivity(new Intent(DashboardActivity.this, BuoyViewActivity.class));
            });
        }

        if (btnNotification != null) {
            btnNotification.setOnClickListener(v -> {
                startActivity(new Intent(DashboardActivity.this, NotificationActivity.class));
            });
        }

        setupNavigation();
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    private void initViews() {
        tvMainTemp = findViewById(R.id.tvMainTemp);

        tvWaveHeight    = findViewById(R.id.tvWaveHeight);
        tvWaterQuality  = findViewById(R.id.tvWindSpeed);
        tvWaterTemp     = findViewById(R.id.tvWaterTemp);
        cardBuoyPhoto   = findViewById(R.id.cardBuoyPhoto);
        bottomNav       = findViewById(R.id.bottomNav);


        btnNotification = findViewById(R.id.btnNotificationContainer);
    }

    private void fetchRealtimeData() {
        mDatabaseSensors.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    try {
                        Object heightVal  = snapshot.child("taas_tubig").getValue();
                        Object qualityVal = snapshot.child("kalidad_tubig").getValue();
                        Object tempVal    = snapshot.child("temperatura_tubig").getValue();

                        String height  = (heightVal != null) ? heightVal.toString() : "--";
                        String quality = (qualityVal != null) ? qualityVal.toString() : "--";
                        String temp    = (tempVal != null) ? tempVal.toString() : "--";

                        tvWaveHeight.setText(height + " cm");
                        tvWaterQuality.setText(quality);
                        tvWaterTemp.setText(temp + "°C");
                    } catch (Exception e) {
                        Log.e("FirebaseError", "Error: " + e.getMessage());
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        mDatabaseWeather.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Object mainTempVal = snapshot.child("temperature").getValue();
                    String mainTemp = (mainTempVal != null) ? mainTempVal.toString() : "--";
                    tvMainTemp.setText(mainTemp + "°");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupNavigation() {
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) return true;

                Intent intent = null;
                if (id == R.id.nav_location) intent = new Intent(this, LocationActivity.class);
                else if (id == R.id.nav_chat) intent = new Intent(this, ChatActivity.class);
                else if (id == R.id.nav_report) intent = new Intent(this, ReportActivity.class);
                else if (id == R.id.nav_profile) intent = new Intent(this, ProfileActivity.class);

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