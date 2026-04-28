package com.example.fishertech;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView; // Import para sa notification icon
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class ChatActivity extends AppCompatActivity {

    Toolbar toolbar;
    BottomNavigationView bottomNav;
    RecyclerView rvForumPosts;
    FloatingActionButton fabAddPost;
    ImageView notification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        rvForumPosts = findViewById(R.id.rvForumPosts);
        rvForumPosts.setLayoutManager(new LinearLayoutManager(this));

        fabAddPost = findViewById(R.id.fabAddPost);
        fabAddPost.setOnClickListener(v -> showPostDialog());

        notification = findViewById(R.id.notification);
        if (notification != null) {
            notification.setOnClickListener(v -> {
                Intent intent = new Intent(ChatActivity.this, NotificationActivity.class);
                startActivity(intent);
            });
        }

        bottomNav = findViewById(R.id.bottomNav);
        setupNavigation();
    }

    private void showPostDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Bagong Post sa Forum");
        final EditText input = new EditText(this);
        input.setHint("Ano ang balita sa dagat?");
        builder.setView(input);
        builder.setPositiveButton("I-Post", (dialog, which) -> {
            String message = input.getText().toString();
            if (!message.isEmpty()) {
                Toast.makeText(this, "Naipasa na ang iyong post!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("I-Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void setupNavigation() {
        bottomNav.setSelectedItemId(R.id.nav_chat);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_chat) return true;

            Intent intent = null;
            if (id == R.id.nav_home) intent = new Intent(this, DashboardActivity.class);
            else if (id == R.id.nav_location) intent = new Intent(this, LocationActivity.class);
            else if (id == R.id.nav_report) intent = new Intent(this, ReportActivity.class);
            else if (id == R.id.nav_profile) intent = new Intent(this, ProfileActivity.class);

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