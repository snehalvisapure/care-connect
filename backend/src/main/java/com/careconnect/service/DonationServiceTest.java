package com.careconnect.service;

import com.careconnect.model.Donation;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * MANUAL TEST CLASS - not a real unit test, just a quick throwaway runner
 * to walk through the pledge -> confirmed donation lifecycle.
 * Safe to delete once you're confident it works, or once real unit tests
 * (JUnit) are added later.
 *
 * Assumes donor user_id = 1 and institution_id = 1 already exist
 * (institution_id = 1 comes from InstitutionDAOTest).
 */
public class DonationServiceTest {

    public static void main(String[] args) {
        DonationService donationService = new DonationService();

        try {
            // 1. Create a pledge (general donation, not tied to a specific need)
            Donation pledge = donationService.createPledge(1, 1, null, new BigDecimal("500.00"));
            System.out.println("Pledge created: " + pledge);

            // 2. Confirm it as pending in the donor's pending pledges
            List<Donation> pendingForDonor = donationService.getPendingPledgesForDonor(1);
            System.out.println("Donor 1's pending pledges: " + pendingForDonor.size());

            // 3. Confirm payment - moves it to COMPLETED with a transaction ID + receipt number
            Donation confirmed = donationService.confirmPayment(pledge.getDonationId(), "TXN-DEMO-12345");
            System.out.println("After confirmPayment: " + confirmed);

            // 4. Trying to confirm again should fail (already COMPLETED)
            try {
                donationService.confirmPayment(pledge.getDonationId(), "TXN-DEMO-99999");
                System.out.println("ERROR: expected an IllegalStateException but none was thrown");
            } catch (IllegalStateException e) {
                System.out.println("Correctly rejected double-confirmation: " + e.getMessage());
            }

            // 5. Create a second pledge and cancel it instead of confirming it
            Donation secondPledge = donationService.createPledge(1, 1, null, new BigDecimal("200.00"));
            Donation cancelled = donationService.cancelPledge(secondPledge.getDonationId());
            System.out.println("After cancelPledge: " + cancelled);

            // 6. Donor's full donation history should now show both
            List<Donation> history = donationService.getDonationHistoryForDonor(1);
            System.out.println("Donor 1's full donation history (" + history.size() + "):");
            for (Donation d : history) {
                System.out.println("  " + d);
            }

            // 7. Institution's pending pledges (should be empty now - one completed, one cancelled)
            List<Donation> pendingForInstitution = donationService.getPendingPledgesForInstitution(1);
            System.out.println("Institution 1's pending pledges: " + pendingForInstitution.size());

        } catch (SQLException e) {
            System.out.println("Database error occurred:");
            e.printStackTrace();
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
