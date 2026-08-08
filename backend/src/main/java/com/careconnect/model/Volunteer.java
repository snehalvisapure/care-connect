package com.careconnect.model;

import java.sql.Timestamp;

/**
 * Represents a row in the `volunteers` table.
 * A Volunteer profile has a 1:1 relationship with a User (user_id is UNIQUE).
 */
public class Volunteer {

    private int volunteerId;
    private int userId;
    private String skills;         // free text, e.g. "cooking, teaching, driving"
    private String availability;   // free text, e.g. "weekends, evenings"
    private String experience;     // free text
    private Timestamp createdAt;

    public Volunteer() {
    }

    public Volunteer(int volunteerId, int userId, String skills, String availability,
                      String experience, Timestamp createdAt) {
        this.volunteerId = volunteerId;
        this.userId = userId;
        this.skills = skills;
        this.availability = availability;
        this.experience = experience;
        this.createdAt = createdAt;
    }

    // Used when creating a new volunteer profile (before volunteerId/createdAt are assigned by DB)
    public Volunteer(int userId, String skills, String availability, String experience) {
        this.userId = userId;
        this.skills = skills;
        this.availability = availability;
        this.experience = experience;
    }

    public int getVolunteerId() {
        return volunteerId;
    }

    public void setVolunteerId(int volunteerId) {
        this.volunteerId = volunteerId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Volunteer{" +
                "volunteerId=" + volunteerId +
                ", userId=" + userId +
                ", skills='" + skills + '\'' +
                ", availability='" + availability + '\'' +
                ", experience='" + experience + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
