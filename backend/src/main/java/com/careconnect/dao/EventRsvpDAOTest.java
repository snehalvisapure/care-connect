package com.careconnect.dao;

import com.careconnect.model.EventRsvp;
import com.careconnect.model.RsvpResponse;

import java.sql.SQLException;
import java.util.List;

/**
 * MANUAL TEST CLASS - not a real unit test, just a quick throwaway runner
 * to confirm EventRsvpDAO actually talks to MySQL correctly.
 * Safe to delete once you're confident the DAO works, or once real
 * unit tests (JUnit) are added later.
 *
 * Assumes an event already exists (run EventDAOTest first to get its
 * event_id) and user_id = 1 already exists in the `users` table.
 *
 * Update EVENT_ID below to match whatever event_id EventDAOTest printed.
 */
public class EventRsvpDAOTest {

    private static final int EVENT_ID = 1; // change this if your event got a different ID
    private static final int USER_ID = 1;

    public static void main(String[] args) {
        EventRsvpDAO dao = new EventRsvpDAO();

        try {
            // 1. Insert a new RSVP: user 1 says INTERESTED in event 1
            EventRsvp newRsvp = new EventRsvp(EVENT_ID, USER_ID, RsvpResponse.INTERESTED);

            boolean inserted = dao.insert(newRsvp);
            System.out.println("Insert successful: " + inserted);
            System.out.println("Generated rsvp_id: " + newRsvp.getRsvpId());

            // 2. Read it back by ID
            EventRsvp fetched = dao.findById(newRsvp.getRsvpId());
            System.out.println("Fetched back: " + fetched);

            // 3. Look it up by (event, user) pair - this is what the Service
            //    layer will use to decide insert vs update ("upsert")
            EventRsvp byPair = dao.findByEventAndUser(EVENT_ID, USER_ID);
            System.out.println("Found by event+user pair: " + byPair);

            // 4. Update the response: user changes mind from INTERESTED to GOING
            fetched.setResponse(RsvpResponse.GOING);
            boolean updated = dao.update(fetched);
            System.out.println("Update successful: " + updated);

            EventRsvp afterUpdate = dao.findById(fetched.getRsvpId());
            System.out.println("After update: " + afterUpdate);

            // 5. List all RSVPs for this event
            List<EventRsvp> forEvent = dao.findByEventId(EVENT_ID);
            System.out.println("RSVPs for event_id=" + EVENT_ID + ": " + forEvent.size());

            // 6. List all RSVPs by this user
            List<EventRsvp> byUser = dao.findByUserId(USER_ID);
            System.out.println("RSVPs by user_id=" + USER_ID + ": " + byUser.size());

            // 7. Try inserting the SAME pair again - should throw SQLException
            //    because of the UNIQUE(event_id, user_id) constraint.
            System.out.println("\nAttempting duplicate RSVP (should fail)...");
            EventRsvp duplicate = new EventRsvp(EVENT_ID, USER_ID, RsvpResponse.GOING);
            try {
                dao.insert(duplicate);
                System.out.println("Unexpected: duplicate insert succeeded!");
            } catch (SQLException e) {
                System.out.println("Expected failure - duplicate blocked by DB constraint: " + e.getMessage());
            }

        } catch (SQLException e) {
            System.out.println("Database error occurred:");
            e.printStackTrace();
        }
    }
}
