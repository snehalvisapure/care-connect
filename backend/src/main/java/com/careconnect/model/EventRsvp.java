package com.careconnect.model;

import java.sql.Timestamp;

/**
 * Represents a row in the `event_rsvp` table.
 * Links a User (any role, not just volunteers) to an Event.
 * DB enforces UNIQUE(event_id, user_id) - a user can only RSVP once per
 * event (changing response is an UPDATE, not a new row).
 */
public class EventRsvp {

    private int rsvpId;
    private int eventId;
    private int userId;
    private RsvpResponse response; // defaults to INTERESTED in DB
    private Timestamp createdAt;

    public EventRsvp() {
    }

    public EventRsvp(int rsvpId, int eventId, int userId, RsvpResponse response, Timestamp createdAt) {
        this.rsvpId = rsvpId;
        this.eventId = eventId;
        this.userId = userId;
        this.response = response;
        this.createdAt = createdAt;
    }

    // Used when creating a new RSVP (before rsvpId/createdAt are assigned)
    public EventRsvp(int eventId, int userId, RsvpResponse response) {
        this.eventId = eventId;
        this.userId = userId;
        this.response = response != null ? response : RsvpResponse.INTERESTED;
    }

    public int getRsvpId() {
        return rsvpId;
    }

    public void setRsvpId(int rsvpId) {
        this.rsvpId = rsvpId;
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public RsvpResponse getResponse() {
        return response;
    }

    public void setResponse(RsvpResponse response) {
        this.response = response;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "EventRsvp{" +
                "rsvpId=" + rsvpId +
                ", eventId=" + eventId +
                ", userId=" + userId +
                ", response=" + response +
                ", createdAt=" + createdAt +
                '}';
    }
}
