package com.example.detectapplication2;

public class Pothole {
    private double latitude;
    private double longitude;
    private String address;
    private String userId;
    private String level; // "light", "medium", "heavy"

    public Pothole() { }

    public Pothole(double latitude, double longitude, String address, String userId, String level) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.userId = userId;
        this.level = level;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }
}

