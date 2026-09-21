package com.example.fishertech;

public class ForumPost {
    private String uid, name, message, timestamp;
    private long createdAt;

    public ForumPost(String uid, String name, String message, String timestamp, long createdAt) {
        this.uid = uid;
        this.name = name;
        this.message = message;
        this.timestamp = timestamp;
        this.createdAt = createdAt;
    }

    public String getUid() { return uid; }
    public String getName() { return name; }
    public String getMessage() { return message; }
    public String getTimestamp() { return timestamp; }
    public long getCreatedAt() { return createdAt; }
}