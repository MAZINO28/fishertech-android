package com.example.fishertech;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private Toolbar toolbar;
    private NotifAdapter adapter;
    private List<NotifModel> notifList;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        mDatabase = FirebaseDatabase.getInstance().getReference("notifications");

        setupToolbar();
        setupRecyclerView();
        loadNotifications();
    }

    private void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupRecyclerView() {
        rvNotifications = findViewById(R.id.rvNotifications);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        notifList = new ArrayList<>();
        adapter = new NotifAdapter(notifList);
        rvNotifications.setAdapter(adapter);
    }

    private void loadNotifications() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                notifList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    NotifModel model = data.getValue(NotifModel.class);
                    if (model != null) {
                        notifList.add(model);
                    }
                }

                Collections.reverse(notifList);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(NotificationActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }


    public static class NotifModel {
        public String title, message, type, target;
        public long created_at; // Ginawang long para swak sa timestamp ng Firebase mo

        public NotifModel() {} // Required para sa Firebase

        public NotifModel(String title, String message, long created_at, String type, String target) {
            this.title = title;
            this.message = message;
            this.created_at = created_at;
            this.type = type;
            this.target = target;
        }

        // Isang function para i-convert ang numerong timestamp papuntang totoong oras (e.g., "11:49 AM | 08 Jun")
        public String getFormattedTime() {
            if (created_at == 0) return "";
            try {
                java.util.Date date = new java.util.Date(created_at);
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("hh:mm a | dd MMM", java.util.Locale.getDefault());
                return sdf.format(date);
            } catch (Exception e) {
                return "";
            }
        }
    }


    class NotifAdapter extends RecyclerView.Adapter<NotifAdapter.VH> {
        private final List<NotifModel> data;

        NotifAdapter(List<NotifModel> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
            View view = LayoutInflater.from(p.getContext()).inflate(R.layout.item_notification, p, false);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int p) {
            NotifModel model = data.get(p);

            h.tvTitle.setText(model.title);     // Lalabas ang dynamic title tulad ng "May Napansin na Sasakyang Pandagat!"
            h.tvMsg.setText(model.message);     // Lalabas ang dynamic message na may kasamang numero ng Boya (1, 2, o 3)
            h.tvTime.setText(model.getFormattedTime()); // Lalabas ang tamang oras base sa timestamp
            h.ivIcon.setImageResource(R.drawable.notification); // Standard icon ng notification mo
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvMsg, tvTime;
            ImageView ivIcon;
            VH(View v) {
                super(v);
                tvTitle = v.findViewById(R.id.tvNotifTitle);
                tvMsg = v.findViewById(R.id.tvNotifMessage);
                tvTime = v.findViewById(R.id.tvNotifTime);
                ivIcon = v.findViewById(R.id.ivNotifIcon);
            }
        }
    }
}