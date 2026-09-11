package com.example.fishertech;

public class ReportPost {
    String name;
    String category;
    String description;
    String additionalRemarks;

    public ReportPost(String name, String category, String description, String additionalRemarks) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.additionalRemarks = additionalRemarks;
    }

    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getAdditionalRemarks() { return additionalRemarks; }
}