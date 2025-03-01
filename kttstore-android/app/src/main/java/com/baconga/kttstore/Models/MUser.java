package com.baconga.kttstore.Models;

public class MUser {
    public String userID;
    public String fullname;
    public String gender;
    public String email;
    public String password;
    public String phone;
    public boolean isDisabled;
    public String role;
    public String lastLogin;
    public String resetPasswordToken;
    public String resetPasswordExpires;
    public int loginAttempts;
    public String lockUntil;

    public MUser(String userID, String fullname, String gender, String email, String password, String phone, boolean isDisabled, String role, String lastLogin, String resetPasswordToken, String resetPasswordExpires, int loginAttempts, String lockUntil) {
        this.userID = userID;
        this.fullname = fullname;
        this.gender = gender;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.isDisabled = isDisabled;
        this.role = role;
        this.lastLogin = lastLogin;
        this.resetPasswordToken = resetPasswordToken;
        this.resetPasswordExpires = resetPasswordExpires;
        this.loginAttempts = loginAttempts;
        this.lockUntil = lockUntil;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
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

    public boolean isDisabled() {
        return isDisabled;
    }

    public void setDisabled(boolean disabled) {
        isDisabled = disabled;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(String lastLogin) {
        this.lastLogin = lastLogin;
    }

    public String getResetPasswordToken() {
        return resetPasswordToken;
    }

    public void setResetPasswordToken(String resetPasswordToken) {
        this.resetPasswordToken = resetPasswordToken;
    }

    public String getResetPasswordExpires() {
        return resetPasswordExpires;
    }

    public void setResetPasswordExpires(String resetPasswordExpires) {
        this.resetPasswordExpires = resetPasswordExpires;
    }

    public int getLoginAttempts() {
        return loginAttempts;
    }

    public void setLoginAttempts(int loginAttempts) {
        this.loginAttempts = loginAttempts;
    }

    public String getLockUntil() {
        return lockUntil;
    }

    public void setLockUntil(String lockUntil) {
        this.lockUntil = lockUntil;
    }
}
