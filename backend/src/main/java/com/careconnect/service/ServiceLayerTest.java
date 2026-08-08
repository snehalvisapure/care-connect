package com.careconnect.service;

import com.careconnect.model.OpportunityStatus;
import com.careconnect.model.RegistrationStatus;
import com.careconnect.model.Volunteer;
import com.careconnect.model.VolunteerOpportunity;
import com.careconnect.model.VolunteerRegistration;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;

/**
 * MANUAL TEST CLASS - not a real unit test, just a quick throwaway runner
 * to confirm the Service layer's business rules actually work:
 *   1. Duplicate volunteer profile prevention
 *   2. Applying to a CLOSED opportunity is blocked
 *   3. Duplicate application is blocked with a friendly message
 *   4. Capacity cap (max_volunteers) is enforced when accepting
 *   5. Withdrawing after acceptance is blocked
 *
 * Assumes:
 *   - user_id = 1 already has a volunteer profile (volunteer_id = 1)
 *   - user_id = 10 exists but has NO volunteer profile yet
 *   - institution_id = 1 exists
 */
public class ServiceLayerTest {

    public static void main(String[] args) {
        VolunteerService volunteerService = new VolunteerService();
        VolunteerOpportunityService opportunityService = new VolunteerOpportunityService();
        VolunteerRegistrationService registrationService = new VolunteerRegistrationService();

        try {
            System.out.println("=== 1. Duplicate profile prevention ===");
            try {
                volunteerService.createProfile(1, "Cooking", "Weekends", "N/A");
                System.out.println("UNEXPECTED: duplicate profile was allowed!");
            } catch (IllegalStateException e) {
                System.out.println("Expected failure: " + e.getMessage());
            }

            System.out.println("\n=== 2. Create second volunteer (user_id=10) ===");
            Volunteer volunteer2 = volunteerService.createProfile(
                    10, "First Aid, Music", "Evenings", "1 year at community center");
            System.out.println("Created volunteer_id=" + volunteer2.getVolunteerId());

            System.out.println("\n=== 3. Create a tightly-capped opportunity (max_volunteers=1) ===");
            VolunteerOpportunity capped = opportunityService.createOpportunity(
                    1, "Diwali Celebration Setup", "Help decorate and set up for Diwali event",
                    "Physical work", Date.valueOf("2026-10-20"),
                    Time.valueOf("09:00:00"), Time.valueOf("12:00:00"),
                    "Sunshine Old Age Home", 1); // cap of exactly 1
            System.out.println("Created opportunity_id=" + capped.getOpportunityId()
                    + " with max_volunteers=" + capped.getMaxVolunteers());

            System.out.println("\n=== 4. Both volunteers apply ===");
            VolunteerRegistration reg1 = registrationService.applyToOpportunity(1, capped.getOpportunityId());
            System.out.println("Volunteer 1 applied: registration_id=" + reg1.getRegistrationId());

            VolunteerRegistration reg2 = registrationService.applyToOpportunity(
                    volunteer2.getVolunteerId(), capped.getOpportunityId());
            System.out.println("Volunteer 2 applied: registration_id=" + reg2.getRegistrationId());

            System.out.println("\n=== 5. Duplicate application blocked ===");
            try {
                registrationService.applyToOpportunity(1, capped.getOpportunityId());
                System.out.println("UNEXPECTED: duplicate application was allowed!");
            } catch (IllegalStateException e) {
                System.out.println("Expected failure: " + e.getMessage());
            }

            System.out.println("\n=== 6. Accept volunteer 1 (should succeed - cap is 1, 0 accepted so far) ===");
            registrationService.acceptRegistration(reg1.getRegistrationId());
            System.out.println("Volunteer 1 accepted successfully.");

            System.out.println("\n=== 7. Accept volunteer 2 (should FAIL - cap of 1 already reached) ===");
            try {
                registrationService.acceptRegistration(reg2.getRegistrationId());
                System.out.println("UNEXPECTED: accepted beyond capacity!");
            } catch (IllegalStateException e) {
                System.out.println("Expected failure: " + e.getMessage());
            }

            System.out.println("\n=== 8. Volunteer 2 withdraws instead (still APPLIED, should succeed) ===");
            registrationService.withdrawApplication(reg2.getRegistrationId());
            System.out.println("Volunteer 2 withdrew successfully.");

            System.out.println("\n=== 9. Volunteer 1 tries to withdraw AFTER being accepted (should FAIL) ===");
            try {
                registrationService.withdrawApplication(reg1.getRegistrationId());
                System.out.println("UNEXPECTED: withdrew after acceptance!");
            } catch (IllegalStateException e) {
                System.out.println("Expected failure: " + e.getMessage());
            }

            System.out.println("\n=== 10. Close the opportunity, then try a new application (should FAIL) ===");
            opportunityService.closeOpportunity(capped.getOpportunityId());
            System.out.println("Opportunity closed. Status now: "
                    + opportunityService.getOpportunity(capped.getOpportunityId()).getStatus());
            try {
                registrationService.applyToOpportunity(volunteer2.getVolunteerId(), capped.getOpportunityId());
                System.out.println("UNEXPECTED: applied to a closed opportunity!");
            } catch (IllegalStateException e) {
                System.out.println("Expected failure: " + e.getMessage());
            }

            System.out.println("\n=== All service layer checks completed ===");

        } catch (SQLException e) {
            System.out.println("Unexpected database error occurred:");
            e.printStackTrace();
        }
    }
}
