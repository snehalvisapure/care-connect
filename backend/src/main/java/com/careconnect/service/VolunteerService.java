package com.careconnect.service;

import com.careconnect.dao.VolunteerDAO;
import com.careconnect.model.Volunteer;

import java.sql.SQLException;
import java.util.List;

/**
 * Business logic for volunteer profiles.
 * Sits between the Controller and the DAO - the Controller should call
 * this class, never VolunteerDAO directly.
 */
public class VolunteerService {

    private final VolunteerDAO volunteerDAO;

    public VolunteerService() {
        this.volunteerDAO = new VolunteerDAO();
    }

    /**
     * Creates a new volunteer profile for a user.
     * Business rule: a user can only have ONE volunteer profile
     * (matches the UNIQUE constraint on volunteers.user_id).
     *
     * @throws IllegalStateException if this user already has a volunteer profile.
     */
    public Volunteer createProfile(int userId, String skills, String availability, String experience)
            throws SQLException {

        Volunteer existing = volunteerDAO.findByUserId(userId);
        if (existing != null) {
            throw new IllegalStateException(
                    "This user already has a volunteer profile (volunteer_id=" + existing.getVolunteerId() + ")");
        }

        Volunteer volunteer = new Volunteer(userId, skills, availability, experience);
        volunteerDAO.insert(volunteer);
        return volunteer;
    }

    /**
     * Fetches a volunteer profile by volunteer_id.
     *
     * @throws IllegalArgumentException if no such profile exists.
     */
    public Volunteer getProfile(int volunteerId) throws SQLException {
        Volunteer volunteer = volunteerDAO.findById(volunteerId);
        if (volunteer == null) {
            throw new IllegalArgumentException("No volunteer found with volunteer_id=" + volunteerId);
        }
        return volunteer;
    }

    /**
     * Fetches a volunteer profile by the linked user_id.
     * Returns null if this user hasn't created a volunteer profile
     * (this is a normal, expected case - e.g. checking whether to show
     * "Become a Volunteer" vs "My Volunteer Profile" on the frontend).
     */
    public Volunteer getProfileByUserId(int userId) throws SQLException {
        return volunteerDAO.findByUserId(userId);
    }

    /**
     * Returns every volunteer profile in the system.
     */
    public List<Volunteer> listAllVolunteers() throws SQLException {
        return volunteerDAO.findAll();
    }

    /**
     * Updates the editable fields (skills, availability, experience) of a
     * volunteer's profile.
     *
     * @throws IllegalArgumentException if no such profile exists.
     */
    public void updateProfile(int volunteerId, String skills, String availability, String experience)
            throws SQLException {

        Volunteer existing = volunteerDAO.findById(volunteerId);
        if (existing == null) {
            throw new IllegalArgumentException("No volunteer found with volunteer_id=" + volunteerId);
        }

        existing.setSkills(skills);
        existing.setAvailability(availability);
        existing.setExperience(experience);
        volunteerDAO.update(existing);
    }

    /**
     * Deletes a volunteer's profile entirely.
     * Note: cascades to delete their registrations too (DB-enforced).
     *
     * @throws IllegalArgumentException if no such profile exists.
     */
    public void deleteProfile(int volunteerId) throws SQLException {
        boolean deleted = volunteerDAO.delete(volunteerId);
        if (!deleted) {
            throw new IllegalArgumentException("No volunteer found with volunteer_id=" + volunteerId);
        }
    }
}
