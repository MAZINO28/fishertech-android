package com.example.fishertech;

public class SavedSpot {
    public String spotId, name, notes;
    public double latitude, longitude;
    public Long createdAt;
    public String assignedBuoy;

    public SavedSpot() {
        // Default constructor for Firebase
    }

    public SavedSpot(String spotId, String name, String notes,
                     double latitude, double longitude, Long createdAt, String assignedBuoy) {
        this.spotId = spotId;
        this.name = name;
        this.notes = notes;
        this.latitude = latitude;
        this.longitude = longitude;
        this.createdAt = createdAt;
        this.assignedBuoy = assignedBuoy;
    }
}
