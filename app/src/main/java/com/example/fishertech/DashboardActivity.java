package com.example.fishertech;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {

    private TextView tvMainTemp, tvWaterQuality, tvWaveHeight, tvWaterTemp, tvCondition, tvDate;
    private TextView tvReportUserName, tvReportCategory, tvAnnouncementMessage, tvReportAdditional, tvAnnouncementTime;
    private ImageView ivWeatherIcon;
    private BottomNavigationView bottomNav;
    private CardView cardBuoyPhoto, cardRecentAnnouncement;
    private LinearLayout btnNotification;

    private DatabaseReference mDatabaseSensors, mDatabaseReports;

    // OpenWeatherMap API Configuration
    private final String API_KEY = "1cb64f7a9c1182e1511454b51eb047e9";
    private final String LAT = "14.4506"; // Bacoor Bay, Cavite Latitude
    private final String LON = "120.9358"; // Bacoor Bay, Cavite Longitude

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        initViews();
        setCurrentDate();

        mDatabaseSensors = FirebaseDatabase.getInstance().getReference("FisherTech/sensors");
        mDatabaseReports = FirebaseDatabase.getInstance().getReference("reports");

        fetchRealtimeData();
        fetchLiveWeather();
        fetchLatestAnnouncement(); // Kunin ang pinakabagong ulat

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
        tvWaveHeight = findViewById(R.id.tvWaveHeight);
        tvWaterQuality = findViewById(R.id.tvWindSpeed);
        tvWaterTemp = findViewById(R.id.tvWaterTemp);
        tvCondition = findViewById(R.id.tvCondition);
        tvDate = findViewById(R.id.tvDate);

        // Recent Report Card Views
        tvReportUserName = findViewById(R.id.tvReportUserName);
        tvReportCategory = findViewById(R.id.tvReportCategory);
        tvAnnouncementMessage = findViewById(R.id.tvAnnouncementMessage);
        tvReportAdditional = findViewById(R.id.tvReportAdditional);
        tvAnnouncementTime = findViewById(R.id.tvAnnouncementTime);

        ivWeatherIcon = findViewById(R.id.ivWeatherIcon);
        cardBuoyPhoto = findViewById(R.id.cardBuoyPhoto);
        cardRecentAnnouncement = findViewById(R.id.cardRecentAnnouncement);
        bottomNav = findViewById(R.id.bottomNav);
        btnNotification = findViewById(R.id.btnNotificationContainer);
    }

    private void setCurrentDate() {
        if (tvDate != null) {
            String currentDate = new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(new Date());
            tvDate.setText(currentDate);
        }
    }

    private void fetchLatestAnnouncement() {
        mDatabaseReports.orderByChild("created_at").limitToLast(1).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot snap : snapshot.getChildren()) {
                        String uid = snap.child("uid").getValue(String.class);

                        // Kunin ang 'type' (o 'category' kung sakaling meron)
                        String category = snap.child("type").getValue(String.class);
                        if (category == null) category = snap.child("category").getValue(String.class);

                        String description = snap.child("description").getValue(String.class);

                        // Tamang pangalan ng field mula sa Firebase: 'additional_remarks'
                        String additionalRemarks = snap.child("additional_remarks").getValue(String.class);
                        if (additionalRemarks == null) additionalRemarks = snap.child("additional").getValue(String.class);

                        Long createdAt = snap.child("created_at").getValue(Long.class);

                        if (tvReportCategory != null) {
                            tvReportCategory.setText(category != null ? category : "Ulat sa Dagat");
                        }
                        if (tvAnnouncementMessage != null) {
                            tvAnnouncementMessage.setText(description != null ? description : "Walang detalye.");
                        }

                        // I-display ang karagdagang puna (additional_remarks)
                        if (tvReportAdditional != null) {
                            if (additionalRemarks != null && !additionalRemarks.isEmpty()) {
                                tvReportAdditional.setText(additionalRemarks);
                                tvReportAdditional.setVisibility(View.VISIBLE);
                            } else {
                                tvReportAdditional.setVisibility(View.GONE);
                            }
                        }

                        // Oras
                        if (tvAnnouncementTime != null && createdAt != null) {
                            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
                            tvAnnouncementTime.setText(sdf.format(new Date(createdAt)));
                        } else if (tvAnnouncementTime != null) {
                            tvAnnouncementTime.setText("Kamakailan lang");
                        }

                        // Kunin ang pangalan ng user gamit ang UID
                        if (uid != null) {
                            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("FisherTech").child("Users").child(uid).child("name");
                            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot userSnap) {
                                    String name = userSnap.exists() ? userSnap.getValue(String.class) : "Mangingisda";
                                    if (tvReportUserName != null) {
                                        tvReportUserName.setText(name);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    if (tvReportUserName != null) tvReportUserName.setText("Mangingisda");
                                }
                            });
                        } else {
                            if (tvReportUserName != null) tvReportUserName.setText("Mangingisda");
                        }
                    }
                } else {
                    if (tvReportUserName != null) tvReportUserName.setText("Walang Ulat");
                    if (tvReportCategory != null) tvReportCategory.setText("");
                    if (tvAnnouncementMessage != null) tvAnnouncementMessage.setText("Wala pang nai-post na ulat sa kasalukuyan.");
                    if (tvReportAdditional != null) tvReportAdditional.setVisibility(View.GONE);
                    if (tvAnnouncementTime != null) tvAnnouncementTime.setText("");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (tvAnnouncementMessage != null) {
                    tvAnnouncementMessage.setText("Nabigong i-load ang ulat.");
                }
            }
        });
    }

    private void fetchLiveWeather() {
        String url = "https://api.openweathermap.org/data/2.5/weather?lat=" + LAT + "&lon=" + LON + "&appid=" + API_KEY + "&units=metric&lang=en";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject main = response.getJSONObject("main");
                        double temp = main.getDouble("temp");
                        double tempMax = main.getDouble("temp_max");
                        double tempMin = main.getDouble("temp_min");

                        String weatherDesc = response.getJSONArray("weather")
                                .getJSONObject(0).getString("description");

                        String iconCode = response.getJSONArray("weather")
                                .getJSONObject(0).getString("icon");
                        setWeatherIcon(iconCode);

                        String lowerDesc = weatherDesc.toLowerCase();
                        if (lowerDesc.contains("light rain")) {
                            weatherDesc = "Mahinang ulan";
                        } else if (lowerDesc.contains("moderate rain")) {
                            weatherDesc = "Katamtamang ulan";
                        } else if (lowerDesc.contains("heavy intensity rain") || lowerDesc.contains("heavy rain")) {
                            weatherDesc = "Malakas na ulan";
                        } else if (lowerDesc.contains("clear sky")) {
                            weatherDesc = "Maliwanag";
                        } else if (lowerDesc.contains("few clouds") || lowerDesc.contains("scattered clouds") || lowerDesc.contains("broken clouds") || lowerDesc.contains("overcast clouds")) {
                            weatherDesc = "Maulap";
                        } else if (lowerDesc.contains("thunderstorm")) {
                            weatherDesc = "Bagyo / Pagkidlat";
                        } else {
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
            case "01d":
            case "01n":
                ivWeatherIcon.setImageResource(R.drawable.sunny);
                break;
            case "02d":
            case "02n":
            case "03d":
            case "03n":
            case "04d":
            case "04n":
                ivWeatherIcon.setImageResource(R.drawable.cloudy);
                break;
            case "09d":
            case "09n":
            case "10d":
            case "10n":
                ivWeatherIcon.setImageResource(R.drawable.rainy);
                break;
            case "11d":
            case "11n":
                ivWeatherIcon.setImageResource(R.drawable.thunderstorm);
                break;
            case "50d":
            case "50n":
                ivWeatherIcon.setImageResource(R.drawable.foggy);
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
                    try {
                        Object heightVal = snapshot.child("taas_tubig").getValue();
                        Object qualityVal = snapshot.child("kalidad_tubig").getValue();
                        Object tempVal = snapshot.child("temperatura_tubig").getValue();

                        String height = (heightVal != null) ? heightVal.toString() : "--";
                        String quality = (qualityVal != null) ? qualityVal.toString() : "--";
                        String temp = (tempVal != null) ? tempVal.toString() : "--";

                        if (tvWaveHeight != null) tvWaveHeight.setText(height + " cm");
                        if (tvWaterQuality != null) tvWaterQuality.setText(quality);
                        if (tvWaterTemp != null) tvWaterTemp.setText(temp + "°C");
                    } catch (Exception e) {
                        Log.e("FirebaseError", "Error: " + e.getMessage());
                    }
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