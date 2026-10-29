package com.example.fishertech;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

public class ChatActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private BottomNavigationView bottomNav;
    private RecyclerView rvForumPosts;
    private FloatingActionButton fabAddPost;
    private ImageView notification;
    private SwipeRefreshLayout swipeRefreshLayout;

    private List<ForumPost> forumList = new ArrayList<>();
    private ChatAdapter chatAdapter;

    private DatabaseReference dbRef;

    // ✅ Para sa Real-time Notification Badge Listener
    private DatabaseReference notifRef;
    private ValueEventListener badgeListener;
    private BottomNavigationBadgeListener bottomNavigationBadgeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        dbRef = FirebaseDatabase.getInstance().getReference();
        notifRef = FirebaseDatabase.getInstance().getReference().child("FisherTech").child("notifications");

        initViews();

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvForumPosts.setLayoutManager(layoutManager);

        chatAdapter = new ChatAdapter(forumList);
        rvForumPosts.setAdapter(chatAdapter);

        loadForumPosts();

        // ✅ Swipe-to-Refresh Listener
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                loadForumPosts();
            });
        }

        fabAddPost.setOnClickListener(v -> showPostDialog());

        if (notification != null) {
            notification.setOnClickListener(v -> {
                startActivity(new Intent(ChatActivity.this, NotificationActivity.class));
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
    }

    // ✅ Itigil ang listener kapag lumipat ng ibang screen para walang memory leak o stacking
    @Override
    protected void onStop() {
        super.onStop();
        stopBadgeListener();
        bottomNavigationBadgeListener.stop();
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
                long lastSeen = Notificationprefs.getLastSeen(ChatActivity.this);

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
        View toolbarView = findViewById(R.id.toolbar);
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
        dbRef.child("forum_posts").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ForumPostTemp> tempPostList = new ArrayList<>();

                for (DataSnapshot postSnap : snapshot.getChildren()) {
                    String uid = postSnap.child("uid").getValue(String.class);
                    String content = postSnap.child("content").getValue(String.class);
                    Long createdAt = postSnap.child("created_at").getValue(Long.class);

                    if (uid != null && content != null) {
                        long timestamp = (createdAt != null) ? createdAt : 0L;
                        tempPostList.add(new ForumPostTemp(uid, content, timestamp));
                    }
                }

                if (tempPostList.isEmpty()) {
                    forumList.clear();
                    chatAdapter.notifyDataSetChanged();
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    return;
                }

                Collections.sort(tempPostList, Comparator.comparingLong(ForumPostTemp::getCreatedAt));

                List<ForumPost> loadedPosts = new ArrayList<>();
                final int totalPosts = tempPostList.size();

                for (ForumPostTemp temp : tempPostList) {
                    dbRef.child("FisherTech").child("Users").child(temp.uid)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot userSnap) {
                                    String username = userSnap.child("name").getValue(String.class);
                                    if (username == null) username = "User";
                                    String profileImageUrl = userSnap.child("profile_pic").getValue(String.class);
                                    String timeString = formatTimestamp(temp.createdAt);

                                    loadedPosts.add(new ForumPost(temp.uid, username, temp.content, timeString,
                                            temp.createdAt, profileImageUrl));

                                    if (loadedPosts.size() == totalPosts) {
                                        Collections.sort(loadedPosts, (p1, p2) -> Long.compare(p1.getCreatedAt(), p2.getCreatedAt()));

                                        forumList.clear();
                                        forumList.addAll(loadedPosts);
                                        chatAdapter.notifyDataSetChanged();

                                        if (!forumList.isEmpty()) {
                                            rvForumPosts.scrollToPosition(forumList.size() - 1);
                                        }

                                        if (swipeRefreshLayout != null) {
                                            swipeRefreshLayout.setRefreshing(false);
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    loadedPosts.add(new ForumPost(temp.uid, "User", temp.content,
                                            "Kamakailan lang", temp.createdAt, null));
                                    if (loadedPosts.size() == totalPosts) {
                                        Collections.sort(loadedPosts, (p1, p2) -> Long.compare(p1.getCreatedAt(), p2.getCreatedAt()));
                                        forumList.clear();
                                        forumList.addAll(loadedPosts);
                                        chatAdapter.notifyDataSetChanged();
                                        if (!forumList.isEmpty()) {
                                            rvForumPosts.scrollToPosition(forumList.size() - 1);
                                        }
                                        if (swipeRefreshLayout != null) {
                                            swipeRefreshLayout.setRefreshing(false);
                                        }
                                    }
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Failed to load posts.", Toast.LENGTH_SHORT).show();
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        });
    }

    private String formatTimestamp(long timestamp) {
        long currentTime = System.currentTimeMillis();
        long timeDifference = currentTime - timestamp;
        long oneDayMillis = 24L * 60L * 60L * 1000L;

        Calendar postDate = Calendar.getInstance();
        postDate.setTimeInMillis(timestamp);

        Calendar currentDate = Calendar.getInstance();
        currentDate.setTimeInMillis(currentTime);

        boolean isSameDay = postDate.get(Calendar.YEAR) == currentDate.get(Calendar.YEAR) &&
                postDate.get(Calendar.DAY_OF_YEAR) == currentDate.get(Calendar.DAY_OF_YEAR);

        if (isSameDay) {
            SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
            return timeFormat.format(new Date(timestamp));
        } else if (timeDifference < (7 * oneDayMillis)) {
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEE, h:mm a", Locale.getDefault());
            return dayFormat.format(new Date(timestamp));
        } else {
            SimpleDateFormat fullFormat = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());
            return fullFormat.format(new Date(timestamp));
        }
    }

    private void showPostDialog() {
        // Gumamit ng MaterialAlertDialogBuilder para sa modern Material Design style
        com.google.android.material.dialog.MaterialAlertDialogBuilder builder =
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);

        builder.setTitle("Bagong Post sa Forum");

        // Gumawa ng layout container para mailagay nang maayos ang padding ng EditText
        final EditText input = new EditText(this);
        input.setHint("Ano ang balita sa dagat?");
        input.setFocusable(true);
        input.setFocusableInTouchMode(true);

        // Maglagay ng margins/padding sa EditText para hindi dumikit sa gilid ng box
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(50, 20, 50, 20); // Left, Top, Right, Bottom margins
        input.setLayoutParams(params);
        container.addView(input);

        builder.setView(container);

        builder.setPositiveButton("I-Post", (dialog, which) -> {
            String message = input.getText().toString().trim();
            if (!message.isEmpty()) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    final String postId = dbRef.child("forum_posts").push().getKey();
                    final long now = System.currentTimeMillis();

                    Map<String, Object> postValues = new HashMap<>();
                    postValues.put("uid", user.getUid());
                    postValues.put("content", message);
                    postValues.put("created_at", now);
                    postValues.put("likes", 0);

                    if (postId != null) {
                        dbRef.child("forum_posts").child(postId).setValue(postValues)
                                .addOnSuccessListener(aVoid -> {
                                    createForumNotification(postId, user.getUid(), message, now);

                                    Toast.makeText(ChatActivity.this, "Nai-post na!", Toast.LENGTH_SHORT).show();
                                    loadForumPosts();
                                })
                                .addOnFailureListener(e -> Toast.makeText(ChatActivity.this, "Error sa pag-post.", Toast.LENGTH_SHORT).show());
                    }
                }
            }
        });

        builder.setNegativeButton("I-Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();

        // I-set ang kulay ng mga button para sumunod sa theme ng app mo (#1E4D6B)
        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#1E4D6B"));
        }
        if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#1E4D6B"));
        }
    }
    private void createForumNotification(String postId, String uid, String message, long createdAt) {
        dbRef.child("FisherTech").child("Users").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot userSnap) {
                        String name = userSnap.child("name").getValue(String.class);
                        if (name == null) name = "Mangingisda";
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

    private static class ForumPostTemp {
        String uid, content;
        long createdAt;

        public ForumPostTemp(String uid, String content, long createdAt) {
            this.uid = uid;
            this.content = content;
            this.createdAt = createdAt;
        }

        public long getCreatedAt() { return createdAt; }
    }

    private class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
        private List<ForumPost> posts;

        public ChatAdapter(List<ForumPost> posts) {
            this.posts = posts;
        }

        @NonNull
        @Override
        public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_forum_post, parent, false);
            return new ChatViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
            ForumPost post = posts.get(position);

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            String currentUserId = (currentUser != null) ? currentUser.getUid() : "";

            boolean isMe = post.getUid() != null && post.getUid().equals(currentUserId);

            if (isMe) {
                holder.bubbleWrapper.setGravity(Gravity.END);
                holder.chatBubble.setBackgroundResource(R.drawable.chat_bubble_right);
                holder.tvUserName.setVisibility(View.GONE);
                holder.tvPostContent.setTextColor(Color.WHITE);
                holder.tvTimestamp.setTextColor(Color.parseColor("#E0E0E0"));
            } else {
                holder.bubbleWrapper.setGravity(Gravity.START);
                holder.chatBubble.setBackgroundResource(R.drawable.chat_bubble_left);
                holder.tvUserName.setVisibility(View.VISIBLE);
                holder.tvUserName.setText(post.getName());
                holder.tvPostContent.setTextColor(Color.parseColor("#333333"));
                holder.tvTimestamp.setTextColor(Color.parseColor("#777777"));
            }

            if (isMe) {
                holder.ivAuthorProfile.setVisibility(View.GONE);
            } else {
                holder.ivAuthorProfile.setVisibility(View.VISIBLE);
                Glide.with(holder.ivAuthorProfile)
                        .load(post.getProfileImageUrl())
                        .circleCrop()
                        .placeholder(R.drawable.ic_user_placeholder)
                        .error(R.drawable.ic_user_placeholder)
                        .into(holder.ivAuthorProfile);
            }
            holder.tvPostContent.setText(post.getMessage());
            holder.tvTimestamp.setText(post.getTimestamp());
        }

        @Override
        public int getItemCount() {
            return posts.size();
        }

        class ChatViewHolder extends RecyclerView.ViewHolder {
            TextView tvUserName, tvPostContent, tvTimestamp;
            ImageView ivAuthorProfile;
            LinearLayout bubbleWrapper, chatBubble;

            ChatViewHolder(View v) {
                super(v);
                tvUserName = v.findViewById(R.id.tvUserName);
                ivAuthorProfile = v.findViewById(R.id.ivAuthorProfile);
                tvPostContent = v.findViewById(R.id.tvPostContent);
                tvTimestamp = v.findViewById(R.id.tvTimestamp);
                bubbleWrapper = v.findViewById(R.id.bubbleWrapper);
                chatBubble = v.findViewById(R.id.chatBubble);
            }
        }
    }
}