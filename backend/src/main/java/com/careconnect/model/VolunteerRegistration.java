package com.careconnect.model;

import java.sql.Timestamp;

/**
 * Represents a row in the `volunteer_registrations` table.
 * Links a Volunteer to a VolunteerOpportunity.
 * DB enforces UNIQUE(opportunity_id, volunteer_id) - a volunteer can only
 * register once per opportunity.
 */
public class VolunteerRegistration {

    private int registrationId;
    private int opportunityId;
    private int volunteerId;
    private RegistrationStatus status; // defaults to APPLIED in DB
    private Timestamp registeredAt;

    public VolunteerRegistration() {
    }

    public VolunteerRegistration(int registrationId, int opportunityId, int volunteerId,
                                  RegistrationStatus status, Timestamp registeredAt) {
        this.registrationId = registrationId;
        this.opportunityId = opportunityId;
        this.volunteerId = volunteerId;
        this.status = status;
        this.registeredAt = registeredAt;
    }

    // Used when creating a new registration (before registrationId/registeredAt are assigned)
    public VolunteerRegistration(int opportunityId, int volunteerId) {
        this.opportunityId = opportunityId;
        this.volunteerId = volunteerId;
        this.status = RegistrationStatus.APPLIED;
    }

    public int getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(int registrationId) {
        this.registrationId = registrationId;
    }

    public int getOpportunityId() {
        return opportunityId;
    }

    public void setOpportunityId(int opportunityId) {
        this.opportunityId = opportunityId;
    }

    public int getVolunteerId() {
        return volunteerId;
    }

    public void setVolunteerId(int volunteerId) {
        this.volunteerId = volunteerId;
    }

    public RegistrationStatus getStatus() {
        return status;
    }

    public void setStatus(RegistrationStatus status) {
        this.status = status;
    }

    public Timestamp getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(Timestamp registeredAt) {
        this.registeredAt = registeredAt;
    }

    @Override
    public String toString() {
        return "VolunteerRegistration{" +
                "registrationId=" + registrationId +
                ", opportunityId=" + opportunityId +
                ", volunteerId=" + volunteerId +
                ", status=" + status +
                ", registeredAt=" + registeredAt +
                '}';
    }
}
