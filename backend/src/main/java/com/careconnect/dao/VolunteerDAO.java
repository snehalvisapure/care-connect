package com.careconnect.dao;

import com.careconnect.model.Volunteer;
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
 * DAO for the `volunteers` table. Core CRUD only.
 * Each method opens its own Connection via DBConnection.getConnection()
 * and closes it automatically (try-with-resources).
 */
public class VolunteerDAO {

    /**
     * Inserts a new volunteer profile.
     * Expects volunteer.getUserId(), getSkills(), getAvailability(), getExperience() to be set.
     * On success, sets the generated volunteer_id back onto the passed-in object.
     *
     * @return true if the insert affected exactly 1 row.
     */
    public boolean insert(Volunteer volunteer) throws SQLException {
        String sql = "INSERT INTO volunteers (user_id, skills, availability, experience) " +
                "VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, volunteer.getUserId());
            stmt.setString(2, volunteer.getSkills());
            stmt.setString(3, volunteer.getAvailability());
            stmt.setString(4, volunteer.getExperience());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected == 1) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        volunteer.setVolunteerId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
            return false;
        }
    }

    /**
     * Finds a volunteer by their volunteer_id.
     *
     * @return the Volunteer, or null if no row matches.
     */
    public Volunteer findById(int volunteerId) throws SQLException {
        String sql = "SELECT * FROM volunteers WHERE volunteer_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, volunteerId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToVolunteer(rs);
                }
                return null;
            }
        }
    }

    /**
     * Returns all volunteer profiles.
     */
    public List<Volunteer> findAll() throws SQLException {
        String sql = "SELECT * FROM volunteers";
        List<Volunteer> volunteers = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                volunteers.add(mapRowToVolunteer(rs));
            }
        }
        return volunteers;
    }

    /**
     * Updates the editable fields (skills, availability, experience) of an
     * existing volunteer profile. user_id and volunteer_id are not changed here.
     *
     * @return true if the update affected exactly 1 row.
     */
    public boolean update(Volunteer volunteer) throws SQLException {
        String sql = "UPDATE volunteers SET skills = ?, availability = ?, experience = ? " +
                "WHERE volunteer_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, volunteer.getSkills());
            stmt.setString(2, volunteer.getAvailability());
            stmt.setString(3, volunteer.getExperience());
            stmt.setInt(4, volunteer.getVolunteerId());

            return stmt.executeUpdate() == 1;
        }
    }

    /**
     * Deletes a volunteer profile by volunteer_id.
     * Note: this will cascade-delete their volunteer_registrations too,
     * per the ON DELETE CASCADE foreign key in the schema.
     *
     * @return true if the delete affected exactly 1 row.
     */
    public boolean delete(int volunteerId) throws SQLException {
        String sql = "DELETE FROM volunteers WHERE volunteer_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, volunteerId);
            return stmt.executeUpdate() == 1;
        }
    }

    /**
     * Maps the current row of a ResultSet to a Volunteer object.
     */
    private Volunteer mapRowToVolunteer(ResultSet rs) throws SQLException {
        Volunteer volunteer = new Volunteer();
        volunteer.setVolunteerId(rs.getInt("volunteer_id"));
        volunteer.setUserId(rs.getInt("user_id"));
        volunteer.setSkills(rs.getString("skills"));
        volunteer.setAvailability(rs.getString("availability"));
        volunteer.setExperience(rs.getString("experience"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        volunteer.setCreatedAt(createdAt);
        return volunteer;
    }
}
