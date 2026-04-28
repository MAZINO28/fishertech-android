package com.example.fishertech;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AdminActivity extends AppCompatActivity {

    private TextView tvSensorData;
    private Button btnLogout;
    private DatabaseReference sensorRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // 1. Initialize Views
        tvSensorData = findViewById(R.id.tvSensorData);
        btnLogout = findViewById(R.id.btnLogout);

        // 2. Reference sa "sensors" node ng FisherTech
        sensorRef = FirebaseDatabase.getInstance().getReference("FisherTech/sensors");

        // 3. Real-time Listener (Dito kumukuha ng data)
        sensorRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    StringBuilder dataBuilder = new StringBuilder();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        // Kukunin ang Key (hal. temp) at Value (hal. 25)
                        String key = child.getKey();
                        String value = String.valueOf(child.getValue());
                        dataBuilder.append(key).append(": ").append(value).append("\n");
                    }
                    tvSensorData.setText(dataBuilder.toString());
                } else {
                    tvSensorData.setText("No sensors found in database.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // 4. Logout Logic
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(AdminActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }
}