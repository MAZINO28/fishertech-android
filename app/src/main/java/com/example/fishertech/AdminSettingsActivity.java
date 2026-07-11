package com.example.fishertech;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class AdminSettingsActivity extends AppCompatActivity {

    Toolbar toolbar;

    SwitchMaterial switchWeather;

    TextView btnChangePassword;
    TextView btnHelp;
    TextView btnPrivacy;
    TextView btnAbout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_settings);

        // TOOLBAR
        toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {

            getSupportActionBar().setDisplayShowTitleEnabled(false);

            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // INITIALIZE VIEWS
        switchWeather = findViewById(R.id.switchWeather);

        btnChangePassword = findViewById(R.id.btnChangePassword);

        btnHelp = findViewById(R.id.btnHelp);

        btnPrivacy = findViewById(R.id.btnPrivacy);

        btnAbout = findViewById(R.id.btnAbout);

        // WEATHER ALERT SWITCH
        switchWeather.setOnCheckedChangeListener((buttonView, isChecked) -> {

            String status;

            if (isChecked) {
                status = "ON";
            } else {
                status = "OFF";
            }

            Toast.makeText(
                    this,
                    "Weather Alerts: " + status,
                    Toast.LENGTH_SHORT
            ).show();
        });

        // CHANGE PASSWORD
        btnChangePassword.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            AdminSettingsActivity.this,
                            AdminChangePasswordActivity.class
                    );

            startActivity(intent);
        });

        // HELP
        btnHelp.setOnClickListener(v -> {

            Toast.makeText(
                    this,
                    "Contact FisherTech Support",
                    Toast.LENGTH_SHORT
            ).show();
        });

        // PRIVACY POLICY
        btnPrivacy.setOnClickListener(v -> {

            Toast.makeText(
                    this,
                    "FisherTech protects user information and sensor data.",
                    Toast.LENGTH_LONG
            ).show();
        });

        // ABOUT
        btnAbout.setOnClickListener(v -> {

            Toast.makeText(
                    this,
                    "FisherTech v1.0.0\nDeveloped for fishermen safety and monitoring.",
                    Toast.LENGTH_LONG
            ).show();
        });
    }

    // BACK BUTTON
    @Override
    public boolean onSupportNavigateUp() {

        onBackPressed();

        return true;
    }
}