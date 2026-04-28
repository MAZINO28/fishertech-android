package com.example.fishertech;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsActivity extends AppCompatActivity {

    Toolbar toolbar;
    SwitchMaterial switchWeather, switchSMS;
    TextView btnChangePassword, btnLanguage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Back arrow sa taas
        }

        switchWeather = findViewById(R.id.switchWeather);
        switchSMS = findViewById(R.id.switchSMS);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnLanguage = findViewById(R.id.btnLanguage);

        switchWeather.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String status = isChecked ? "Naka-ON" : "Naka-OFF";
            Toast.makeText(this, "Weather Alerts: " + status, Toast.LENGTH_SHORT).show();
        });

        switchSMS.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String status = isChecked ? "Naka-ON" : "Naka-OFF";
            Toast.makeText(this, "SMS Alerts: " + status, Toast.LENGTH_SHORT).show();
        });

        btnChangePassword.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, ChangePasswordActivity.class);
            startActivity(intent);
        });

        btnLanguage.setOnClickListener(v -> {
            Toast.makeText(this, "Pumili ng Wika (Tagalog/English)", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}