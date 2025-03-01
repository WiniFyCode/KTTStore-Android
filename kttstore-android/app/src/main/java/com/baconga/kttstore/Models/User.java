package com.baconga.kttstore.Models;

public class User {
    private long userID;
    private String fullname;
    private String email;
    private String phone;
    private String gender;
    private String role;
    private boolean isDisabled;

    public User(long userID, String fullname, String email, String phone, String gender, String role, boolean isDisabled) {
        this.userID = userID;
        this.fullname = fullname;
        this.email = email;
        this.phone = phone;
        this.gender = gender;
        this.role = role;
        this.isDisabled = isDisabled;
    }

    // Getters
    public long getUserID() { return userID; }
    public String getFullname() { return fullname; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getGender() { return gender; }
    public String getRole() { return role; }
    public boolean isDisabled() { return isDisabled; }

    // Setters
    public void setFullname(String fullname) { this.fullname = fullname; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setGender(String gender) { this.gender = gender; }
} 