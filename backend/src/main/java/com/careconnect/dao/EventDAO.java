package com.careconnect.dao;

import com.careconnect.model.Event;
import com.careconnect.model.EventCategory;
import com.careconnect.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the `events` table. Core CRUD only.
 * Each method opens its own Connection via DBConnection.getConnection()
 * and closes it automatically (try-with-resources).
 */
public class EventDAO {

    /**
     * Inserts a new event.
     * Expects institution_id, title, description, event_date, event_time,
     * location, category to be set on the object.
     * On success, sets the generated event_id back onto the passed-in object.
     *
     * @return true if the insert affected exactly 1 row.
     */
    public boolean insert(Event event) throws SQLException {
        String sql = "INSERT INTO events (institution_id, title, description, event_date, " +
                "event_time, location, category) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, event.getInstitutionId());
            stmt.setString(2, event.getTitle());
            stmt.setString(3, event.getDescription());
            stmt.setDate(4, event.getEventDate());
            stmt.setTime(5, event.getEventTime());
            stmt.setString(6, event.getLocation());
            EventCategory category = event.getCategory() != null ? event.getCategory() : EventCategory.OTHER;
            stmt.setString(7, category.name());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected == 1) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        event.setEventId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
            return false;
        }
    }

    /**
     * Finds an event by its event_id.
     *
     * @return the Event, or null if no row matches.
     */
    public Event findById(int eventId) throws SQLException {
        String sql = "SELECT * FROM events WHERE event_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, eventId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToEvent(rs);
                }
                return null;
            }
        }
    }

    /**
     * Returns all events.
     */
    public List<Event> findAll() throws SQLException {
        String sql = "SELECT * FROM events";
        List<Event> events = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                events.add(mapRowToEvent(rs));
            }
        }
        return events;
    }

    /**
     * Returns all events belonging to a specific institution.
     */
    public List<Event> findByInstitutionId(int institutionId) throws SQLException {
        String sql = "SELECT * FROM events WHERE institution_id = ?";
        List<Event> events = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, institutionId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    events.add(mapRowToEvent(rs));
                }
            }
        }
        return events;
    }

    /**
     * Updates the editable fields of an existing event
     * (title, description, event_date, event_time, location, category).
     * institution_id and event_id are not changed here.
     *
     * @return true if the update affected exactly 1 row.
     */
    public boolean update(Event event) throws SQLException {
        String sql = "UPDATE events SET title = ?, description = ?, event_date = ?, " +
                "event_time = ?, location = ?, category = ? WHERE event_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, event.getTitle());
            stmt.setString(2, event.getDescription());
            stmt.setDate(3, event.getEventDate());
            stmt.setTime(4, event.getEventTime());
            stmt.setString(5, event.getLocation());
            stmt.setString(6, event.getCategory().name());
            stmt.setInt(7, event.getEventId());

            return stmt.executeUpdate() == 1;
        }
    }

    /**
     * Deletes an event by event_id.
     * Note: this will cascade-delete its event_rsvp rows too,
     * per the ON DELETE CASCADE foreign key in the schema.
     *
     * @return true if the delete affected exactly 1 row.
     */
    public boolean delete(int eventId) throws SQLException {
        String sql = "DELETE FROM events WHERE event_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, eventId);
            return stmt.executeUpdate() == 1;
        }
    }

    /**
     * Maps the current row of a ResultSet to an Event object.
     */
    private Event mapRowToEvent(ResultSet rs) throws SQLException {
        Event event = new Event();
        event.setEventId(rs.getInt("event_id"));
        event.setInstitutionId(rs.getInt("institution_id"));
        event.setTitle(rs.getString("title"));
        event.setDescription(rs.getString("description"));

        Date eventDate = rs.getDate("event_date");
        event.setEventDate(eventDate);

        Time eventTime = rs.getTime("event_time");
        event.setEventTime(eventTime);

        event.setLocation(rs.getString("location"));

        String categoryStr = rs.getString("category");
        event.setCategory(categoryStr != null ? EventCategory.valueOf(categoryStr) : null);

        Timestamp createdAt = rs.getTimestamp("created_at");
        event.setCreatedAt(createdAt);

        return event;
    }
}
