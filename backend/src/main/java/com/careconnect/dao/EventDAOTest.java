package com.careconnect.dao;

import com.careconnect.model.Event;
import com.careconnect.model.EventCategory;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.util.List;

/**
 * MANUAL TEST CLASS - not a real unit test, just a quick throwaway runner
 * to confirm EventDAO actually talks to MySQL correctly.
 * Safe to delete once you're confident the DAO works, or once real
 * unit tests (JUnit) are added later.
 *
 * Assumes institution_id = 1 already exists in the `institutions` table.
 */
public class EventDAOTest {

    public static void main(String[] args) {
        EventDAO dao = new EventDAO();

        try {
            // 1. Insert a new event linked to institution_id = 1
            Event newEvent = new Event(
                    1,                                        // institution_id
                    "Grandma Rosa's 80th Birthday",            // title
                    "Come celebrate with cake and music!",     // description
                    Date.valueOf("2026-09-15"),                // event_date
                    Time.valueOf("16:00:00"),                  // event_time
                    "Sunshine Old Age Home, Pune",              // location
                    EventCategory.BIRTHDAY                     // category
            );

            boolean inserted = dao.insert(newEvent);
            System.out.println("Insert successful: " + inserted);
            System.out.println("Generated event_id: " + newEvent.getEventId());

            // 2. Read it back by ID to confirm it round-tripped correctly
            Event fetched = dao.findById(newEvent.getEventId());
            System.out.println("Fetched back: " + fetched);

            // 3. Find by institution
            List<Event> byInstitution = dao.findByInstitutionId(1);
            System.out.println("Events for institution_id=1: " + byInstitution.size());

            // 4. List all events (sanity check findAll())
            List<Event> all = dao.findAll();
            System.out.println("Total events in table: " + all.size());
            for (Event e : all) {
                System.out.println("  " + e);
            }

        } catch (SQLException e) {
            System.out.println("Database error occurred:");
            e.printStackTrace();
        }
    }
}
