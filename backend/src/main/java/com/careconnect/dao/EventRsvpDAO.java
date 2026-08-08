package com.careconnect.dao;

import com.careconnect.model.EventRsvp;
import com.careconnect.model.RsvpResponse;
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
 * DAO for the `event_rsvp` table. Core CRUD only.
 * Each method opens its own Connection via DBConnection.getConnection()
 * and closes it automatically (try-with-resources).
 *
 * Note: the DB enforces UNIQUE(event_id, user_id), so calling insert() with
 * a duplicate pair will throw a SQLException (MySQL error code 1062,
 * Duplicate entry). The Service layer should catch this - though for RSVPs,
 * the more natural behaviour is usually "update the existing RSVP" rather
 * than reject a repeat call, since users change their mind (INTERESTED ->
 * GOING, etc). See findByEventAndUser() below, used for that upsert pattern.
 */
public class EventRsvpDAO {

    /**
     * Inserts a new RSVP.
     * Expects rsvp.getEventId(), getUserId(), getResponse() to be set.
     * On success, sets the generated rsvp_id back onto the passed-in object.
     *
     * @return true if the insert affected exactly 1 row.
     * @throws SQLException if this (event_id, user_id) pair already has an RSVP,
     *         or either foreign key doesn't exist.
     */
    public boolean insert(EventRsvp rsvp) throws SQLException {
        String sql = "INSERT INTO event_rsvp (event_id, user_id, response) VALUES (?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, rsvp.getEventId());
            stmt.setInt(2, rsvp.getUserId());
            RsvpResponse response = rsvp.getResponse() != null ? rsvp.getResponse() : RsvpResponse.INTERESTED;
            stmt.setString(3, response.name());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected == 1) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        rsvp.setRsvpId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
            return false;
        }
    }

    /**
     * Finds an RSVP by its rsvp_id.
     *
     * @return the EventRsvp, or null if no row matches.
     */
    public EventRsvp findById(int rsvpId) throws SQLException {
        String sql = "SELECT * FROM event_rsvp WHERE rsvp_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, rsvpId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToRsvp(rs);
                }
                return null;
            }
        }
    }

    /**
     * Finds the RSVP for a specific (event_id, user_id) pair, if one exists.
     * Useful for the Service layer to implement "upsert" behaviour -
     * check if this user already RSVP'd before deciding insert vs update.
     *
     * @return the EventRsvp, or null if this user hasn't RSVP'd to this event.
     */
    public EventRsvp findByEventAndUser(int eventId, int userId) throws SQLException {
        String sql = "SELECT * FROM event_rsvp WHERE event_id = ? AND user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, eventId);
            stmt.setInt(2, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToRsvp(rs);
                }
                return null;
            }
        }
    }

    /**
     * Returns all RSVPs.
     */
    public List<EventRsvp> findAll() throws SQLException {
        String sql = "SELECT * FROM event_rsvp";
        List<EventRsvp> rsvps = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                rsvps.add(mapRowToRsvp(rs));
            }
        }
        return rsvps;
    }

    /**
     * Returns all RSVPs for a specific event (e.g. for an institution to see
     * who's coming).
     */
    public List<EventRsvp> findByEventId(int eventId) throws SQLException {
        String sql = "SELECT * FROM event_rsvp WHERE event_id = ?";
        List<EventRsvp> rsvps = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, eventId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rsvps.add(mapRowToRsvp(rs));
                }
            }
        }
        return rsvps;
    }

    /**
     * Returns all RSVPs made by a specific user (e.g. for a "My Events" page).
     */
    public List<EventRsvp> findByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM event_rsvp WHERE user_id = ?";
        List<EventRsvp> rsvps = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rsvps.add(mapRowToRsvp(rs));
                }
            }
        }
        return rsvps;
    }

    /**
     * Updates the response of an existing RSVP (e.g. INTERESTED -> GOING).
     * event_id and user_id are not changed here.
     *
     * @return true if the update affected exactly 1 row.
     */
    public boolean update(EventRsvp rsvp) throws SQLException {
        String sql = "UPDATE event_rsvp SET response = ? WHERE rsvp_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, rsvp.getResponse().name());
            stmt.setInt(2, rsvp.getRsvpId());

            return stmt.executeUpdate() == 1;
        }
    }

    /**
     * Deletes an RSVP by rsvp_id (a user removing their RSVP entirely).
     *
     * @return true if the delete affected exactly 1 row.
     */
    public boolean delete(int rsvpId) throws SQLException {
        String sql = "DELETE FROM event_rsvp WHERE rsvp_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, rsvpId);
            return stmt.executeUpdate() == 1;
        }
    }

    /**
     * Maps the current row of a ResultSet to an EventRsvp object.
     */
    private EventRsvp mapRowToRsvp(ResultSet rs) throws SQLException {
        EventRsvp rsvp = new EventRsvp();
        rsvp.setRsvpId(rs.getInt("rsvp_id"));
        rsvp.setEventId(rs.getInt("event_id"));
        rsvp.setUserId(rs.getInt("user_id"));

        String responseStr = rs.getString("response");
        rsvp.setResponse(responseStr != null ? RsvpResponse.valueOf(responseStr) : null);

        Timestamp createdAt = rs.getTimestamp("created_at");
        rsvp.setCreatedAt(createdAt);

        return rsvp;
    }
}
