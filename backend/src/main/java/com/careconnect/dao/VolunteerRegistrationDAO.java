package com.careconnect.dao;

import com.careconnect.model.RegistrationStatus;
import com.careconnect.model.VolunteerRegistration;
import com.careconnect.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the `volunteer_registrations` table. Core CRUD only.
 * Each method opens its own Connection via DBConnection.getConnection()
 * and closes it automatically (try-with-resources).
 *
 * Note: the DB enforces UNIQUE(opportunity_id, volunteer_id), so calling
 * insert() with a duplicate pair will throw a SQLException (MySQL error
 * code 1062, Duplicate entry). The Service layer should catch this and
 * turn it into a friendly "you've already registered" message.
 */
public class VolunteerRegistrationDAO {

    /**
     * Inserts a new registration (a volunteer applying to an opportunity).
     * Expects registration.getOpportunityId() and getVolunteerId() to be set.
     * status defaults to APPLIED (set by the model's short constructor, or the DB default).
     * On success, sets the generated registration_id back onto the passed-in object.
     *
     * @return true if the insert affected exactly 1 row.
     * @throws SQLException if the (opportunity_id, volunteer_id) pair already exists,
     *         or either foreign key doesn't exist.
     */
    public boolean insert(VolunteerRegistration registration) throws SQLException {
        String sql = "INSERT INTO volunteer_registrations (opportunity_id, volunteer_id, status) " +
                "VALUES (?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, registration.getOpportunityId());
            stmt.setInt(2, registration.getVolunteerId());
            RegistrationStatus status = registration.getStatus() != null
                    ? registration.getStatus() : RegistrationStatus.APPLIED;
            stmt.setString(3, status.name());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected == 1) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        registration.setRegistrationId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
            return false;
        }
    }

    /**
     * Finds a registration by its registration_id.
     *
     * @return the VolunteerRegistration, or null if no row matches.
     */
    public VolunteerRegistration findById(int registrationId) throws SQLException {
        String sql = "SELECT * FROM volunteer_registrations WHERE registration_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, registrationId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToRegistration(rs);
                }
                return null;
            }
        }
    }

    /**
     * Returns all volunteer registrations.
     */
    public List<VolunteerRegistration> findAll() throws SQLException {
        String sql = "SELECT * FROM volunteer_registrations";
        List<VolunteerRegistration> registrations = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                registrations.add(mapRowToRegistration(rs));
            }
        }
        return registrations;
    }

    /**
     * Updates the status of an existing registration
     * (e.g. APPLIED -> ACCEPTED, ACCEPTED -> COMPLETED).
     * opportunity_id and volunteer_id are not changed here - if the pairing
     * is wrong, delete and re-insert instead.
     *
     * @return true if the update affected exactly 1 row.
     */
    public boolean update(VolunteerRegistration registration) throws SQLException {
        String sql = "UPDATE volunteer_registrations SET status = ? WHERE registration_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, registration.getStatus().name());
            stmt.setInt(2, registration.getRegistrationId());

            return stmt.executeUpdate() == 1;
        }
    }

    /**
     * Deletes a registration by registration_id (e.g. a volunteer withdrawing
     * their application).
     *
     * @return true if the delete affected exactly 1 row.
     */
    public boolean delete(int registrationId) throws SQLException {
        String sql = "DELETE FROM volunteer_registrations WHERE registration_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, registrationId);
            return stmt.executeUpdate() == 1;
        }
    }

    /**
     * Maps the current row of a ResultSet to a VolunteerRegistration object.
     */
    private VolunteerRegistration mapRowToRegistration(ResultSet rs) throws SQLException {
        VolunteerRegistration registration = new VolunteerRegistration();
        registration.setRegistrationId(rs.getInt("registration_id"));
        registration.setOpportunityId(rs.getInt("opportunity_id"));
        registration.setVolunteerId(rs.getInt("volunteer_id"));

        String statusStr = rs.getString("status");
        registration.setStatus(statusStr != null ? RegistrationStatus.valueOf(statusStr) : null);

        Timestamp registeredAt = rs.getTimestamp("registered_at");
        registration.setRegisteredAt(registeredAt);

        return registration;
    }
}
