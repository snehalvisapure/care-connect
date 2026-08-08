package com.careconnect.service;

import com.careconnect.dao.EventDAO;
import com.careconnect.model.Event;
import com.careconnect.model.EventCategory;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for events.
 * Sits between the Controller and the DAO - the Controller should call
 * this class, never EventDAO directly.
 */
public class EventService {

    private final EventDAO eventDAO;

    public EventService() {
        this.eventDAO = new EventDAO();
    }

    /**
     * Creates a new event, posted by an institution.
     */
    public Event createEvent(int institutionId, String title, String description, Date eventDate,
                              Time eventTime, String location, EventCategory category) throws SQLException {

        Event event = new Event(institutionId, title, description, eventDate, eventTime, location, category);
        eventDAO.insert(event);
        return event;
    }

    /**
     * Fetches an event by event_id.
     *
     * @throws IllegalArgumentException if no such event exists.
     */
    public Event getEvent(int eventId) throws SQLException {
        Event event = eventDAO.findById(eventId);
        if (event == null) {
            throw new IllegalArgumentException("No event found with event_id=" + eventId);
        }
        return event;
    }

    /**
     * Returns all events, sorted soonest-first by date.
     * This is what the public calendar page should call - matches the
     * "discovery-first" principle, no login required to call this method.
     */
    public List<Event> listAllEvents() throws SQLException {
        return eventDAO.findAll().stream()
                .sorted(Comparator.comparing(Event::getEventDate))
                .collect(Collectors.toList());
    }

    /**
     * Returns only upcoming events (event_date is today or later), sorted
     * soonest-first. Useful for a "what's coming up" view that hides past events.
     */
    public List<Event> listUpcomingEvents() throws SQLException {
        Date today = new Date(System.currentTimeMillis());
        return eventDAO.findAll().stream()
                .filter(e -> !e.getEventDate().before(today))
                .sorted(Comparator.comparing(Event::getEventDate))
                .collect(Collectors.toList());
    }

    /**
     * Returns all events, filtered to a single category (e.g. only FESTIVAL
     * events), sorted soonest-first. Supports the calendar's category filter.
     */
    public List<Event> listByCategory(EventCategory category) throws SQLException {
        return eventDAO.findAll().stream()
                .filter(e -> e.getCategory() == category)
                .sorted(Comparator.comparing(Event::getEventDate))
                .collect(Collectors.toList());
    }

    /**
     * Returns all events posted by a specific institution, sorted soonest-first.
     */
    public List<Event> listByInstitution(int institutionId) throws SQLException {
        return eventDAO.findByInstitutionId(institutionId).stream()
                .sorted(Comparator.comparing(Event::getEventDate))
                .collect(Collectors.toList());
    }

    /**
     * Updates the editable details of an event.
     *
     * @throws IllegalArgumentException if no such event exists.
     */
    public void updateEvent(int eventId, String title, String description, Date eventDate,
                             Time eventTime, String location, EventCategory category) throws SQLException {

        Event existing = eventDAO.findById(eventId);
        if (existing == null) {
            throw new IllegalArgumentException("No event found with event_id=" + eventId);
        }

        existing.setTitle(title);
        existing.setDescription(description);
        existing.setEventDate(eventDate);
        existing.setEventTime(eventTime);
        existing.setLocation(location);
        existing.setCategory(category);

        eventDAO.update(existing);
    }

    /**
     * Deletes an event entirely.
     * Note: cascades to delete its RSVPs too (DB-enforced).
     *
     * @throws IllegalArgumentException if no such event exists.
     */
    public void deleteEvent(int eventId) throws SQLException {
        boolean deleted = eventDAO.delete(eventId);
        if (!deleted) {
            throw new IllegalArgumentException("No event found with event_id=" + eventId);
        }
    }
}
