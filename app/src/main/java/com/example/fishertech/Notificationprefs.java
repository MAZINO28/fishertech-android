package com.example.fishertech;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Isang lugar lang para sa "last seen notification" para pareho ang
 * binabasa ng Dashboard (badge) at sinusulatan ng NotificationActivity.
 */
public final class Notificationprefs {

    private static final String PREFS_NAME = "FisherTechPrefs";
    private static final String KEY_PREFIX = "last_seen_notification_";

    private Notificationprefs() {}

    private static String key() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        // Bawat user ay may sariling "last seen" para hindi maghalo kung may ibang account
        return KEY_PREFIX + (user != null ? user.getUid() : "guest");
    }

    public static long getLastSeen(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String k = key();
        if (!prefs.contains(k)) {
            // Unang beses: huwag bilangin bilang "unread" ang mga lumang notification
            long now = System.currentTimeMillis();
            prefs.edit().putLong(k, now).apply();
            return now;
        }
        return prefs.getLong(k, 0L);
    }

    /**
     * I-save ang created_at ng pinakabagong notification na nakita ng user.
     * Hindi kailanman bumababa ang value (laging mas malaki lang ang ina-accept).
     */
    public static void markSeen(Context context, long latestCreatedAt) {
        if (latestCreatedAt <= 0) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long current = prefs.getLong(key(), 0L);
        if (latestCreatedAt > current) {
            prefs.edit().putLong(key(), latestCreatedAt).apply();
        }
    }
}