package com.example.fishertech;

public class ForumPost {
    private String uid, name, message, timestamp, profileImageUrl;
    private long createdAt;

    public ForumPost(String uid, String name, String message, String timestamp, long createdAt) {
        this(uid, name, message, timestamp, createdAt, null);
    }

    public ForumPost(String uid, String name, String message, String timestamp, long createdAt,
                     String profileImageUrl) {
        this.uid = uid;
        this.name = name;
        this.message = message;
        this.timestamp = timestamp;
        this.createdAt = createdAt;
        this.profileImageUrl = profileImageUrl;
    }

    public String getUid() { return uid; }
    public String getName() { return name; }
    public String getMessage() { return message; }
    public String getTimestamp() { return timestamp; }
    public long getCreatedAt() { return createdAt; }
    public String getProfileImageUrl() { return profileImageUrl; }
}