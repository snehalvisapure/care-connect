package com.careconnect.dao;

import com.careconnect.model.Donation;

import java.math.BigDecimal;
import java.util.List;

public class DonationDAOTest {

    public static void main(String[] args) {

        DonationDAO donationDAO = new DonationDAO();

        try {
            // Test donation / pledge creation
            Donation donation = new Donation(
                    1,                       // donor_id
                    1,                       // institution_id
                    null,                   // need_id
                    "MONEY",                // donation_type
                    new BigDecimal("500.00")
            );

            int donationId = donationDAO.createDonation(donation);

            System.out.println("Created donation ID: " + donationId);

            // Test retrieving the donation
            Donation savedDonation =
                    donationDAO.getDonationById(donationId);

            System.out.println("Retrieved donation:");
            System.out.println(savedDonation);

            // Test pending pledges
            List<Donation> pledges =
                    donationDAO.getPendingPledgesByDonor(1);

            System.out.println("Pending pledges for donor 1: "
                    + pledges.size());

            for (Donation pledge : pledges) {
                System.out.println(pledge);
            }

            System.out.println("DonationDAO test completed successfully.");

        } catch (Exception e) {
            System.out.println("DonationDAO test failed.");
            e.printStackTrace();
        }
    }
}