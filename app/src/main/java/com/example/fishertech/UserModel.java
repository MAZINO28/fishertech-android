package com.example.fishertech;

public class UserModel {

    private String name;
    private String email;
    private String role;
    private String phone;
    private String location;
    private String boat_number;
    private long last_login; // Idinagdag para sa timestamp
    private boolean disable;

    public UserModel() {
    }

    public UserModel(String name, String email, String role, String phone, String location, String boat_number, long last_login, boolean disable) {
        this.name = name;
        this.email = email;
        this.role = role;
        this.phone = phone;
        this.location = location;
        this.boat_number = boat_number;
        this.last_login = last_login;
        this.disable = disable;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getBoat_number() { return boat_number; }
    public void setBoat_number(String boat_number) { this.boat_number = boat_number; }

    public long getLast_login() { return last_login; }
    public void setLast_login(long last_login) { this.last_login = last_login; }

    public boolean isDisable() { return disable; }
    public void setDisable(boolean disable) { this.disable = disable; }
}