package com.example.fishertech;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public final class BottomNavigationBadgeListener {

    private final Context context;
    private final BottomNavigationView bottomNav;
    private final DatabaseReference forumPostsRef;
    private final DatabaseReference reportsRef;
    private ValueEventListener talakayanListener;
    private ValueEventListener ulatListener;

    public BottomNavigationBadgeListener(Context context, BottomNavigationView bottomNav) {
        this.context = context;
        this.bottomNav = bottomNav;
        DatabaseReference root = FirebaseDatabase.getInstance().getReference();
        forumPostsRef = root.child("forum_posts");
        reportsRef = root.child("reports");
    }

    public void start() {
        if (talakayanListener == null) {
            talakayanListener = createListener(true);
            forumPostsRef.addValueEventListener(talakayanListener);
        }
        if (ulatListener == null) {
            ulatListener = createListener(false);
            reportsRef.addValueEventListener(ulatListener);
        }
    }

    public void stop() {
        if (talakayanListener != null) {
            forumPostsRef.removeEventListener(talakayanListener);
            talakayanListener = null;
        }
        if (ulatListener != null) {
            reportsRef.removeEventListener(ulatListener);
            ulatListener = null;
        }
    }

    public void clearBadge(int menuItemId) {
        bottomNav.removeBadge(menuItemId);
    }

    private ValueEventListener createListener(boolean talakayan) {
        return new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long lastSeen = talakayan
                        ? Notificationprefs.getLastSeenTalakayan(context)
                        : Notificationprefs.getLastSeenUlat(context);
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                String currentUserId = currentUser != null ? currentUser.getUid() : null;
                int unreadCount = 0;
                for (DataSnapshot item : snapshot.getChildren()) {
                    Long createdAt = item.child("created_at").getValue(Long.class);
                    String senderId = item.child("uid").getValue(String.class);
                    if (createdAt == null || createdAt <= lastSeen) continue;
                    if (currentUserId != null && currentUserId.equals(senderId)) continue;
                    unreadCount++;
                }
                updateBadge(talakayan ? R.id.nav_chat : R.id.nav_report, unreadCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("BottomNavBadge", "Unable to read " + (talakayan ? "forum_posts" : "reports")
                        + ": " + error.getMessage());
            }
        };
    }

    private void updateBadge(int menuItemId, int unreadCount) {
        if (unreadCount <= 0) {
            bottomNav.removeBadge(menuItemId);
            return;
        }
        BadgeDrawable badge = bottomNav.getOrCreateBadge(menuItemId);
        badge.setNumber(unreadCount);
        badge.setVisible(true);
    }
}
