package com.example.fishertech;

public class ForumPost {
    private String name;
    private String message;

    public ForumPost(String name, String message) {
        this.name = name;
        this.message = message;
    }

    public String getName() { return name; }
    public String getMessage() { return message; }
}