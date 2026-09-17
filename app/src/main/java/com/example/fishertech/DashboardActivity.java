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
    private ImageView ivWeatherIcon; // Idinagdag para sa dynamic weather icon
    private BottomNavigationView bottomNav;
    private CardView cardBuoyPhoto;
    private LinearLayout btnNotification;

    private DatabaseReference mDatabaseSensors;

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

        fetchRealtimeData();
        fetchLiveWeather();

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
        ivWeatherIcon = findViewById(R.id.ivWeatherIcon); // Naka-link na ang weather icon ID
        cardBuoyPhoto = findViewById(R.id.cardBuoyPhoto);
        bottomNav = findViewById(R.id.bottomNav);
        btnNotification = findViewById(R.id.btnNotificationContainer);
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
                ivWeatherIcon.setImageResource(R.drawable.thunderstorm);
                break;

            // Mist / Fog / Haze
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