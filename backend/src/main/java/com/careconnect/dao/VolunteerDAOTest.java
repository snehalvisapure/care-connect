package com.careconnect.dao;

import com.careconnect.model.Volunteer;

import java.sql.SQLException;
import java.util.List;

/**
 * MANUAL TEST CLASS - not a real unit test, just a quick throwaway runner
 * to confirm VolunteerDAO actually talks to MySQL correctly.
 * Safe to delete once you're confident the DAO works, or once real
 * unit tests (JUnit) are added later.
 *
 * Assumes a row with user_id = 1 already exists in the `users` table.
 */
public class VolunteerDAOTest {

    public static void main(String[] args) {
        VolunteerDAO dao = new VolunteerDAO();

        try {
            // 1. Insert a new volunteer profile linked to user_id = 1
            Volunteer newVolunteer = new Volunteer(
                    1,                              // user_id (must already exist in users table)
                    "Cooking, Teaching, Driving",   // skills
                    "Weekends",                     // availability
                    "2 years at a local shelter"    // experience
            );

            boolean inserted = dao.insert(newVolunteer);
            System.out.println("Insert successful: " + inserted);
            System.out.println("Generated volunteer_id: " + newVolunteer.getVolunteerId());

            // 2. Read it back by ID to confirm it round-tripped correctly
            Volunteer fetched = dao.findById(newVolunteer.getVolunteerId());
            System.out.println("Fetched back: " + fetched);

            // 3. List all volunteers (sanity check findAll())
            List<Volunteer> all = dao.findAll();
            System.out.println("Total volunteers in table: " + all.size());
            for (Volunteer v : all) {
                System.out.println("  " + v);
            }

        } catch (SQLException e) {
            System.out.println("Database error occurred:");
            e.printStackTrace();
        }
    }
}
