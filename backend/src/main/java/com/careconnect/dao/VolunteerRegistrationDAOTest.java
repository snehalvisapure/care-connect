package com.careconnect.dao;

import com.careconnect.model.RegistrationStatus;
import com.careconnect.model.VolunteerRegistration;

import java.sql.SQLException;
import java.util.List;

/**
 * MANUAL TEST CLASS - not a real unit test, just a quick throwaway runner
 * to confirm VolunteerRegistrationDAO actually talks to MySQL correctly.
 * Safe to delete once you're confident the DAO works, or once real
 * unit tests (JUnit) are added later.
 *
 * Assumes volunteer_id = 1 (from VolunteerDAOTest) and
 * opportunity_id = 1 (from VolunteerOpportunityDAOTest) already exist.
 */
public class VolunteerRegistrationDAOTest {

    public static void main(String[] args) {
        VolunteerRegistrationDAO dao = new VolunteerRegistrationDAO();

        try {
            // 1. Insert a new registration: volunteer 1 applies to opportunity 1
            VolunteerRegistration newRegistration = new VolunteerRegistration(
                    1,  // opportunity_id (must already exist)
                    1   // volunteer_id (must already exist)
            );

            boolean inserted = dao.insert(newRegistration);
            System.out.println("Insert successful: " + inserted);
            System.out.println("Generated registration_id: " + newRegistration.getRegistrationId());

            // 2. Read it back by ID to confirm it round-tripped correctly
            VolunteerRegistration fetched = dao.findById(newRegistration.getRegistrationId());
            System.out.println("Fetched back: " + fetched);

            // 3. Update the status, e.g. institution accepts the volunteer
            fetched.setStatus(RegistrationStatus.ACCEPTED);
            boolean updated = dao.update(fetched);
            System.out.println("Update successful: " + updated);

            VolunteerRegistration afterUpdate = dao.findById(fetched.getRegistrationId());
            System.out.println("After update: " + afterUpdate);

            // 4. List all registrations (sanity check findAll())
            List<VolunteerRegistration> all = dao.findAll();
            System.out.println("Total registrations in table: " + all.size());
            for (VolunteerRegistration r : all) {
                System.out.println("  " + r);
            }

            // 5. Try inserting the SAME pair again - should throw SQLException
            //    because of the UNIQUE(opportunity_id, volunteer_id) constraint.
            System.out.println("\nAttempting duplicate registration (should fail)...");
            VolunteerRegistration duplicate = new VolunteerRegistration(1, 1);
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
