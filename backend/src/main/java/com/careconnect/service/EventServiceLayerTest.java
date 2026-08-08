package com.careconnect.service;

import com.careconnect.model.Event;
import com.careconnect.model.EventCategory;
import com.careconnect.model.EventRsvp;
import com.careconnect.model.RsvpResponse;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.util.List;

/**
 * MANUAL TEST CLASS - not a real unit test, just a quick throwaway runner
 * to confirm the Event/EventRsvp Service layer business rules work:
 *   1. Create an event and list it via various filters
 *   2. RSVP upsert behaviour (first RSVP = insert, second call = update, not a duplicate)
 *   3. countGoing() reflects multiple users correctly
 *   4. Removing an RSVP works, and removing again correctly fails
 *
 * Assumes:
 *   - institution_id = 1 already exists
 *   - user_id = 1 and user_id = 10 already exist
 */
public class EventServiceLayerTest {

    public static void main(String[] args) {
        EventService eventService = new EventService();
        EventRsvpService rsvpService = new EventRsvpService();

        try {
            System.out.println("=== 1. Create a new event ===");
            Event event = eventService.createEvent(
                    1, "Diwali Fundraiser Dinner", "Community dinner to raise funds",
                    Date.valueOf("2026-11-08"), Time.valueOf("18:30:00"),
                    "Sunshine Old Age Home", EventCategory.FUNDRAISER);
            System.out.println("Created event_id=" + event.getEventId());

            System.out.println("\n=== 2. List upcoming events (should include the new one) ===");
            List<Event> upcoming = eventService.listUpcomingEvents();
            System.out.println("Upcoming events count: " + upcoming.size());

            System.out.println("\n=== 3. List by category FUNDRAISER ===");
            List<Event> fundraisers = eventService.listByCategory(EventCategory.FUNDRAISER);
            System.out.println("FUNDRAISER events count: " + fundraisers.size());

            System.out.println("\n=== 4. User 1 RSVPs as INTERESTED (first time - should insert) ===");
            EventRsvp rsvp1 = rsvpService.rsvp(event.getEventId(), 1, RsvpResponse.INTERESTED);
            System.out.println("RSVP result: " + rsvp1);

            System.out.println("\n=== 5. User 1 changes mind to GOING (should UPDATE, not create a 2nd row) ===");
            EventRsvp rsvp1Updated = rsvpService.rsvp(event.getEventId(), 1, RsvpResponse.GOING);
            System.out.println("RSVP result: " + rsvp1Updated);
            System.out.println("Same rsvp_id as before? " + (rsvp1.getRsvpId() == rsvp1Updated.getRsvpId()));

            System.out.println("\n=== 6. User 10 RSVPs as GOING ===");
            EventRsvp rsvp2 = rsvpService.rsvp(event.getEventId(), 10, RsvpResponse.GOING);
            System.out.println("RSVP result: " + rsvp2);

            System.out.println("\n=== 7. Count how many are GOING (should be 2) ===");
            long goingCount = rsvpService.countGoing(event.getEventId());
            System.out.println("Going count: " + goingCount);

            System.out.println("\n=== 8. Check user 1's current RSVP status ===");
            EventRsvp status = rsvpService.getRsvpStatus(event.getEventId(), 1);
            System.out.println("User 1 status: " + status);

            System.out.println("\n=== 9. User 1 removes their RSVP entirely ===");
            rsvpService.removeRsvp(event.getEventId(), 1);
            System.out.println("Removed successfully.");
            EventRsvp afterRemoval = rsvpService.getRsvpStatus(event.getEventId(), 1);
            System.out.println("User 1 status after removal (should be null): " + afterRemoval);

            System.out.println("\n=== 10. Try removing again (should FAIL - nothing to remove) ===");
            try {
                rsvpService.removeRsvp(event.getEventId(), 1);
                System.out.println("UNEXPECTED: removed a non-existent RSVP!");
            } catch (IllegalArgumentException e) {
                System.out.println("Expected failure: " + e.getMessage());
            }

            System.out.println("\n=== 11. Final counts ===");
            System.out.println("RSVPs for this event: " + rsvpService.listByEvent(event.getEventId()).size());
            System.out.println("Going count now: " + rsvpService.countGoing(event.getEventId()));

            System.out.println("\n=== All event service checks completed ===");

        } catch (SQLException e) {
            System.out.println("Unexpected database error occurred:");
            e.printStackTrace();
        }
    }
}
