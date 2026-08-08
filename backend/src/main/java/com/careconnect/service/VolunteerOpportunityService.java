package com.careconnect.service;

import com.careconnect.dao.VolunteerOpportunityDAO;
import com.careconnect.model.OpportunityStatus;
import com.careconnect.model.VolunteerOpportunity;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for volunteer opportunities.
 * Sits between the Controller and the DAO - the Controller should call
 * this class, never VolunteerOpportunityDAO directly.
 */
public class VolunteerOpportunityService {

    private final VolunteerOpportunityDAO opportunityDAO;

    public VolunteerOpportunityService() {
        this.opportunityDAO = new VolunteerOpportunityDAO();
    }

    /**
     * Creates a new volunteer opportunity, posted by an institution.
     * Always starts as OPEN, regardless of what's passed in.
     */
    public VolunteerOpportunity createOpportunity(int institutionId, String title, String description,
                                                   String requiredSkills, Date eventDate, Time startTime,
                                                   Time endTime, String location, Integer maxVolunteers)
            throws SQLException {

        VolunteerOpportunity opportunity = new VolunteerOpportunity(
                institutionId, title, description, requiredSkills,
                eventDate, startTime, endTime, location, maxVolunteers);
        // constructor already defaults status to OPEN

        opportunityDAO.insert(opportunity);
        return opportunity;
    }

    /**
     * Fetches an opportunity by opportunity_id.
     *
     * @throws IllegalArgumentException if no such opportunity exists.
     */
    public VolunteerOpportunity getOpportunity(int opportunityId) throws SQLException {
        VolunteerOpportunity opportunity = opportunityDAO.findById(opportunityId);
        if (opportunity == null) {
            throw new IllegalArgumentException("No opportunity found with opportunity_id=" + opportunityId);
        }
        return opportunity;
    }

    /**
     * Returns every opportunity, regardless of status.
     * Useful for an institution's own dashboard (they'd want to see
     * CLOSED/COMPLETED ones too, not just OPEN).
     */
    public List<VolunteerOpportunity> listAllOpportunities() throws SQLException {
        return opportunityDAO.findAll();
    }

    /**
     * Returns only OPEN opportunities.
     * This is the list a volunteer browsing the public site should see -
     * matches the "discovery-first" principle: this stays publicly readable,
     * no login required to call this method.
     */
    public List<VolunteerOpportunity> listOpenOpportunities() throws SQLException {
        return opportunityDAO.findAll().stream()
                .filter(o -> o.getStatus() == OpportunityStatus.OPEN)
                .collect(Collectors.toList());
    }

    /**
     * Returns all opportunities posted by a specific institution.
     */
    public List<VolunteerOpportunity> listByInstitution(int institutionId) throws SQLException {
        return opportunityDAO.findAll().stream()
                .filter(o -> o.getInstitutionId() == institutionId)
                .collect(Collectors.toList());
    }

    /**
     * Updates the editable details of an opportunity. Does not change status -
     * use closeOpportunity() / reopenOpportunity() / markCompleted() for that.
     *
     * @throws IllegalArgumentException if no such opportunity exists.
     */
    public void updateOpportunityDetails(int opportunityId, String title, String description,
                                          String requiredSkills, Date eventDate, Time startTime,
                                          Time endTime, String location, Integer maxVolunteers)
            throws SQLException {

        VolunteerOpportunity existing = opportunityDAO.findById(opportunityId);
        if (existing == null) {
            throw new IllegalArgumentException("No opportunity found with opportunity_id=" + opportunityId);
        }

        existing.setTitle(title);
        existing.setDescription(description);
        existing.setRequiredSkills(requiredSkills);
        existing.setEventDate(eventDate);
        existing.setStartTime(startTime);
        existing.setEndTime(endTime);
        existing.setLocation(location);
        existing.setMaxVolunteers(maxVolunteers);
        // status untouched deliberately

        opportunityDAO.update(existing);
    }

    /**
     * Closes an opportunity (stops accepting new applications).
     * Business rule: only an OPEN opportunity can be closed.
     *
     * @throws IllegalArgumentException if no such opportunity exists.
     * @throws IllegalStateException if the opportunity isn't currently OPEN.
     */
    public void closeOpportunity(int opportunityId) throws SQLException {
        VolunteerOpportunity opportunity = getOpportunity(opportunityId);
        if (opportunity.getStatus() != OpportunityStatus.OPEN) {
            throw new IllegalStateException(
                    "Only an OPEN opportunity can be closed. Current status: " + opportunity.getStatus());
        }
        opportunity.setStatus(OpportunityStatus.CLOSED);
        opportunityDAO.update(opportunity);
    }

    /**
     * Marks an opportunity as COMPLETED (the event has taken place).
     * Business rule: cannot mark a still-OPEN opportunity as completed -
     * close it first. This nudges institutions to stop new applications
     * before wrapping up the event.
     *
     * @throws IllegalArgumentException if no such opportunity exists.
     * @throws IllegalStateException if the opportunity is still OPEN.
     */
    public void markCompleted(int opportunityId) throws SQLException {
        VolunteerOpportunity opportunity = getOpportunity(opportunityId);
        if (opportunity.getStatus() == OpportunityStatus.OPEN) {
            throw new IllegalStateException("Close the opportunity before marking it completed.");
        }
        opportunity.setStatus(OpportunityStatus.COMPLETED);
        opportunityDAO.update(opportunity);
    }

    /**
     * Deletes an opportunity entirely.
     * Note: cascades to delete its registrations too (DB-enforced).
     *
     * @throws IllegalArgumentException if no such opportunity exists.
     */
    public void deleteOpportunity(int opportunityId) throws SQLException {
        boolean deleted = opportunityDAO.delete(opportunityId);
        if (!deleted) {
            throw new IllegalArgumentException("No opportunity found with opportunity_id=" + opportunityId);
        }
    }
}
