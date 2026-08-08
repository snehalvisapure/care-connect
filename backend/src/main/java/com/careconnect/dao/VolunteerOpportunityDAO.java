package com.careconnect.dao;

import com.careconnect.model.OpportunityStatus;
import com.careconnect.model.VolunteerOpportunity;
import com.careconnect.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the `volunteer_opportunities` table. Core CRUD only.
 * Each method opens its own Connection via DBConnection.getConnection()
 * and closes it automatically (try-with-resources).
 */
public class VolunteerOpportunityDAO {

    /**
     * Inserts a new volunteer opportunity.
     * Expects institution_id, title, description, required_skills, event_date,
     * start_time, end_time, location, max_volunteers to be set on the object.
     * status defaults to OPEN (set by the model's short constructor, or the DB default).
     * On success, sets the generated opportunity_id back onto the passed-in object.
     *
     * @return true if the insert affected exactly 1 row.
     */
    public boolean insert(VolunteerOpportunity opportunity) throws SQLException {
        String sql = "INSERT INTO volunteer_opportunities " +
                "(institution_id, title, description, required_skills, event_date, " +
                "start_time, end_time, location, max_volunteers, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, opportunity.getInstitutionId());
            stmt.setString(2, opportunity.getTitle());
            stmt.setString(3, opportunity.getDescription());
            stmt.setString(4, opportunity.getRequiredSkills());
            stmt.setDate(5, opportunity.getEventDate());
            stmt.setTime(6, opportunity.getStartTime());
            stmt.setTime(7, opportunity.getEndTime());
            stmt.setString(8, opportunity.getLocation());
            setNullableInt(stmt, 9, opportunity.getMaxVolunteers());
            OpportunityStatus status = opportunity.getStatus() != null
                    ? opportunity.getStatus() : OpportunityStatus.OPEN;
            stmt.setString(10, status.name());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected == 1) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        opportunity.setOpportunityId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
            return false;
        }
    }

    /**
     * Finds an opportunity by its opportunity_id.
     *
     * @return the VolunteerOpportunity, or null if no row matches.
     */
    public VolunteerOpportunity findById(int opportunityId) throws SQLException {
        String sql = "SELECT * FROM volunteer_opportunities WHERE opportunity_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, opportunityId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToOpportunity(rs);
                }
                return null;
            }
        }
    }

    /**
     * Returns all volunteer opportunities.
     */
    public List<VolunteerOpportunity> findAll() throws SQLException {
        String sql = "SELECT * FROM volunteer_opportunities";
        List<VolunteerOpportunity> opportunities = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                opportunities.add(mapRowToOpportunity(rs));
            }
        }
        return opportunities;
    }

    /**
     * Updates the editable fields of an existing opportunity
     * (title, description, required_skills, event_date, start_time, end_time,
     * location, max_volunteers, status). institution_id and opportunity_id are
     * not changed here.
     *
     * @return true if the update affected exactly 1 row.
     */
    public boolean update(VolunteerOpportunity opportunity) throws SQLException {
        String sql = "UPDATE volunteer_opportunities SET title = ?, description = ?, " +
                "required_skills = ?, event_date = ?, start_time = ?, end_time = ?, " +
                "location = ?, max_volunteers = ?, status = ? WHERE opportunity_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, opportunity.getTitle());
            stmt.setString(2, opportunity.getDescription());
            stmt.setString(3, opportunity.getRequiredSkills());
            stmt.setDate(4, opportunity.getEventDate());
            stmt.setTime(5, opportunity.getStartTime());
            stmt.setTime(6, opportunity.getEndTime());
            stmt.setString(7, opportunity.getLocation());
            setNullableInt(stmt, 8, opportunity.getMaxVolunteers());
            stmt.setString(9, opportunity.getStatus().name());
            stmt.setInt(10, opportunity.getOpportunityId());

            return stmt.executeUpdate() == 1;
        }
    }

    /**
     * Deletes an opportunity by opportunity_id.
     * Note: this will cascade-delete its volunteer_registrations too,
     * per the ON DELETE CASCADE foreign key in the schema.
     *
     * @return true if the delete affected exactly 1 row.
     */
    public boolean delete(int opportunityId) throws SQLException {
        String sql = "DELETE FROM volunteer_opportunities WHERE opportunity_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, opportunityId);
            return stmt.executeUpdate() == 1;
        }
    }

    /**
     * Helper to set an Integer parameter that might be null (max_volunteers is nullable).
     */
    private void setNullableInt(PreparedStatement stmt, int paramIndex, Integer value) throws SQLException {
        if (value == null) {
            stmt.setNull(paramIndex, Types.INTEGER);
        } else {
            stmt.setInt(paramIndex, value);
        }
    }

    /**
     * Maps the current row of a ResultSet to a VolunteerOpportunity object.
     */
    private VolunteerOpportunity mapRowToOpportunity(ResultSet rs) throws SQLException {
        VolunteerOpportunity opportunity = new VolunteerOpportunity();
        opportunity.setOpportunityId(rs.getInt("opportunity_id"));
        opportunity.setInstitutionId(rs.getInt("institution_id"));
        opportunity.setTitle(rs.getString("title"));
        opportunity.setDescription(rs.getString("description"));
        opportunity.setRequiredSkills(rs.getString("required_skills"));

        Date eventDate = rs.getDate("event_date");
        opportunity.setEventDate(eventDate);

        Time startTime = rs.getTime("start_time");
        opportunity.setStartTime(startTime);

        Time endTime = rs.getTime("end_time");
        opportunity.setEndTime(endTime);

        opportunity.setLocation(rs.getString("location"));

        int maxVolunteers = rs.getInt("max_volunteers");
        opportunity.setMaxVolunteers(rs.wasNull() ? null : maxVolunteers);

        String statusStr = rs.getString("status");
        opportunity.setStatus(statusStr != null ? OpportunityStatus.valueOf(statusStr) : null);

        Timestamp createdAt = rs.getTimestamp("created_at");
        opportunity.setCreatedAt(createdAt);

        return opportunity;
    }
}
