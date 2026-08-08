package com.careconnect.model;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

/**
 * Represents a row in the `events` table.
 * Created by an Institution (birthdays, festivals, visiting days, etc.).
 * Unlike VolunteerOpportunity, events have no OPEN/CLOSED lifecycle status -
 * they simply exist on a date, and users RSVP to them via EventRsvp.
 */
public class Event {

    private int eventId;
    private int institutionId;
    private String title;
    private String description;
    private Date eventDate;
    private Time eventTime;      // nullable - some events may not have a fixed time
    private String location;
    private EventCategory category; // defaults to OTHER in DB
    private Timestamp createdAt;

    public Event() {
    }

    public Event(int eventId, int institutionId, String title, String description, Date eventDate,
                 Time eventTime, String location, EventCategory category, Timestamp createdAt) {
        this.eventId = eventId;
        this.institutionId = institutionId;
        this.title = title;
        this.description = description;
        this.eventDate = eventDate;
        this.eventTime = eventTime;
        this.location = location;
        this.category = category;
        this.createdAt = createdAt;
    }

    // Used when creating a new event (before eventId/createdAt are assigned by DB)
    public Event(int institutionId, String title, String description, Date eventDate,
                 Time eventTime, String location, EventCategory category) {
        this.institutionId = institutionId;
        this.title = title;
        this.description = description;
        this.eventDate = eventDate;
        this.eventTime = eventTime;
        this.location = location;
        this.category = category != null ? category : EventCategory.OTHER;
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
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

    public Date getEventDate() {
        return eventDate;
    }

    public void setEventDate(Date eventDate) {
        this.eventDate = eventDate;
    }

    public Time getEventTime() {
        return eventTime;
    }

    public void setEventTime(Time eventTime) {
        this.eventTime = eventTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public EventCategory getCategory() {
        return category;
    }

    public void setCategory(EventCategory category) {
        this.category = category;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Event{" +
                "eventId=" + eventId +
                ", institutionId=" + institutionId +
                ", title='" + title + '\'' +
                ", eventDate=" + eventDate +
                ", eventTime=" + eventTime +
                ", category=" + category +
                '}';
    }
}
