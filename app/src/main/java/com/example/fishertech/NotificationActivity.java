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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private Toolbar toolbar;
    private NotifAdapter adapter;
    private List<NotifModel> notifList;
    private DatabaseReference mDatabase;
    private SharedPreferences prefs;

    // Para ma-remove ang mga listener kapag hindi na nakikita ang screen
    private final List<Query> activeQueries = new ArrayList<>();
    private final List<ValueEventListener> activeListeners = new ArrayList<>();

    // Mga key na ino-proseso na o tapos na sa session na ito (iwas doble)
    private final Set<String> handledKeys = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        prefs = getSharedPreferences("FisherTechPrefs", MODE_PRIVATE);
        mDatabase = FirebaseDatabase.getInstance().getReference().child("FisherTech").child("notifications");

        // ❌ TINANGGAL: dating "last_seen = ngayon" dito. Mali iyon kasi ang mga bagong
        // notification ay nagagawa PAGKATAPOS nito, kaya nabibilang na unread.
        // Ngayon, ang "last seen" ay sinesave base sa pinakabagong notification na nakita sa feed.

        setupToolbar();
        setupRecyclerView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        monitorBuoyAlerts();
        monitorNewReports();
        monitorNewForumPosts();
        loadNotificationsFeed();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopListening();
    }

    private void listen(Query query, ValueEventListener listener) {
        query.addValueEventListener(listener);
        activeQueries.add(query);
        activeListeners.add(listener);
    }

    private void stopListening() {
        for (int i = 0; i < activeQueries.size(); i++) {
            activeQueries.get(i).removeEventListener(activeListeners.get(i));
        }
        activeQueries.clear();
        activeListeners.clear();
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
        listen(mDatabase.orderByChild("created_at"), new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                notifList.clear();
                long latest = 0;
                for (DataSnapshot snap : snapshot.getChildren()) {
                    NotifModel model = snap.getValue(NotifModel.class);
                    if (model != null) {
                        notifList.add(0, model);
                        if (model.created_at > latest) latest = model.created_at;
                    }
                }
                adapter.notifyDataSetChanged();

                // ✅ Lahat ng nakikita sa screen na ito ay "nabasa" na.
                // Tumatakbo rin ito sa bawat bagong dating habang bukas ang screen.
                Notificationprefs.markSeen(NotificationActivity.this, latest);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(NotificationActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ================== PAGGAWA NG NOTIFICATION (WALANG DUPLICATE) ==================

    /**
     * True kung dapat pang iproseso ang key. Hindi na uulitin kung nagawa na
     * (naka-save sa prefs) o ino-proseso pa lang sa session na ito.
     */
    private boolean claim(String key) {
        if (prefs.getBoolean("notif_done_" + key, false)) return false;
        return handledKeys.add(key);
    }

    /**
     * Gagawa ng notification gamit ang FIXED na key. Kaya kahit ilang device o
     * ilang beses patakbuhin, iisang notification lang ang mabubuo (hindi na doble).
     */
    private void createIfAbsent(final String key, final String title, final String message,
                                final String type, final String uid, final long createdAt) {
        final DatabaseReference ref = mDatabase.child(key);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("title", title);
                    map.put("message", message);
                    map.put("created_at", createdAt);
                    map.put("type", type);
                    map.put("target", "all");
                    if (uid != null) map.put("uid", uid);
                    ref.setValue(map);
                }
                prefs.edit().putBoolean("notif_done_" + key, true).apply();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                handledKeys.remove(key); // para ma-retry sa susunod
            }
        });
    }

    private void monitorBuoyAlerts() {
        DatabaseReference buoysRef = FirebaseDatabase.getInstance().getReference().child("FisherTech").child("buoys");

        listen(buoysRef, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot buoySnap : snapshot.getChildren()) {
                    String buoyId = buoySnap.getKey();
                    if (buoyId == null) continue;

                    Boolean isUnauthorized = buoySnap.child("is_unauthorized_movement").getValue(Boolean.class);
                    Boolean isPossibleTheft = buoySnap.child("is_possible_theft").getValue(Boolean.class);
                    String statusMessage = buoySnap.child("status_message").getValue(String.class);

                    if (Boolean.TRUE.equals(isUnauthorized)) {
                        String msg = (statusMessage != null) ? statusMessage : "Kakaibang galaw ang natukoy sa " + buoyId;
                        String key = "buoy_move_" + buoyId + "_" + msg.hashCode();
                        if (claim(key)) {
                            createIfAbsent(key, "BABALA: Kakaibang Galaw ng Boya!", msg,
                                    "buoy_movement", null, System.currentTimeMillis());
                        }
                    }

                    if (Boolean.TRUE.equals(isPossibleTheft)) {
                        String msg = (statusMessage != null) ? statusMessage : "Posibleng pagnanakaw o paglipat ng lokasyon sa " + buoyId;
                        String key = "buoy_theft_" + buoyId + "_" + msg.hashCode();
                        if (claim(key)) {
                            createIfAbsent(key, "ALERTO: Posibleng Pagnanakaw ng Boya!", msg,
                                    "buoy_theft", null, System.currentTimeMillis());
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

        listen(reportsRef, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot reportSnap : snapshot.getChildren()) {
                    final String reportId = reportSnap.getKey();
                    if (reportId == null) continue;

                    final String key = "report_" + reportId;
                    if (!claim(key)) continue;

                    String tempCategory = reportSnap.child("type").getValue(String.class);
                    if (tempCategory == null) tempCategory = reportSnap.child("category").getValue(String.class);
                    if (tempCategory == null) tempCategory = "Bagong Ulat";
                    final String category = tempCategory;

                    String tempDescription = reportSnap.child("description").getValue(String.class);
                    if (tempDescription == null) tempDescription = reportSnap.child("message").getValue(String.class);
                    if (tempDescription == null) tempDescription = "May nag-post ng bagong ulat sa komunidad.";
                    final String description = tempDescription;

                    final String uid = reportSnap.child("uid").getValue(String.class);

                    // ✅ Gamitin ang ACTUAL na oras ng pag-post ng ulat, hindi ang oras na binuksan ang Abiso
                    Long reportTime = reportSnap.child("created_at").getValue(Long.class);
                    final long createdAt = (reportTime != null) ? reportTime : System.currentTimeMillis();

                    if (uid != null) {
                        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference()
                                .child("FisherTech").child("Users").child(uid).child("name");
                        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot userSnap) {
                                String name = userSnap.exists() ? userSnap.getValue(String.class) : "Mangingisda";
                                createIfAbsent(key, name + " - " + category, description,
                                        "community_report", uid, createdAt);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                createIfAbsent(key, "Bagong Ulat: " + category, description,
                                        "community_report", uid, createdAt);
                            }
                        });
                    } else {
                        createIfAbsent(key, "Bagong Ulat: " + category, description,
                                "community_report", null, createdAt);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void monitorNewForumPosts() {
        DatabaseReference forumRef = FirebaseDatabase.getInstance().getReference().child("forum_posts");

        listen(forumRef, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot postSnap : snapshot.getChildren()) {
                    final String postId = postSnap.getKey();
                    if (postId == null) continue;

                    final String key = "forum_" + postId;
                    if (!claim(key)) continue;

                    String content = postSnap.child("content").getValue(String.class);
                    if (content == null) content = "May bagong mensahe sa forum chat.";
                    final String messageContent = content;

                    final String uid = postSnap.child("uid").getValue(String.class);

                    // ✅ Gamitin ang oras ng mismong chat post kung meron
                    Long postTime = postSnap.child("created_at").getValue(Long.class);
                    if (postTime == null) postTime = postSnap.child("timestamp").getValue(Long.class);
                    final long createdAt = (postTime != null) ? postTime : System.currentTimeMillis();

                    if (uid != null) {
                        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference()
                                .child("FisherTech").child("Users").child(uid).child("name");
                        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot userSnap) {
                                String name = userSnap.exists() ? userSnap.getValue(String.class) : "Mangingisda";
                                createIfAbsent(key, name + " - Forum Chat", messageContent,
                                        "forum_chat", uid, createdAt);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                createIfAbsent(key, "Bagong Chat sa Forum", messageContent,
                                        "forum_chat", uid, createdAt);
                            }
                        });
                    } else {
                        createIfAbsent(key, "Bagong Chat sa Forum", messageContent,
                                "forum_chat", null, createdAt);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public static class NotifModel {
        public String title, message, type, target, uid;
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