package com.careconnect.dao;

import com.careconnect.model.DeliveryStatus;
import com.careconnect.model.Donation;
import com.careconnect.model.ItemDonation;
import com.careconnect.model.PickupOrDrop;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

/**
 * MANUAL TEST CLASS - not a real unit test, just a quick throwaway runner
 * to confirm ItemDonationDAO actually talks to MySQL correctly.
 * Safe to delete once you're confident the DAO works, or once real
 * unit tests (JUnit) are added later.
 *
 * Assumes donor user_id = 1 and institution_id = 1 already exist
 * (institution_id = 1 comes from InstitutionDAOTest).
 */
public class ItemDonationDAOTest {

    public static void main(String[] args) {
        DonationDAO donationDAO = new DonationDAO();
        ItemDonationDAO itemDonationDAO = new ItemDonationDAO();

        try {
            // 1. Create the parent donation row directly (ItemDonationService
            //    normally does this step - this test exercises the DAO alone)
            Donation donation = new Donation(1, 1, null, "ITEM", BigDecimal.ZERO);
            int donationId = donationDAO.createDonation(donation);
            System.out.println("Parent donation created, donation_id=" + donationId);

            // 2. Insert the item donation detail row
            ItemDonation newItemDonation = new ItemDonation(
                    donationId, "Blankets", 15, PickupOrDrop.DROP_OFF, null);
            boolean inserted = itemDonationDAO.insert(newItemDonation);
            System.out.println("Insert successful: " + inserted);
            System.out.println("Generated item_donation_id: " + newItemDonation.getItemDonationId());

            // 3. Read it back by ID
            ItemDonation fetched = itemDonationDAO.findById(newItemDonation.getItemDonationId());
            System.out.println("Fetched back: " + fetched);

            // 4. Read it back by donation_id
            ItemDonation byDonationId = itemDonationDAO.findByDonationId(donationId);
            System.out.println("Fetched by donation_id: " + byDonationId);

            // 5. List all item donations for institution 1 (joins against `donations`)
            List<ItemDonation> forInstitution = itemDonationDAO.findByInstitutionId(1);
            System.out.println("Total item donations for institution_id=1: " + forInstitution.size());

            // 6. List all item donations for donor 1
            List<ItemDonation> forDonor = itemDonationDAO.findByDonorId(1);
            System.out.println("Total item donations for donor_id=1: " + forDonor.size());

            // 7. Move it through PICKED_UP -> DELIVERED
            boolean pickedUp = itemDonationDAO.updateDeliveryStatus(
                    newItemDonation.getItemDonationId(), DeliveryStatus.PICKED_UP);
            System.out.println("Marked PICKED_UP: " + pickedUp);

            boolean delivered = itemDonationDAO.updateDeliveryStatus(
                    newItemDonation.getItemDonationId(), DeliveryStatus.DELIVERED);
            boolean dateSet = itemDonationDAO.updateDeliveryDate(
                    newItemDonation.getItemDonationId(), new Date(System.currentTimeMillis()));
            System.out.println("Marked DELIVERED: " + delivered + ", delivery date set: " + dateSet);

            ItemDonation afterDelivery = itemDonationDAO.findById(newItemDonation.getItemDonationId());
            System.out.println("After delivery: " + afterDelivery);

        } catch (SQLException e) {
            System.out.println("Test failed with SQL error.");
            e.printStackTrace();
        }
    }
}
