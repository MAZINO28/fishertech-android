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
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DashboardAdminActivity extends AppCompatActivity {

    private TextView tvMainTemp, tvWaterQuality, tvWaveHeight, tvWaterTemp, tvCondition, tvDate;
    private ImageView ivWeatherIcon; // Idinagdag para sa dynamic weather icon
    private TextView tvUserCount, tvReportCount, tvBuoyCount;
    private LinearLayout layoutActivityLog;

    private ImageView ivNotification;
    private BottomNavigationView bottomNav;
    private CardView cardBuoyPhoto, cardUsers, cardActiveBuoys, cardReports;

    private DatabaseReference mDatabaseSensors;
    private DatabaseReference mRefUsers;
    private DatabaseReference mRefReports;
    private DatabaseReference mRefBuoys;

    // OpenWeatherMap API Configuration
    private final String API_KEY = "1cb64f7a9c1182e1511454b51eb047e9";
    private final String LAT = "14.4506"; // Bacoor Bay, Cavite Latitude
    private final String LON = "120.9358"; // Bacoor Bay, Cavite Longitude

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_admin);

        initViews();
        setCurrentDate();

        mDatabaseSensors = FirebaseDatabase.getInstance().getReference("FisherTech/sensors");
        mRefUsers = FirebaseDatabase.getInstance().getReference("FisherTech/Users");
        mRefReports = FirebaseDatabase.getInstance().getReference("reports");
        mRefBuoys = FirebaseDatabase.getInstance().getReference("FisherTech/boya_images");

        fetchRealtimeData();
        fetchLiveWeather();
        setupNotification();
        setupCardClicks();
        setupNavigation();
    }

    private void initViews() {
        tvMainTemp = findViewById(R.id.tvMainTemp);
        tvWaveHeight = findViewById(R.id.tvWaveHeight);
        tvWaterQuality = findViewById(R.id.tvWaterQuality);
        tvWaterTemp = findViewById(R.id.tvWaterTemp);
        tvCondition = findViewById(R.id.tvCondition);
        tvDate = findViewById(R.id.tvDate);
        ivWeatherIcon = findViewById(R.id.ivWeatherIcon); // Naka-link na ang weather icon ID
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

    private void setCurrentDate() {
        if (tvDate != null) {
            String currentDate = new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(new Date());
            tvDate.setText(currentDate);
        }
    }

    private void fetchLiveWeather() {
        String url = "https://api.openweathermap.org/data/2.5/weather?lat=" + LAT + "&lon=" + LON + "&appid=" + API_KEY + "&units=metric&lang=fil";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject main = response.getJSONObject("main");
                        double temp = main.getDouble("temp");
                        double tempMax = main.getDouble("temp_max");
                        double tempMin = main.getDouble("temp_min");

                        String weatherDesc = response.getJSONArray("weather")
                                .getJSONObject(0).getString("description");

                        // Kunin ang icon code para sa dynamic icon updating
                        String iconCode = response.getJSONArray("weather")
                                .getJSONObject(0).getString("icon");
                        setWeatherIcon(iconCode);

                        if (!weatherDesc.isEmpty()) {
                            weatherDesc = weatherDesc.substring(0, 1).toUpperCase() + weatherDesc.substring(1);
                        }

                        if (tvMainTemp != null) {
                            tvMainTemp.setText(Math.round(temp) + "°");
                        }
                        if (tvCondition != null) {
                            tvCondition.setText(weatherDesc + " • H:" + Math.round(tempMax) + "° L:" + Math.round(tempMin) + "°");
                        }

                    } catch (JSONException e) {
                        Log.e("WeatherError", "JSON parsing error: " + e.getMessage());
                    }
                },
                error -> Log.e("WeatherError", "Volley error: " + error.getMessage())
        );

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(jsonObjectRequest);
    }

    private void setWeatherIcon(String iconCode) {
        if (ivWeatherIcon == null) return;

        switch (iconCode) {
            // Sunny / Clear
            case "01d":
            case "01n":
                ivWeatherIcon.setImageResource(R.drawable.sunny);
                break;

            // Cloudy / Few clouds / Scattered clouds
            case "02d":
            case "02n":
            case "03d":
            case "03n":
            case "04d":
            case "04n":
                ivWeatherIcon.setImageResource(R.drawable.cloudy);
                break;

            // Rainy / Shower rain
            case "09d":
            case "09n":
            case "10d":
            case "10n":
                ivWeatherIcon.setImageResource(R.drawable.rainy);
                break;

            // Thunderstorm (Bagyo)
            case "11d":
            case "11n":
                ivWeatherIcon.setImageResource(R.drawable.thunderstorm); // Siguraduhing mayroon kang ganitong icon
                break;

            // Mist / Fog / Haze
            case "50d":
            case "50n":
                ivWeatherIcon.setImageResource(R.drawable.foggy); // O kaya ay sunny/cloudy kung wala pa
                break;

            default:
                ivWeatherIcon.setImageResource(R.drawable.sunny);
                break;
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