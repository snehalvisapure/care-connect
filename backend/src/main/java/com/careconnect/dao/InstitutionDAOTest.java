package com.careconnect.dao;

import com.careconnect.model.Institution;
import com.careconnect.model.InstitutionType;

import java.sql.SQLException;
import java.util.List;

/**
 * MANUAL TEST CLASS - not a real unit test, just a quick throwaway runner
 * to confirm InstitutionDAO actually talks to MySQL correctly.
 * Safe to delete once you're confident the DAO works, or once real
 * unit tests (JUnit) are added later.
 *
 * Assumes a row with user_id = 1 already exists in the `users` table.
 */
public class InstitutionDAOTest {

    public static void main(String[] args) {
        InstitutionDAO dao = new InstitutionDAO();

        try {
            // 1. Insert a new institution registration linked to user_id = 1
            Institution newInstitution = new Institution(
                    1,                                  // user_id (must already exist in users table)
                    "Sunshine Old Age Home",             // institution_name
                    InstitutionType.OLD_AGE_HOME,        // institution_type
                    "REG-2026-00123",                    // registration_number (must be unique)
                    "A home providing care for the elderly.", // description
                    "12 MG Road",                        // address
                    "Pune",                              // city
                    "Maharashtra",                       // state
                    "411001",                             // pincode
                    "Asha Kulkarni"                       // contact_person
            );

            boolean inserted = dao.insert(newInstitution);
            System.out.println("Insert successful: " + inserted);
            System.out.println("Generated institution_id: " + newInstitution.getInstitutionId());

            // 2. Read it back by ID to confirm it round-tripped correctly
            Institution fetched = dao.findById(newInstitution.getInstitutionId());
            System.out.println("Fetched back: " + fetched);

            // 3. Confirm it can also be found by user_id
            Institution byUser = dao.findByUserId(1);
            System.out.println("Found by user_id=1: " + byUser);

            // 4. List all institutions (sanity check findAll())
            List<Institution> all = dao.findAll();
            System.out.println("Total institutions in table: " + all.size());
            for (Institution i : all) {
                System.out.println("  " + i);
            }

        } catch (SQLException e) {
            System.out.println("Database error occurred:");
            e.printStackTrace();
        }
    }
}
