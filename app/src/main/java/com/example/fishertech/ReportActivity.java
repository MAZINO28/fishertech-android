package com.example.fishertech;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportActivity extends AppCompatActivity {

    private RadioGroup radioGroupCategory;
    private EditText etDescription, etAdditionalRemarks;
    private Button btnSubmitReport;
    private RecyclerView rvReportFeed;
    private BottomNavigationView bottomNav;
    private ImageView notification;

    private List<ReportPost> postList = new ArrayList<>();
    private ReportAdapter reportAdapter;

    private DatabaseReference dbRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference();

        radioGroupCategory = findViewById(R.id.radioGroupCategory);
        etDescription = findViewById(R.id.etDescription);
        etAdditionalRemarks = findViewById(R.id.etAdditionalRemarks);
        btnSubmitReport = findViewById(R.id.btnSubmitReport);
        rvReportFeed = findViewById(R.id.rvReportFeed);
        bottomNav = findViewById(R.id.bottomNav);
        notification = findViewById(R.id.notification);

        if (notification != null) {
            notification.setOnClickListener(v -> {
                Intent intent = new Intent(ReportActivity.this, NotificationActivity.class);
                startActivity(intent);
            });
        }

        rvReportFeed.setLayoutManager(new LinearLayoutManager(this));
        reportAdapter = new ReportAdapter(postList);
        rvReportFeed.setAdapter(reportAdapter);

        loadReportsFromFirebase();
        btnSubmitReport.setOnClickListener(v -> submitReportToFirebase());
        setupNavigation();
    }

    private void loadReportsFromFirebase() {
        dbRef.child("reports").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                postList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    String category = data.child("type").getValue(String.class);
                    String desc = data.child("description").getValue(String.class);
                    String remarks = data.child("additional_remarks").getValue(String.class);
                    String uid = data.child("uid").getValue(String.class);

                    if (uid != null) {
                        dbRef.child("FisherTech").child("Users").child(uid).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                String name = userSnapshot.exists() ? userSnapshot.getValue(String.class) : "Mangingisda";
                                postList.add(0, new ReportPost(name, category, desc, remarks));
                                reportAdapter.notifyDataSetChanged();
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {}
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ReportActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void submitReportToFirebase() {
        String desc = etDescription.getText().toString().trim();
        String remarks = etAdditionalRemarks.getText().toString().trim();
        int selectedId = radioGroupCategory.getCheckedRadioButtonId();

        if (selectedId == -1) {
            Toast.makeText(this, "Pumili muna ng kategorya", Toast.LENGTH_SHORT).show();
            return;
        }

        RadioButton selectedRadioButton = findViewById(selectedId);
        String cat = selectedRadioButton.getText().toString();

        if (desc.isEmpty()) {
            Toast.makeText(this, "Punan ang description", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String reportId = dbRef.child("reports").push().getKey();
            Map<String, Object> reportData = new HashMap<>();
            reportData.put("uid", user.getUid());
            reportData.put("type", cat);
            reportData.put("description", desc);
            reportData.put("additional_remarks", remarks);
            reportData.put("status", "received");
            reportData.put("created_at", System.currentTimeMillis());

            if (reportId != null) {
                dbRef.child("reports").child(reportId).setValue(reportData).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        etDescription.setText("");
                        etAdditionalRemarks.setText("");
                        radioGroupCategory.clearCheck();
                        Toast.makeText(this, "Ulat naipadala na!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    private void setupNavigation() {
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_report);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_report) {
                    return true;
                }

                Intent intent = null;
                if (id == R.id.nav_home) {
                    intent = new Intent(ReportActivity.this, DashboardActivity.class);
                } else if (id == R.id.nav_location) {
                    intent = new Intent(ReportActivity.this, LocationActivity.class);
                } else if (id == R.id.nav_chat) {
                    intent = new Intent(ReportActivity.this, ChatActivity.class);
                } else if (id == R.id.nav_profile) {
                    intent = new Intent(ReportActivity.this, ProfileActivity.class);
                }

                if (intent != null) {
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    finish();
                    return true;
                }
                return false;
            });
        }
    }

    public static class ReportPost {
        public String name, category, description, additionalRemarks;
        public ReportPost(String name, String category, String description, String additionalRemarks) {
            this.name = name;
            this.category = category;
            this.description = description;
            this.additionalRemarks = additionalRemarks;
        }
    }
}