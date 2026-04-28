package com.example.fishertech;

public class ReportPost {
    String name;
    String category;
    String description;

    public ReportPost(String name, String category, String description) {
        this.name = name;
        this.category = category;
        this.description = description;
    }

    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
}