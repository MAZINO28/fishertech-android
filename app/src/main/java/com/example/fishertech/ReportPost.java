package com.example.fishertech;

public class ReportPost {
    String name;
    String category;
    String description;
    String additionalRemarks;
    String timestamp; // Idinagdag para sa oras

    public ReportPost(String name, String category, String description, String additionalRemarks, String timestamp) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.additionalRemarks = additionalRemarks;
        this.timestamp = timestamp;
    }

    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getAdditionalRemarks() { return additionalRemarks; }
    public String getTimestamp() { return timestamp; }
}