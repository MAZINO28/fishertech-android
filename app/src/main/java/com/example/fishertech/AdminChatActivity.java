package com.example.fishertech;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminChatActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private BottomNavigationView bottomNav;
    private RecyclerView rvForumPosts;
    private FloatingActionButton fabAddPost;
    private ImageView notification;

    private List<ForumPost> forumList = new ArrayList<>();
    private ChatAdapter chatAdapter;

    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chat);

        dbRef = FirebaseDatabase
                .getInstance()
                .getReference();

        initViews();

        rvForumPosts.setLayoutManager(
                new LinearLayoutManager(this)
        );

        chatAdapter = new ChatAdapter(forumList);

        rvForumPosts.setAdapter(chatAdapter);

        loadForumPosts();

        fabAddPost.setOnClickListener(
                v -> showPostDialog()
        );

        if (notification != null) {

            notification.setOnClickListener(v -> {

                Intent intent = new Intent(
                        AdminChatActivity.this,
                        NotificationActivity.class
                );

                startActivity(intent);
            });
        }

        setupNavigation();
    }

    private void initViews() {

        toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {

            getSupportActionBar()
                    .setDisplayShowTitleEnabled(false);
        }

        rvForumPosts = findViewById(R.id.rvForumPosts);

        fabAddPost = findViewById(R.id.fabAddPost);

        notification = findViewById(R.id.notification);

        bottomNav = findViewById(R.id.bottomNav);
    }

    private void loadForumPosts() {

        dbRef.child("forum_posts")
                .addValueEventListener(
                        new ValueEventListener() {

                            @Override
                            public void onDataChange(
                                    @NonNull DataSnapshot snapshot
                            ) {

                                forumList.clear();

                                for (DataSnapshot postSnap : snapshot.getChildren()) {

                                    String uid = postSnap
                                            .child("uid")
                                            .getValue(String.class);

                                    String content = postSnap
                                            .child("content")
                                            .getValue(String.class);

                                    if (uid != null) {

                                        dbRef.child("FisherTech") // Idinagdag ito
                                                .child("Users")
                                                .child(uid)
                                                .child("name")
                                                .addListenerForSingleValueEvent(
                                                        new ValueEventListener() {

                                                            @Override
                                                            public void onDataChange(
                                                                    @NonNull DataSnapshot userSnap
                                                            ) {

                                                                String username;

                                                                if (userSnap.exists()) {

                                                                    username = userSnap.getValue(String.class);

                                                                } else {

                                                                    username = "ADMIN";
                                                                }

                                                                forumList.add(
                                                                        0,
                                                                        new ForumPost(
                                                                                username,
                                                                                content
                                                                        )
                                                                );

                                                                chatAdapter.notifyDataSetChanged();
                                                            }

                                                            @Override
                                                            public void onCancelled(
                                                                    @NonNull DatabaseError error
                                                            ) {

                                                            }
                                                        }
                                                );
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(
                                    @NonNull DatabaseError error
                            ) {

                                Toast.makeText(
                                        AdminChatActivity.this,
                                        "Failed to load posts.",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                );
    }

    private void showPostDialog() {

        AlertDialog.Builder builder =
                new AlertDialog.Builder(this);

        builder.setTitle("Bagong Post sa Forum");

        final EditText input = new EditText(this);

        input.setHint("Ano ang balita sa dagat?");

        builder.setView(input);

        builder.setPositiveButton(
                "I-Post",
                (dialog, which) -> {

                    String message = input.getText()
                            .toString()
                            .trim();

                    if (!message.isEmpty()) {

                        FirebaseUser user =
                                FirebaseAuth.getInstance()
                                        .getCurrentUser();

                        if (user != null) {

                            String postId = dbRef
                                    .child("forum_posts")
                                    .push()
                                    .getKey();

                            Map<String, Object> postValues =
                                    new HashMap<>();

                            postValues.put(
                                    "uid",
                                    user.getUid()
                            );

                            postValues.put(
                                    "content",
                                    message
                            );

                            postValues.put(
                                    "created_at",
                                    System.currentTimeMillis()
                            );

                            postValues.put(
                                    "likes",
                                    0
                            );

                            if (postId != null) {

                                dbRef.child("forum_posts")
                                        .child(postId)
                                        .setValue(postValues)

                                        .addOnSuccessListener(aVoid ->

                                                Toast.makeText(
                                                        AdminChatActivity.this,
                                                        "Nai-post na!",
                                                        Toast.LENGTH_SHORT
                                                ).show()
                                        )

                                        .addOnFailureListener(e ->

                                                Toast.makeText(
                                                        AdminChatActivity.this,
                                                        "Error sa pag-post.",
                                                        Toast.LENGTH_SHORT
                                                ).show()
                                        );
                            }
                        }
                    }
                }
        );

        builder.setNegativeButton(
                "I-Cancel",
                (dialog, which) -> dialog.cancel()
        );

        builder.show();
    }

    private void setupNavigation() {

        if (bottomNav != null) {

            bottomNav.setSelectedItemId(
                    R.id.nav_chat
            );

            bottomNav.setOnItemSelectedListener(item -> {

                int id = item.getItemId();

                if (id == R.id.nav_chat) {
                    return true;
                }

                Intent intent = null;

                if (id == R.id.nav_home) {

                    intent = new Intent(
                            this,
                            DashboardAdminActivity.class
                    );

                } else if (id == R.id.nav_location) {

                    intent = new Intent(
                            this,
                            AdminLocationActivity.class
                    );

                } else if (id == R.id.nav_report) {

                    intent = new Intent(
                            this,
                            AdminReportsActivity.class
                    );

                } else if (id == R.id.nav_profile) {

                    intent = new Intent(
                            this,
                            AdminProfileActivity.class
                    );
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

    public static class ForumPost {

        String name;
        String message;

        public ForumPost(
                String name,
                String message
        ) {

            this.name = name;
            this.message = message;
        }
    }

    private class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

        private List<ForumPost> posts;

        public ChatAdapter(List<ForumPost> posts) {

            this.posts = posts;
        }

        @NonNull
        @Override
        public ChatViewHolder onCreateViewHolder(
                @NonNull ViewGroup parent,
                int viewType
        ) {

            View view = LayoutInflater
                    .from(parent.getContext())
                    .inflate(
                            android.R.layout.simple_list_item_2,
                            parent,
                            false
                    );

            RecyclerView.LayoutParams lp =
                    (RecyclerView.LayoutParams)
                            view.getLayoutParams();

            lp.setMargins(20, 15, 20, 15);

            view.setLayoutParams(lp);

            return new ChatViewHolder(view);
        }

        @Override
        public void onBindViewHolder(
                @NonNull ChatViewHolder holder,
                int position
        ) {

            ForumPost post = posts.get(position);

            holder.tvName.setText(post.name);

            holder.tvMessage.setText(post.message);
        }

        @Override
        public int getItemCount() {

            return posts.size();
        }

        class ChatViewHolder extends RecyclerView.ViewHolder {

            TextView tvName;
            TextView tvMessage;

            ChatViewHolder(View v) {

                super(v);

                tvName = v.findViewById(
                        android.R.id.text1
                );

                tvMessage = v.findViewById(
                        android.R.id.text2
                );

                v.setBackgroundResource(
                        R.drawable.chat_bubble
                );

                v.setPadding(40, 30, 40, 30);

                tvName.setTextColor(
                        getResources().getColor(
                                android.R.color.black
                        )
                );

                tvName.setTypeface(
                        null,
                        Typeface.BOLD
                );

                tvMessage.setTextColor(
                        getResources().getColor(
                                android.R.color.black
                        )
                );

                tvMessage.setTextSize(16);
            }
        }
    }
}