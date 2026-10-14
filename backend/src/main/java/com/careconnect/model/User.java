package com.careconnect.model;

import java.sql.Timestamp;

/**
 * PLACEHOLDER model, created by Member 3 (Nehaa) on 2026-08-09 to support
 * the Admin module's "view users" feature, because Member 1 had not yet
 * built a User model/DAO for User Management.
 *
 * This is intentionally READ-ONLY - Admin only needs to view users, not
 * create/edit them (that's Member 1's User Management module). If/when
 * Member 1 delivers their own User model, reconcile the two - this one
 * can likely just be deleted in favour of theirs.
 *
 * Mirrors the `users` table.
 *
 * SECURITY NOTE: password is marked `transient` so Gson skips it when
 * serializing to JSON - an admin "view users" endpoint should never send
 * password hashes back over the API, even internally.
 */
public class User {

    private int userId;
    private String fullName;
    private String email;
    private transient String password; // never serialized to JSON - see note above
    private String phone;
    private UserRole role;
    private String address;
    private String profilePhoto;
    private Timestamp createdAt;

    public User() {
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
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

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getProfilePhoto() {
        return profilePhoto;
    }

    public void setProfilePhoto(String profilePhoto) {
        this.profilePhoto = profilePhoto;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                '}';
    }
}
