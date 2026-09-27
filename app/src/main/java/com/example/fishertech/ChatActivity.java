package com.example.fishertech;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout; // ✅ SwipeRefreshLayout Import

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
    private SwipeRefreshLayout swipeRefreshLayout; // ✅ SwipeRefreshLayout Variable

    private List<ForumPost> forumList = new ArrayList<>();
    private ChatAdapter chatAdapter;

    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        dbRef = FirebaseDatabase.getInstance().getReference();

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
    }

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
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout); // ✅ Initialization
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
                    dbRef.child("FisherTech").child("Users").child(temp.uid).child("name")
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot userSnap) {
                                    String username = userSnap.exists() ? userSnap.getValue(String.class) : "User";
                                    String timeString = formatTimestamp(temp.createdAt);

                                    loadedPosts.add(new ForumPost(temp.uid, username, temp.content, timeString, temp.createdAt));

                                    if (loadedPosts.size() == totalPosts) {
                                        Collections.sort(loadedPosts, (p1, p2) -> Long.compare(p1.getCreatedAt(), p2.getCreatedAt()));

                                        forumList.clear();
                                        forumList.addAll(loadedPosts);
                                        chatAdapter.notifyDataSetChanged();

                                        if (!forumList.isEmpty()) {
                                            rvForumPosts.scrollToPosition(forumList.size() - 1);
                                        }

                                        // ✅ Ihinto ang refresh animation pagkatapos ma-load
                                        if (swipeRefreshLayout != null) {
                                            swipeRefreshLayout.setRefreshing(false);
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    loadedPosts.add(new ForumPost(temp.uid, "User", temp.content, "Kamakailan lang", temp.createdAt));
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
        long oneDayMillis = 24 * 60 * 60 * 1000;

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
                    Map<String, Object> postValues = new HashMap<>();
                    postValues.put("uid", user.getUid());
                    postValues.put("content", message);
                    postValues.put("created_at", System.currentTimeMillis());
                    postValues.put("likes", 0);

                    if (postId != null) {
                        dbRef.child("forum_posts").child(postId).setValue(postValues)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(ChatActivity.this, "Nai-post na!", Toast.LENGTH_SHORT).show();
                                    loadForumPosts(); // I-refresh ang listahan pagka-post
                                })
                                .addOnFailureListener(e -> Toast.makeText(ChatActivity.this, "Error sa pag-post.", Toast.LENGTH_SHORT).show());
                    }
                }
            }
        });

        builder.setNegativeButton("I-Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
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

            holder.tvPostContent.setText(post.getMessage());
            holder.tvTimestamp.setText(post.getTimestamp());
        }

        @Override
        public int getItemCount() {
            return posts.size();
        }

        class ChatViewHolder extends RecyclerView.ViewHolder {
            TextView tvUserName, tvPostContent, tvTimestamp;
            LinearLayout bubbleWrapper, chatBubble;

            ChatViewHolder(View v) {
                super(v);
                tvUserName = v.findViewById(R.id.tvUserName);
                tvPostContent = v.findViewById(R.id.tvPostContent);
                tvTimestamp = v.findViewById(R.id.tvTimestamp);
                bubbleWrapper = v.findViewById(R.id.bubbleWrapper);
                chatBubble = v.findViewById(R.id.chatBubble);
            }
        }
    }
}