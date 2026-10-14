package com.example.fishertech;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private Toolbar toolbar;
    private NotifAdapter adapter;
    private List<NotifModel> notifList;
    private DatabaseReference mDatabase;
    private SharedPreferences prefs;
    private boolean isInitialLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        prefs = getSharedPreferences("FisherTechPrefs", MODE_PRIVATE);
        mDatabase = FirebaseDatabase.getInstance().getReference().child("FisherTech").child("notifications");

        setupToolbar();
        setupRecyclerView();

        // Subaybayan ang galaw at estado ng mga boya batay sa Concept Checklist
        monitorBuoyAlerts();

        // Makinig sa live-triggered notifications
        listenToLiveNotifications();
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

    private void listenToLiveNotifications() {
        notifList.clear();
        isInitialLoad = true;

        mDatabase.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (isInitialLoad) {
                    return; // Magsimula sa blangko pagbukas ng screen
                }

                NotifModel model = snapshot.getValue(NotifModel.class);
                if (model != null) {
                    notifList.add(0, model);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(NotificationActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isInitialLoad = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                isInitialLoad = false;
            }
        });
    }

    // Naka-focus lang sa Checklist: Unusual movement at possible buoy theft
    private void monitorBuoyAlerts() {
        DatabaseReference buoysRef = FirebaseDatabase.getInstance().getReference().child("FisherTech").child("buoys");

        buoysRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot buoySnap : snapshot.getChildren()) {
                    String buoyId = buoySnap.getKey();

                    Boolean isUnauthorized = buoySnap.child("is_unauthorized_movement").getValue(Boolean.class);
                    Boolean isPossibleTheft = buoySnap.child("is_possible_theft").getValue(Boolean.class);
                    String statusMessage = buoySnap.child("status_message").getValue(String.class);

                    // 1. Detection of unusual or unauthorized buoy movement
                    if (Boolean.TRUE.equals(isUnauthorized)) {
                        String lastNotif = prefs.getString("unauth_" + buoyId, "");
                        String currentMsg = (statusMessage != null) ? statusMessage : "Kakaibang galaw ang natukoy sa " + buoyId;

                        if (!lastNotif.equals(currentMsg)) {
                            prefs.edit().putString("unauth_" + buoyId, currentMsg).apply();
                            postNotificationInternal("BABALA: Kakaibang Galaw ng Boya!", currentMsg, "buoy_movement");
                        }
                    }

                    // 2. Location alert notification for possible buoy theft
                    if (Boolean.TRUE.equals(isPossibleTheft)) {
                        String lastTheftNotif = prefs.getString("theft_" + buoyId, "");
                        String theftMsg = (statusMessage != null) ? statusMessage : "Posibleng pagnanakaw o paglipat ng lokasyon sa " + buoyId;

                        if (!lastTheftNotif.equals(theftMsg)) {
                            prefs.edit().putString("theft_" + buoyId, theftMsg).apply();
                            postNotificationInternal("ALERTO: Posibleng Pagnanakaw ng Boya!", theftMsg, "buoy_theft");
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void postNotificationInternal(String title, String message, String type) {
        String pushId = mDatabase.push().getKey();
        if (pushId != null) {
            Map<String, Object> map = new HashMap<>();
            map.put("title", title);
            map.put("message", message);
            map.put("created_at", System.currentTimeMillis());
            map.put("type", type);
            map.put("target", "all");
            mDatabase.child(pushId).setValue(map);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public static class NotifModel {
        public String title, message, type, target;
        public long created_at;

        public NotifModel() {}

        public NotifModel(String title, String message, long created_at, String type, String target) {
            this.title = title;
            this.message = message;
            this.created_at = created_at;
            this.type = type;
            this.target = target;
        }

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

            h.tvTitle.setText(model.title);
            h.tvMsg.setText(model.message);
            h.tvTime.setText(model.getFormattedTime());
            h.ivIcon.setImageResource(R.drawable.notification);
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