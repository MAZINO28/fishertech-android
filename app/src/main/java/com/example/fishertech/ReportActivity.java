package com.example.fishertech;

import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
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

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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

        // Ipinasa ang callback para sa save button
        reportAdapter = new ReportAdapter(postList, this::saveReportAsTxtFile);
        rvReportFeed.setAdapter(reportAdapter);

        loadReportsFromFirebase();
        btnSubmitReport.setOnClickListener(v -> submitReportToFirebase());
        setupNavigation();
    }

    private void saveReportAsTxtFile(ReportPost post) {
        String fileName = "FisherTech_Report_" + System.currentTimeMillis() + ".txt";
        String fileContent = "=== FISHERTECH REPORT ===\n" +
                "Pangalan ng Nag-ulat: " + post.name + "\n" +
                "Kategorya: " + post.category + "\n" +
                "Oras/Petsa: " + post.timestamp + "\n\n" +
                "Deskripsyon:\n" + post.description + "\n\n" +
                "Karagdagang Puna:\n" + (post.additionalRemarks != null && !post.additionalRemarks.isEmpty() ? post.additionalRemarks : "Wala") + "\n" +
                "=========================";

        boolean isSaved = false;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Para sa Android 10 pataas (paggamit ng MediaStore)
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                        if (outputStream != null) {
                            outputStream.write(fileContent.getBytes());
                            isSaved = true;
                        }
                    }
                }
            } else {
                // Para sa mas lumang bersyon ng Android
                java.io.File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs();
                }
                java.io.File file = new java.io.File(downloadsDir, fileName);
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
                    fos.write(fileContent.getBytes());
                    isSaved = true;
                }
            }

            if (isSaved) {
                Toast.makeText(this, "Tagumpay na nai-save ang ulat sa Downloads folder!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Hindi nai-save ang ulat.", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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
                    Long createdAt = data.child("created_at").getValue(Long.class);

                    String timeString = "Kamakailan lang";
                    if (createdAt != null) {
                        long currentTime = System.currentTimeMillis();
                        long timeDifference = currentTime - createdAt;
                        long oneDayMillis = 24 * 60 * 60 * 1000;

                        Calendar postDate = Calendar.getInstance();
                        postDate.setTimeInMillis(createdAt);

                        Calendar currentDate = Calendar.getInstance();
                        currentDate.setTimeInMillis(currentTime);

                        boolean isSameDay = postDate.get(Calendar.YEAR) == currentDate.get(Calendar.YEAR) &&
                                postDate.get(Calendar.DAY_OF_YEAR) == currentDate.get(Calendar.DAY_OF_YEAR);

                        if (isSameDay) {
                            SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
                            timeString = timeFormat.format(new Date(createdAt));
                        } else if (timeDifference < (7 * oneDayMillis)) {
                            SimpleDateFormat dayFormat = new SimpleDateFormat("EEE, h:mm a", Locale.getDefault());
                            timeString = dayFormat.format(new Date(createdAt));
                        } else {
                            SimpleDateFormat fullFormat = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());
                            timeString = fullFormat.format(new Date(createdAt));
                        }
                    }

                    final String finalTime = timeString;

                    if (uid != null) {
                        dbRef.child("FisherTech").child("Users").child(uid).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                String name = userSnapshot.exists() ? userSnapshot.getValue(String.class) : "Mangingisda";
                                postList.add(0, new ReportPost(name, category, desc, remarks, finalTime));
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
        public String name, category, description, additionalRemarks, timestamp;
        public ReportPost(String name, String category, String description, String additionalRemarks, String timestamp) {
            this.name = name;
            this.category = category;
            this.description = description;
            this.additionalRemarks = additionalRemarks;
            this.timestamp = timestamp;
        }
    }
}