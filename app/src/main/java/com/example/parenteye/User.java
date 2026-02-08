package com.example.parenteye;

public class User {

    private String email;
    private String phone;
    private String uid;
    private boolean isParent;
    private String parentUid; // מזהה ההורה (לילדים בלבד)
    private double latitude;  // קו רוחב
    private double longitude; // קו אורך
    private boolean isSosActive; // האם ה-SOS דולק

    public User() {
        // חובה עבור Firebase
    }

    // בנאי מלא
    public User(String email, String phone, String uid, boolean isParent, String parentUid) {
        this.email = email;
        this.phone = phone;
        this.uid = uid;
        this.isParent = isParent;
        this.parentUid = parentUid;
        this.isSosActive = false; // ברירת מחדל
    }

    // Getters and Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public boolean isParent() { return isParent; }
    public void setParent(boolean parent) { isParent = parent; }

    public String getParentUid() { return parentUid; }
    public void setParentUid(String parentUid) { this.parentUid = parentUid; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public boolean isSosActive() { return isSosActive; }
    public void setSosActive(boolean sosActive) { isSosActive = sosActive; }
}