package com.example.fishertech;

import android.content.Intent;
import android.content.SharedPreferences;
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

import com.google.firebase.auth.FirebaseAuth;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        prefs = getSharedPreferences("FisherTechPrefs", MODE_PRIVATE);
        mDatabase = FirebaseDatabase.getInstance().getReference().child("FisherTech").child("notifications");

        prefs.edit().putLong("last_seen_notification", System.currentTimeMillis()).apply();

        setupToolbar();
        setupRecyclerView();

        monitorBuoyAlerts();
        monitorNewReports();
        monitorNewForumPosts();
        loadNotificationsFeed();
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

    private void loadNotificationsFeed() {
        mDatabase.orderByChild("created_at").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                notifList.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    NotifModel model = snap.getValue(NotifModel.class);
                    if (model != null) {
                        notifList.add(0, model);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(NotificationActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

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

                    if (Boolean.TRUE.equals(isUnauthorized)) {
                        String lastNotif = prefs.getString("unauth_" + buoyId, "");
                        String currentMsg = (statusMessage != null) ? statusMessage : "Kakaibang galaw ang natukoy sa " + buoyId;

                        if (!lastNotif.equals(currentMsg)) {
                            prefs.edit().putString("unauth_" + buoyId, currentMsg).apply();
                            postNotificationInternal("BABALA: Kakaibang Galaw ng Boya!", currentMsg, "buoy_movement", null);
                        }
                    }

                    if (Boolean.TRUE.equals(isPossibleTheft)) {
                        String lastTheftNotif = prefs.getString("theft_" + buoyId, "");
                        String theftMsg = (statusMessage != null) ? statusMessage : "Posibleng pagnanakaw o paglipat ng lokasyon sa " + buoyId;

                        if (!lastTheftNotif.equals(theftMsg)) {
                            prefs.edit().putString("theft_" + buoyId, theftMsg).apply();
                            postNotificationInternal("ALERTO: Posibleng Pagnanakaw ng Boya!", theftMsg, "buoy_theft", null);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void monitorNewReports() {
        DatabaseReference reportsRef = FirebaseDatabase.getInstance().getReference().child("reports");

        reportsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot reportSnap : snapshot.getChildren()) {
                    final String reportId = reportSnap.getKey();
                    boolean isNotified = prefs.getBoolean("report_notif_" + reportId, false);

                    if (!isNotified) {
                        String tempCategory = reportSnap.child("type").getValue(String.class);
                        if (tempCategory == null) tempCategory = reportSnap.child("category").getValue(String.class);
                        if (tempCategory == null) tempCategory = "Bagong Ulat";
                        final String category = tempCategory;

                        String tempDescription = reportSnap.child("description").getValue(String.class);
                        if (tempDescription == null) tempDescription = reportSnap.child("message").getValue(String.class);
                        if (tempDescription == null) tempDescription = "May nag-post ng bagong ulat sa komunidad.";
                        final String description = tempDescription;

                        final String uid = reportSnap.child("uid").getValue(String.class);

                        if (uid != null) {
                            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("FisherTech").child("Users").child(uid).child("name");
                            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot userSnap) {
                                    String name = userSnap.exists() ? userSnap.getValue(String.class) : "Mangingisda";
                                    String title = name + " - " + category;

                                    if (reportId != null) {
                                        prefs.edit().putBoolean("report_notif_" + reportId, true).apply();
                                    }
                                    postNotificationInternal(title, description, "community_report", uid);
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    if (reportId != null) {
                                        prefs.edit().putBoolean("report_notif_" + reportId, true).apply();
                                    }
                                    postNotificationInternal("Bagong Ulat: " + category, description, "community_report", uid);
                                }
                            });
                        } else {
                            if (reportId != null) {
                                prefs.edit().putBoolean("report_notif_" + reportId, true).apply();
                            }
                            postNotificationInternal("Bagong Ulat: " + category, description, "community_report", null);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void monitorNewForumPosts() {
        DatabaseReference forumRef = FirebaseDatabase.getInstance().getReference().child("forum_posts");

        forumRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot postSnap : snapshot.getChildren()) {
                    final String postId = postSnap.getKey();
                    boolean isNotified = prefs.getBoolean("forum_notif_" + postId, false);

                    if (!isNotified) {
                        String content = postSnap.child("content").getValue(String.class);
                        if (content == null) content = "May bagong mensahe sa forum chat.";
                        final String messageContent = content;

                        final String uid = postSnap.child("uid").getValue(String.class);

                        if (uid != null) {
                            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("FisherTech").child("Users").child(uid).child("name");
                            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot userSnap) {
                                    String name = userSnap.exists() ? userSnap.getValue(String.class) : "Mangingisda";
                                    String title = name + " - Forum Chat";

                                    if (postId != null) {
                                        prefs.edit().putBoolean("forum_notif_" + postId, true).apply();
                                    }
                                    postNotificationInternal(title, messageContent, "forum_chat", uid);
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    if (postId != null) {
                                        prefs.edit().putBoolean("forum_notif_" + postId, true).apply();
                                    }
                                    postNotificationInternal("Bagong Chat sa Forum", messageContent, "forum_chat", uid);
                                }
                            });
                        } else {
                            if (postId != null) {
                                prefs.edit().putBoolean("forum_notif_" + postId, true).apply();
                            }
                            postNotificationInternal("Bagong Chat sa Forum", messageContent, "forum_chat", null);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ✅ Na-update para isama ang 'uid' sa notification data structure
    private void postNotificationInternal(String title, String message, String type, String uid) {
        String pushId = mDatabase.push().getKey();
        if (pushId != null) {
            Map<String, Object> map = new HashMap<>();
            map.put("title", title);
            map.put("message", message);
            map.put("created_at", System.currentTimeMillis());
            map.put("type", type);
            map.put("target", "all");
            if (uid != null) {
                map.put("uid", uid); // Naipapasa na ngayon ang UID ng nag-post!
            }
            mDatabase.child(pushId).setValue(map);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public static class NotifModel {
        public String title, message, type, target, uid; // Idinagdag ang uid field dito
        public long created_at;

        public NotifModel() {}

        public NotifModel(String title, String message, long created_at, String type, String target, String uid) {
            this.title = title;
            this.message = message;
            this.created_at = created_at;
            this.type = type;
            this.target = target;
            this.uid = uid;
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
            h.ivIcon.setImageResource(android.R.drawable.ic_dialog_info);
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