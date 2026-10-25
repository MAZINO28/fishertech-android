package com.example.fishertech;

public class ReportPost {
    String name;
    String category;
    String description;
    String additionalRemarks;
    String timestamp; // Idinagdag para sa oras
    String profileImageUrl;

    public ReportPost(String name, String category, String description, String additionalRemarks, String timestamp) {
        this(name, category, description, additionalRemarks, timestamp, null);
    }

    public ReportPost(String name, String category, String description, String additionalRemarks,
                      String timestamp, String profileImageUrl) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.additionalRemarks = additionalRemarks;
        this.timestamp = timestamp;
        this.profileImageUrl = profileImageUrl;
    }

    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getAdditionalRemarks() { return additionalRemarks; }
    public String getTimestamp() { return timestamp; }
    public String getProfileImageUrl() { return profileImageUrl; }
}