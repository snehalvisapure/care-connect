package com.careconnect.service;

import com.careconnect.dao.VolunteerDAO;
import com.careconnect.dao.VolunteerOpportunityDAO;
import com.careconnect.dao.VolunteerRegistrationDAO;
import com.careconnect.model.OpportunityStatus;
import com.careconnect.model.RegistrationStatus;
import com.careconnect.model.Volunteer;
import com.careconnect.model.VolunteerOpportunity;
import com.careconnect.model.VolunteerRegistration;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for volunteer registrations (a volunteer applying to an
 * opportunity, and an institution accepting/rejecting that application).
 * Sits between the Controller and the DAO - the Controller should call
 * this class, never VolunteerRegistrationDAO directly.
 *
 * Key business rules enforced here (not in the DAO):
 * 1. Can only apply to an OPEN opportunity.
 * 2. Can't apply twice to the same opportunity (DB constraint backs this up;
 *    this class turns the raw SQL error into a friendly message).
 * 3. Can't accept more volunteers than an opportunity's max_volunteers cap
 *    (if a cap is set - null means uncapped).
 */
public class VolunteerRegistrationService {

    private final VolunteerRegistrationDAO registrationDAO;
    private final VolunteerOpportunityDAO opportunityDAO;
    private final VolunteerDAO volunteerDAO;

    // MySQL error code for a UNIQUE constraint violation (duplicate entry)
    private static final int MYSQL_DUPLICATE_ENTRY_ERROR_CODE = 1062;

    public VolunteerRegistrationService() {
        this.registrationDAO = new VolunteerRegistrationDAO();
        this.opportunityDAO = new VolunteerOpportunityDAO();
        this.volunteerDAO = new VolunteerDAO();
    }

    /**
     * A volunteer applies to an opportunity.
     *
     * @throws IllegalArgumentException if the volunteer or opportunity doesn't exist.
     * @throws IllegalStateException if the opportunity isn't OPEN, or the
     *         volunteer has already applied to it.
     */
    public VolunteerRegistration applyToOpportunity(int volunteerId, int opportunityId) throws SQLException {
        Volunteer volunteer = volunteerDAO.findById(volunteerId);
        if (volunteer == null) {
            throw new IllegalArgumentException("No volunteer found with volunteer_id=" + volunteerId);
        }

        VolunteerOpportunity opportunity = opportunityDAO.findById(opportunityId);
        if (opportunity == null) {
            throw new IllegalArgumentException("No opportunity found with opportunity_id=" + opportunityId);
        }

        if (opportunity.getStatus() != OpportunityStatus.OPEN) {
            throw new IllegalStateException(
                    "Cannot apply - this opportunity is " + opportunity.getStatus() + ", not OPEN.");
        }

        VolunteerRegistration registration = new VolunteerRegistration(opportunityId, volunteerId);

        try {
            registrationDAO.insert(registration);
        } catch (SQLException e) {
            if (e.getErrorCode() == MYSQL_DUPLICATE_ENTRY_ERROR_CODE) {
                throw new IllegalStateException("You have already applied to this opportunity.");
            }
            throw e; // some other DB error - let it propagate
        }

        return registration;
    }

    /**
     * An institution accepts a volunteer's application.
     * Business rule: cannot exceed the opportunity's max_volunteers cap
     * (if one is set). Counts existing ACCEPTED registrations for this
     * opportunity before allowing another.
     *
     * @throws IllegalArgumentException if no such registration exists.
     * @throws IllegalStateException if the registration isn't currently APPLIED,
     *         or the opportunity's volunteer cap has been reached.
     */
    public void acceptRegistration(int registrationId) throws SQLException {
        VolunteerRegistration registration = registrationDAO.findById(registrationId);
        if (registration == null) {
            throw new IllegalArgumentException("No registration found with registration_id=" + registrationId);
        }

        if (registration.getStatus() != RegistrationStatus.APPLIED) {
            throw new IllegalStateException(
                    "Only an APPLIED registration can be accepted. Current status: " + registration.getStatus());
        }

        VolunteerOpportunity opportunity = opportunityDAO.findById(registration.getOpportunityId());
        Integer maxVolunteers = opportunity.getMaxVolunteers();

        if (maxVolunteers != null) {
            long acceptedCount = registrationDAO.findAll().stream()
                    .filter(r -> r.getOpportunityId() == opportunity.getOpportunityId())
                    .filter(r -> r.getStatus() == RegistrationStatus.ACCEPTED)
                    .count();

            if (acceptedCount >= maxVolunteers) {
                throw new IllegalStateException(
                        "Cannot accept - this opportunity's volunteer cap (" + maxVolunteers + ") has been reached.");
            }
        }

        registration.setStatus(RegistrationStatus.ACCEPTED);
        registrationDAO.update(registration);
    }

    /**
     * An institution rejects a volunteer's application.
     *
     * @throws IllegalArgumentException if no such registration exists.
     * @throws IllegalStateException if the registration isn't currently APPLIED.
     */
    public void rejectRegistration(int registrationId) throws SQLException {
        VolunteerRegistration registration = registrationDAO.findById(registrationId);
        if (registration == null) {
            throw new IllegalArgumentException("No registration found with registration_id=" + registrationId);
        }

        if (registration.getStatus() != RegistrationStatus.APPLIED) {
            throw new IllegalStateException(
                    "Only an APPLIED registration can be rejected. Current status: " + registration.getStatus());
        }

        registration.setStatus(RegistrationStatus.REJECTED);
        registrationDAO.update(registration);
    }

    /**
     * Marks a registration as COMPLETED (the volunteer actually showed up
     * and participated). Only an ACCEPTED registration can be completed.
     *
     * @throws IllegalArgumentException if no such registration exists.
     * @throws IllegalStateException if the registration isn't currently ACCEPTED.
     */
    public void markCompleted(int registrationId) throws SQLException {
        VolunteerRegistration registration = registrationDAO.findById(registrationId);
        if (registration == null) {
            throw new IllegalArgumentException("No registration found with registration_id=" + registrationId);
        }

        if (registration.getStatus() != RegistrationStatus.ACCEPTED) {
            throw new IllegalStateException(
                    "Only an ACCEPTED registration can be marked completed. Current status: " + registration.getStatus());
        }

        registration.setStatus(RegistrationStatus.COMPLETED);
        registrationDAO.update(registration);
    }

    /**
     * A volunteer withdraws their own application.
     * Business rule: can only withdraw while still APPLIED - once accepted,
     * treat it as a commitment (the institution may be counting on them).
     * If you want to allow withdrawing after acceptance too, relax this check.
     *
     * @throws IllegalArgumentException if no such registration exists.
     * @throws IllegalStateException if the registration is no longer APPLIED.
     */
    public void withdrawApplication(int registrationId) throws SQLException {
        VolunteerRegistration registration = registrationDAO.findById(registrationId);
        if (registration == null) {
            throw new IllegalArgumentException("No registration found with registration_id=" + registrationId);
        }

        if (registration.getStatus() != RegistrationStatus.APPLIED) {
            throw new IllegalStateException(
                    "Cannot withdraw - this application is already " + registration.getStatus() + ".");
        }

        registrationDAO.delete(registrationId);
    }

    /**
     * Returns all registrations for a given opportunity (e.g. for an
     * institution reviewing who's applied).
     */
    public List<VolunteerRegistration> listByOpportunity(int opportunityId) throws SQLException {
        return registrationDAO.findAll().stream()
                .filter(r -> r.getOpportunityId() == opportunityId)
                .collect(Collectors.toList());
    }

    /**
     * Returns all registrations for a given volunteer (e.g. for a
     * volunteer's "My Applications" page).
     */
    public List<VolunteerRegistration> listByVolunteer(int volunteerId) throws SQLException {
        return registrationDAO.findAll().stream()
                .filter(r -> r.getVolunteerId() == volunteerId)
                .collect(Collectors.toList());
    }
}
