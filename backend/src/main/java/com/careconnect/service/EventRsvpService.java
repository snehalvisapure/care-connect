package com.careconnect.service;

import com.careconnect.dao.EventDAO;
import com.careconnect.dao.EventRsvpDAO;
import com.careconnect.model.Event;
import com.careconnect.model.EventRsvp;
import com.careconnect.model.RsvpResponse;

import java.sql.SQLException;
import java.util.List;

/**
 * Business logic for event RSVPs.
 * Sits between the Controller and the DAO - the Controller should call
 * this class, never EventRsvpDAO directly.
 *
 * Key business rule: RSVPing is an UPSERT, not a strict insert-once action.
 * Unlike volunteer registrations (where a duplicate application is an error),
 * a user changing their mind about attending (INTERESTED -> GOING -> NOT_GOING)
 * is completely normal and expected. So rsvp() below checks for an existing
 * RSVP first and updates it instead of trying to insert a duplicate.
 */
public class EventRsvpService {

    private final EventRsvpDAO rsvpDAO;
    private final EventDAO eventDAO;

    public EventRsvpService() {
        this.rsvpDAO = new EventRsvpDAO();
        this.eventDAO = new EventDAO();
    }

    /**
     * Sets a user's RSVP for an event. If they haven't RSVP'd yet, creates
     * a new RSVP. If they have, updates the existing one to the new response.
     * This is the single method the Controller should call for RSVP actions -
     * it doesn't matter to the caller whether it's their first RSVP or a change.
     *
     * @throws IllegalArgumentException if the event doesn't exist.
     */
    public EventRsvp rsvp(int eventId, int userId, RsvpResponse response) throws SQLException {
        Event event = eventDAO.findById(eventId);
        if (event == null) {
            throw new IllegalArgumentException("No event found with event_id=" + eventId);
        }

        EventRsvp existing = rsvpDAO.findByEventAndUser(eventId, userId);

        if (existing != null) {
            existing.setResponse(response);
            rsvpDAO.update(existing);
            return existing;
        } else {
            EventRsvp newRsvp = new EventRsvp(eventId, userId, response);
            rsvpDAO.insert(newRsvp);
            return newRsvp;
        }
    }

    /**
     * Removes a user's RSVP entirely (they no longer want to be counted
     * in any way, as opposed to setting response to NOT_GOING which still
     * keeps a record).
     *
     * @throws IllegalArgumentException if this user has no RSVP for this event.
     */
    public void removeRsvp(int eventId, int userId) throws SQLException {
        EventRsvp existing = rsvpDAO.findByEventAndUser(eventId, userId);
        if (existing == null) {
            throw new IllegalArgumentException(
                    "No RSVP found for event_id=" + eventId + " and user_id=" + userId);
        }
        rsvpDAO.delete(existing.getRsvpId());
    }

    /**
     * Returns a user's current RSVP for an event, or null if they haven't
     * RSVP'd. Useful for the frontend to show "You're going!" vs an RSVP button.
     */
    public EventRsvp getRsvpStatus(int eventId, int userId) throws SQLException {
        return rsvpDAO.findByEventAndUser(eventId, userId);
    }

    /**
     * Returns all RSVPs for an event (e.g. for an institution to see the
     * guest list / interest level).
     */
    public List<EventRsvp> listByEvent(int eventId) throws SQLException {
        return rsvpDAO.findByEventId(eventId);
    }

    /**
     * Returns all RSVPs made by a user (e.g. for their "My Events" page).
     */
    public List<EventRsvp> listByUser(int userId) throws SQLException {
        return rsvpDAO.findByUserId(userId);
    }

    /**
     * Counts how many users responded GOING to an event.
     * Convenience method for displaying "X people are going" on the frontend.
     */
    public long countGoing(int eventId) throws SQLException {
        return rsvpDAO.findByEventId(eventId).stream()
                .filter(r -> r.getResponse() == RsvpResponse.GOING)
                .count();
    }
}
