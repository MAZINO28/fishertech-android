package com.example.fishertech;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class BuoyViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buoy_view);

        // 1. Hanapin ang mga buttons
        ImageButton btnBack = findViewById(R.id.btnBack);
        MaterialButton btnRefresh = findViewById(R.id.btnRefresh);

        // 2. Click para bumalik sa Dashboard
        btnBack.setOnClickListener(v -> {
            finish(); // Isasara nito itong page at babalik sa huling screen
        });

        // 3. Click para sa Refresh
        btnRefresh.setOnClickListener(v -> {
            Toast.makeText(BuoyViewActivity.this, "Updating capture...", Toast.LENGTH_SHORT).show();
            // Optional: Dito mo ilalagay yung code para mag-load ulit ang image
        });
    }
}