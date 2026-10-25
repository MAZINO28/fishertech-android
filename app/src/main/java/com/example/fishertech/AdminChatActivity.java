package com.example.fishertech;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminChatActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private BottomNavigationView bottomNav;
    private RecyclerView rvForumPosts;
    private FloatingActionButton fabAddPost;
    private ImageView notification;
    private SwipeRefreshLayout swipeRefreshLayout;

    private List<ForumPost> forumList = new ArrayList<>();
    private AdminChatAdapter chatAdapter;

    private DatabaseReference dbRef;

    // ✅ Para sa Real-time Notification Badge Listener
    private DatabaseReference notifRef;
    private ValueEventListener badgeListener;
    private BottomNavigationBadgeListener bottomNavigationBadgeListener;
    private ValueEventListener forumPostsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_chat);

        dbRef = FirebaseDatabase.getInstance().getReference();
        notifRef = FirebaseDatabase.getInstance().getReference().child("FisherTech").child("notifications");

        initViews();

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvForumPosts.setLayoutManager(layoutManager);

        chatAdapter = new AdminChatAdapter(forumList, this::deleteMessageFromFirebase);
        rvForumPosts.setAdapter(chatAdapter);

        // ✅ Swipe-to-Refresh Listener
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::loadForumPosts);
        }

        fabAddPost.setOnClickListener(v -> showPostDialog());

        if (notification != null) {
            notification.setOnClickListener(v -> {
                startActivity(new Intent(AdminChatActivity.this, NotificationActivity.class));
            });
        }

        setupNavigation();
        bottomNavigationBadgeListener = new BottomNavigationBadgeListener(this, bottomNav);
    }

    // ✅ Simulan ang badge listener tuwing nakikita ang screen
    @Override
    protected void onStart() {
        super.onStart();
        startBadgeListener();
        bottomNavigationBadgeListener.start();
        startForumPostsListener();
    }

    // ✅ Itigil ang listener kapag lumipat ng ibang screen para walang memory leak
    @Override
    protected void onStop() {
        super.onStop();
        stopBadgeListener();
        bottomNavigationBadgeListener.stop();
        stopForumPostsListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Notificationprefs.setLastSeenTalakayan(this, System.currentTimeMillis());
        bottomNavigationBadgeListener.clearBadge(R.id.nav_chat);
    }

    // ================== UNREAD BADGE SYSTEM ==================

    private void startBadgeListener() {
        if (badgeListener != null) return;

        badgeListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long lastSeen = Notificationprefs.getLastSeen(AdminChatActivity.this);

                int unreadCount = 0;
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Long createdAt = snap.child("created_at").getValue(Long.class);
                    if (createdAt == null) continue;

                    if (createdAt <= lastSeen) continue;
                    unreadCount++;
                }

                updateBadge(unreadCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("BadgeError", "Error loading unread notifications: " + error.getMessage());
            }
        };

        notifRef.addValueEventListener(badgeListener);
    }

    private void stopBadgeListener() {
        if (badgeListener != null && notifRef != null) {
            notifRef.removeEventListener(badgeListener);
        }
        badgeListener = null;
    }

    private void updateBadge(int unreadCount) {
        Toolbar toolbarView = findViewById(R.id.toolbar);
        if (toolbarView == null) return;

        TextView tvBadge = toolbarView.findViewById(R.id.tvNotificationBadge);
        if (tvBadge == null) return;

        if (unreadCount > 0) {
            tvBadge.setVisibility(View.VISIBLE);
            tvBadge.setText(unreadCount > 99 ? "99+" : String.valueOf(unreadCount));
            tvBadge.bringToFront();
        } else {
            tvBadge.setVisibility(View.GONE);
        }
    }

    // ========================================================

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        rvForumPosts = findViewById(R.id.rvForumPosts);
        fabAddPost = findViewById(R.id.fabAddPost);
        notification = findViewById(R.id.notification);
        bottomNav = findViewById(R.id.bottomNav);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
    }

    private void loadForumPosts() {
        dbRef.child("forum_posts").addListenerForSingleValueEvent(createForumPostsListener());
    }

    private void startForumPostsListener() {
        if (forumPostsListener == null) {
            forumPostsListener = createForumPostsListener();
            dbRef.child("forum_posts").addValueEventListener(forumPostsListener);
        }
    }

    private void stopForumPostsListener() {
        if (forumPostsListener != null) {
            dbRef.child("forum_posts").removeEventListener(forumPostsListener);
            forumPostsListener = null;
        }
    }

    private ValueEventListener createForumPostsListener() {
        return new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                forumList.clear();
                List<ForumPostTemp> tempPostList = new ArrayList<>();

                for (DataSnapshot postSnap : snapshot.getChildren()) {
                    String postId = postSnap.getKey();
                    String uid = postSnap.child("uid").getValue(String.class);
                    String content = postSnap.child("content").getValue(String.class);
                    Long createdAt = postSnap.child("created_at").getValue(Long.class);

                    if (postId != null && uid != null && content != null) {
                        long timestamp = (createdAt != null) ? createdAt : 0L;
                        tempPostList.add(new ForumPostTemp(postId, uid, content, timestamp));
                    }
                }

                Collections.sort(tempPostList, Comparator.comparingLong(ForumPostTemp::getCreatedAt));

                if (tempPostList.isEmpty()) {
                    chatAdapter.notifyDataSetChanged();
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    return;
                }

                long totalPosts = tempPostList.size();
                final int[] processedCount = {0};

                for (ForumPostTemp temp : tempPostList) {
                    dbRef.child("FisherTech").child("Users").child(temp.uid)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot userSnap) {
                                    String username = userSnap.child("name").getValue(String.class);
                                    if (username == null) username = "Admin";
                                    String profileImageUrl = userSnap.child("profile_pic").getValue(String.class);

                                    String timeString = "Kamakailan lang";
                                    long currentTime = System.currentTimeMillis();
                                    long timeDifference = currentTime - temp.createdAt;
                                    long oneDayMillis = 24L * 60L * 60L * 1000L;

                                    Calendar postDate = Calendar.getInstance();
                                    postDate.setTimeInMillis(temp.createdAt);

                                    Calendar currentDate = Calendar.getInstance();
                                    currentDate.setTimeInMillis(currentTime);

                                    boolean isSameDay = postDate.get(Calendar.YEAR) == currentDate.get(Calendar.YEAR) &&
                                            postDate.get(Calendar.DAY_OF_YEAR) == currentDate.get(Calendar.DAY_OF_YEAR);

                                    if (isSameDay) {
                                        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
                                        timeString = timeFormat.format(new Date(temp.createdAt));
                                    } else if (timeDifference < (7 * oneDayMillis)) {
                                        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE, h:mm a", Locale.getDefault());
                                        timeString = dayFormat.format(new Date(temp.createdAt));
                                    } else {
                                        SimpleDateFormat fullFormat = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());
                                        timeString = fullFormat.format(new Date(temp.createdAt));
                                    }

                                    forumList.add(new ForumPost(temp.postId, temp.uid, username, temp.content,
                                            timeString, temp.createdAt, profileImageUrl));

                                    processedCount[0]++;
                                    if (processedCount[0] == totalPosts) {
                                        Collections.sort(forumList, (p1, p2) -> Long.compare(p1.getCreatedAt(), p2.getCreatedAt()));
                                        chatAdapter.notifyDataSetChanged();

                                        if (!forumList.isEmpty()) {
                                            rvForumPosts.scrollToPosition(forumList.size() - 1);
                                        }
                                        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    processedCount[0]++;
                                    if (processedCount[0] == totalPosts) {
                                        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                                    }
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminChatActivity.this, "Failed to load posts.", Toast.LENGTH_SHORT).show();
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            }
        };
    }

    private void deleteMessageFromFirebase(String postId) {
        if (postId != null) {
            dbRef.child("forum_posts").child(postId).removeValue()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(AdminChatActivity.this, "Na-delete na ang mensahe.", Toast.LENGTH_SHORT).show();
                        loadForumPosts();
                    })
                    .addOnFailureListener(e -> Toast.makeText(AdminChatActivity.this, "Error sa pag-delete: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void showPostDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Bagong Post sa Forum");

        final EditText input = new EditText(this);
        input.setHint("Ano ang balita sa dagat?");
        builder.setView(input);

        builder.setPositiveButton("I-Post", (dialog, which) -> {
            String message = input.getText().toString().trim();
            if (!message.isEmpty()) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    String postId = dbRef.child("forum_posts").push().getKey();
                    long now = System.currentTimeMillis();

                    Map<String, Object> postValues = new HashMap<>();
                    postValues.put("uid", user.getUid());
                    postValues.put("content", message);
                    postValues.put("created_at", now);
                    postValues.put("likes", 0);

                    if (postId != null) {
                        dbRef.child("forum_posts").child(postId).setValue(postValues)
                                .addOnSuccessListener(aVoid -> {
                                    // ✅ Gumawe ng notification para makita ng iba at mag-update ang badge
                                    createAdminForumNotification(postId, user.getUid(), message, now);

                                    Toast.makeText(AdminChatActivity.this, "Nai-post na!", Toast.LENGTH_SHORT).show();
                                    loadForumPosts();
                                })
                                .addOnFailureListener(e -> Toast.makeText(AdminChatActivity.this, "Error sa pag-post.", Toast.LENGTH_SHORT).show());
                    }
                }
            }
        });

        builder.setNegativeButton("I-Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // ✅ Helper method para gumawa ng notification sa Firebase mula sa admin side
    private void createAdminForumNotification(String postId, String uid, String message, long createdAt) {
        dbRef.child("FisherTech").child("Users").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot userSnap) {
                        String name = userSnap.child("name").getValue(String.class);
                        if (name == null) name = "Admin";
                        String profileUrl = userSnap.child("profile_pic").getValue(String.class);

                        Map<String, Object> map = new HashMap<>();
                        map.put("title", name + " - Forum Chat");
                        map.put("message", message);
                        map.put("created_at", createdAt);
                        map.put("type", "forum_chat");
                        map.put("target", "all");
                        map.put("uid", uid);
                        if (profileUrl != null && !profileUrl.trim().isEmpty()) {
                            map.put("senderProfileUrl", profileUrl);
                        }

                        dbRef.child("FisherTech").child("notifications")
                                .child("forum_" + postId).setValue(map);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void setupNavigation() {
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_chat);

            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_chat) return true;

                Intent intent = null;
                if (id == R.id.nav_home) intent = new Intent(this, DashboardAdminActivity.class);
                else if (id == R.id.nav_location) intent = new Intent(this, AdminLocationActivity.class);
                else if (id == R.id.nav_report) intent = new Intent(this, AdminReportsActivity.class);
                else if (id == R.id.nav_profile) intent = new Intent(this, AdminProfileActivity.class);

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

    private static class ForumPostTemp {
        String postId, uid, content;
        long createdAt;

        public ForumPostTemp(String postId, String uid, String content, long createdAt) {
            this.postId = postId;
            this.uid = uid;
            this.content = content;
            this.createdAt = createdAt;
        }

        public long getCreatedAt() { return createdAt; }
    }

    public static class ForumPost {
        private String postId, uid, name, message, timestamp, profileImageUrl;
        private long createdAt;

        public ForumPost(String postId, String uid, String name, String message, String timestamp, long createdAt) {
            this(postId, uid, name, message, timestamp, createdAt, null);
        }

        public ForumPost(String postId, String uid, String name, String message, String timestamp, long createdAt,
                         String profileImageUrl) {
            this.postId = postId;
            this.uid = uid;
            this.name = name;
            this.message = message;
            this.timestamp = timestamp;
            this.createdAt = createdAt;
            this.profileImageUrl = profileImageUrl;
        }

        public String getPostId() { return postId; }
        public String getUid() { return uid; }
        public String getName() { return name; }
        public String getMessage() { return message; }
        public String getTimestamp() { return timestamp; }
        public long getCreatedAt() { return createdAt; }
        public String getProfileImageUrl() { return profileImageUrl; }
    }

    private static class AdminChatAdapter extends RecyclerView.Adapter<AdminChatAdapter.ChatViewHolder> {
        private List<ForumPost> posts;
        private OnDeleteClickListener deleteClickListener;

        public interface OnDeleteClickListener {
            void onDeleteClick(String postId);
        }

        public AdminChatAdapter(List<ForumPost> posts, OnDeleteClickListener listener) {
            this.posts = posts;
            this.deleteClickListener = listener;
        }

        @NonNull
        @Override
        public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_admin_forum_post, parent, false);
            return new ChatViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
            ForumPost post = posts.get(position);

            holder.tvUserName.setText(post.getName());
            Glide.with(holder.ivAuthorProfile)
                    .load(post.getProfileImageUrl())
                    .circleCrop()
                    .placeholder(R.drawable.ic_user_placeholder)
                    .error(R.drawable.ic_user_placeholder)
                    .into(holder.ivAuthorProfile);
            holder.tvPostContent.setText(post.getMessage());
            holder.tvTimestamp.setText(post.getTimestamp());

            holder.btnDeleteMessage.setOnClickListener(v -> {
                if (deleteClickListener != null) {
                    deleteClickListener.onDeleteClick(post.getPostId());
                }
            });
        }

        @Override
        public int getItemCount() {
            return posts.size();
        }

        static class ChatViewHolder extends RecyclerView.ViewHolder {
            TextView tvUserName, tvPostContent, tvTimestamp;
            ImageView ivAuthorProfile;
            Button btnDeleteMessage;

            ChatViewHolder(View v) {
                super(v);
                tvUserName = v.findViewById(R.id.tvUserName);
                ivAuthorProfile = v.findViewById(R.id.ivAuthorProfile);
                tvPostContent = v.findViewById(R.id.tvMessage);
                tvTimestamp = v.findViewById(R.id.tvTimestamp);
                btnDeleteMessage = v.findViewById(R.id.btnDeleteMessage);
            }
        }
    }
}