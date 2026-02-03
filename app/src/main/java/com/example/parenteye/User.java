package com.example.parenteye;

public class User {

    private String email;
    private String password;
    private String phone;
    private String id;
    private boolean isParent;

    public User(){

    }
    public User(String email, String password, String phone, String id, boolean isParent) {
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.id = id;
        this.isParent = isParent;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isParent() {
        return isParent;
    }

    public void setParent(boolean parent) {
        isParent = parent;
    }
}
