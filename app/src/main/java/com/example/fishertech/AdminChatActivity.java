package com.example.fishertech;

import android.content.Intent;
import android.os.Bundle;
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

    private List<ForumPost> forumList = new ArrayList<>();
    private AdminChatAdapter chatAdapter;

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

        // Ginagamit na ang AdminChatAdapter na may delete function
        chatAdapter = new AdminChatAdapter(forumList, this::deleteMessageFromFirebase);
        rvForumPosts.setAdapter(chatAdapter);

        loadForumPosts();

        fabAddPost.setOnClickListener(v -> showPostDialog());

        if (notification != null) {
            notification.setOnClickListener(v -> {
                startActivity(new Intent(AdminChatActivity.this, NotificationActivity.class));
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
    }

    private void loadForumPosts() {
        dbRef.child("forum_posts").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                forumList.clear();
                List<ForumPostTemp> tempPostList = new ArrayList<>();

                for (DataSnapshot postSnap : snapshot.getChildren()) {
                    String postId = postSnap.getKey(); // Kinukuha ang natatanging ID ng post
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
                    return;
                }

                for (ForumPostTemp temp : tempPostList) {
                    dbRef.child("FisherTech").child("Users").child(temp.uid).child("name")
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot userSnap) {
                                    String username = userSnap.exists() ? userSnap.getValue(String.class) : "Admin";

                                    // Smart Date & Time Formatting
                                    String timeString = "Kamakailan lang";
                                    long currentTime = System.currentTimeMillis();
                                    long timeDifference = currentTime - temp.createdAt;
                                    long oneDayMillis = 24 * 60 * 60 * 1000;

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

                                    forumList.add(new ForumPost(temp.postId, temp.uid, username, temp.content, timeString, temp.createdAt));

                                    Collections.sort(forumList, (p1, p2) -> Long.compare(p1.getCreatedAt(), p2.getCreatedAt()));

                                    chatAdapter.notifyDataSetChanged();

                                    if (!forumList.isEmpty()) {
                                        rvForumPosts.scrollToPosition(forumList.size() - 1);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {}
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminChatActivity.this, "Failed to load posts.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteMessageFromFirebase(String postId) {
        if (postId != null) {
            dbRef.child("forum_posts").child(postId).removeValue()
                    .addOnSuccessListener(aVoid -> Toast.makeText(AdminChatActivity.this, "Na-delete na ang mensahe.", Toast.LENGTH_SHORT).show())
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
                    Map<String, Object> postValues = new HashMap<>();
                    postValues.put("uid", user.getUid());
                    postValues.put("content", message);
                    postValues.put("created_at", System.currentTimeMillis());
                    postValues.put("likes", 0);

                    if (postId != null) {
                        dbRef.child("forum_posts").child(postId).setValue(postValues)
                                .addOnSuccessListener(aVoid -> Toast.makeText(AdminChatActivity.this, "Nai-post na!", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(AdminChatActivity.this, "Error sa pag-post.", Toast.LENGTH_SHORT).show());
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
        private String postId, uid, name, message, timestamp;
        private long createdAt;

        public ForumPost(String postId, String uid, String name, String message, String timestamp, long createdAt) {
            this.postId = postId;
            this.uid = uid;
            this.name = name;
            this.message = message;
            this.timestamp = timestamp;
            this.createdAt = createdAt;
        }

        public String getPostId() { return postId; }
        public String getUid() { return uid; }
        public String getName() { return name; }
        public String getMessage() { return message; }
        public String getTimestamp() { return timestamp; }
        public long getCreatedAt() { return createdAt; }
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
            // Gumagamit na ng bagong admin layout para sa forum posts
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_admin_forum_post, parent, false);
            return new ChatViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
            ForumPost post = posts.get(position);

            holder.tvUserName.setText(post.getName());
            holder.tvPostContent.setText(post.getMessage());
            holder.tvTimestamp.setText(post.getTimestamp());

            // Pindutin ang Burahin button para matanggal sa database at screen
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
            Button btnDeleteMessage;

            ChatViewHolder(View v) {
                super(v);
                tvUserName = v.findViewById(R.id.tvUserName);
                tvPostContent = v.findViewById(R.id.tvMessage); // Siguraduhing tugma sa ID sa item_admin_forum_post.xml
                tvTimestamp = v.findViewById(R.id.tvTimestamp);
                btnDeleteMessage = v.findViewById(R.id.btnDeleteMessage);
            }
        }
    }
}