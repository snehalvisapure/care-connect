package com.careconnect.model;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

/**
 * Represents a row in the `volunteer_opportunities` table.
 * Created by an Institution; Volunteers register against it.
 */
public class VolunteerOpportunity {

    private int opportunityId;
    private int institutionId;
    private String title;
    private String description;
    private String requiredSkills;
    private Date eventDate;
    private Time startTime;
    private Time endTime;
    private String location;
    private Integer maxVolunteers;   // nullable -> Integer, not int
    private OpportunityStatus status; // defaults to OPEN in DB
    private Timestamp createdAt;

    public VolunteerOpportunity() {
    }

    public VolunteerOpportunity(int opportunityId, int institutionId, String title, String description,
                                 String requiredSkills, Date eventDate, Time startTime, Time endTime,
                                 String location, Integer maxVolunteers, OpportunityStatus status,
                                 Timestamp createdAt) {
        this.opportunityId = opportunityId;
        this.institutionId = institutionId;
        this.title = title;
        this.description = description;
        this.requiredSkills = requiredSkills;
        this.eventDate = eventDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.location = location;
        this.maxVolunteers = maxVolunteers;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Used when creating a new opportunity (before opportunityId/createdAt/status are assigned)
    public VolunteerOpportunity(int institutionId, String title, String description, String requiredSkills,
                                 Date eventDate, Time startTime, Time endTime, String location,
                                 Integer maxVolunteers) {
        this.institutionId = institutionId;
        this.title = title;
        this.description = description;
        this.requiredSkills = requiredSkills;
        this.eventDate = eventDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.location = location;
        this.maxVolunteers = maxVolunteers;
        this.status = OpportunityStatus.OPEN;
    }

    public int getOpportunityId() {
        return opportunityId;
    }

    public void setOpportunityId(int opportunityId) {
        this.opportunityId = opportunityId;
    }

    public int getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(int institutionId) {
        this.institutionId = institutionId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRequiredSkills() {
        return requiredSkills;
    }

    public void setRequiredSkills(String requiredSkills) {
        this.requiredSkills = requiredSkills;
    }

    public Date getEventDate() {
        return eventDate;
    }

    public void setEventDate(Date eventDate) {
        this.eventDate = eventDate;
    }

    public Time getStartTime() {
        return startTime;
    }

    public void setStartTime(Time startTime) {
        this.startTime = startTime;
    }

    public Time getEndTime() {
        return endTime;
    }

    public void setEndTime(Time endTime) {
        this.endTime = endTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getMaxVolunteers() {
        return maxVolunteers;
    }

    public void setMaxVolunteers(Integer maxVolunteers) {
        this.maxVolunteers = maxVolunteers;
    }

    public OpportunityStatus getStatus() {
        return status;
    }

    public void setStatus(OpportunityStatus status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "VolunteerOpportunity{" +
                "opportunityId=" + opportunityId +
                ", institutionId=" + institutionId +
                ", title='" + title + '\'' +
                ", eventDate=" + eventDate +
                ", status=" + status +
                ", maxVolunteers=" + maxVolunteers +
                '}';
    }
}
