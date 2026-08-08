package com.careconnect.dao;

import com.careconnect.model.VolunteerOpportunity;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.util.List;

/**
 * MANUAL TEST CLASS - not a real unit test, just a quick throwaway runner
 * to confirm VolunteerOpportunityDAO actually talks to MySQL correctly.
 * Safe to delete once you're confident the DAO works, or once real
 * unit tests (JUnit) are added later.
 *
 * Assumes a row with institution_id = 1 already exists in the `institutions` table.
 */
public class VolunteerOpportunityDAOTest {

    public static void main(String[] args) {
        VolunteerOpportunityDAO dao = new VolunteerOpportunityDAO();

        try {
            // 1. Insert a new opportunity linked to institution_id = 1
            VolunteerOpportunity newOpportunity = new VolunteerOpportunity(
                    1,                                       // institution_id (must already exist)
                    "Weekend Reading Program",                // title
                    "Read books and spend time with residents", // description
                    "Patience, Communication",                // required_skills
                    Date.valueOf("2026-09-05"),               // event_date
                    Time.valueOf("10:00:00"),                 // start_time
                    Time.valueOf("13:00:00"),                 // end_time
                    "Sunshine Old Age Home, Pune",             // location
                    10                                        // max_volunteers
            );

            boolean inserted = dao.insert(newOpportunity);
            System.out.println("Insert successful: " + inserted);
            System.out.println("Generated opportunity_id: " + newOpportunity.getOpportunityId());

            // 2. Read it back by ID to confirm it round-tripped correctly
            VolunteerOpportunity fetched = dao.findById(newOpportunity.getOpportunityId());
            System.out.println("Fetched back: " + fetched);

            // 3. List all opportunities (sanity check findAll())
            List<VolunteerOpportunity> all = dao.findAll();
            System.out.println("Total opportunities in table: " + all.size());
            for (VolunteerOpportunity o : all) {
                System.out.println("  " + o);
            }

        } catch (SQLException e) {
            System.out.println("Database error occurred:");
            e.printStackTrace();
        }
    }
}
