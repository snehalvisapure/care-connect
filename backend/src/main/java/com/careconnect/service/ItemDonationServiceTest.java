package com.careconnect.service;

import com.careconnect.model.ItemDonation;
import com.careconnect.model.Need;
import com.careconnect.model.NeedUrgency;
import com.careconnect.model.PickupOrDrop;

import java.sql.SQLException;

/**
 * MANUAL TEST CLASS - not a real unit test, just a quick throwaway runner
 * to walk through the in-kind donation lifecycle AND confirm the
 * Needs <-> Donations integration (a DELIVERED item donation should advance
 * the linked need's quantity_received and status).
 * Safe to delete once you're confident it works, or once real unit tests
 * (JUnit) are added later.
 *
 * Assumes donor user_id = 1 and institution_id = 1 already exist
 * (institution_id = 1 comes from InstitutionDAOTest).
 */
public class ItemDonationServiceTest {

    public static void main(String[] args) {
        NeedService needService = new NeedService();
        ItemDonationService itemDonationService = new ItemDonationService();

        try {
            // 1. Post a need for institution 1 that requires 20 units
            Need need = needService.createNeed(1, "Groceries", "Rice (25kg bags)",
                    20, NeedUrgency.HIGH, "Monthly rice stock for the kitchen.");
            System.out.println("Need created: " + need);
            System.out.println("Initial status should be OPEN: " + need.getStatus());

            // 2. Donor pledges an in-kind donation of 8 units toward that need (drop-off)
            ItemDonation donation1 = itemDonationService.createItemDonation(
                    1, 1, need.getNeedId(), "Rice (25kg bags)", 8, PickupOrDrop.DROP_OFF, null);
            System.out.println("Item donation 1 created: " + donation1);

            // 3. Mark it delivered -> should push quantity_received to 8, status PARTIALLY_FULFILLED
            itemDonationService.markDelivered(donation1.getItemDonationId(), null);
            Need afterFirstDelivery = needService.getNeed(need.getNeedId());
            System.out.println("Need after first delivery: " + afterFirstDelivery);
            System.out.println("Expected quantity_received=8, status=PARTIALLY_FULFILLED -> actual: "
                    + afterFirstDelivery.getQuantityReceived() + ", " + afterFirstDelivery.getStatus());

            // 4. A second donor pledges 15 more units, this time via PICKUP
            ItemDonation donation2 = itemDonationService.createItemDonation(
                    1, 1, need.getNeedId(), "Rice (25kg bags)", 15,
                    PickupOrDrop.PICKUP, "12 MG Road, Pune");
            itemDonationService.markPickedUp(donation2.getItemDonationId());
            itemDonationService.markDelivered(donation2.getItemDonationId(), null);

            // 5. quantity_received should be capped at quantity_required (20), status FULFILLED
            Need afterSecondDelivery = needService.getNeed(need.getNeedId());
            System.out.println("Need after second delivery: " + afterSecondDelivery);
            System.out.println("Expected quantity_received=20 (capped), status=FULFILLED -> actual: "
                    + afterSecondDelivery.getQuantityReceived() + ", " + afterSecondDelivery.getStatus());

            // 6. Cancelling a PENDING/PICKED_UP donation should work; cancelling a DELIVERED one should not
            ItemDonation donation3 = itemDonationService.createItemDonation(
                    1, 1, null, "Blankets", 5, PickupOrDrop.DROP_OFF, null);
            ItemDonation cancelled = itemDonationService.cancelItemDonation(donation3.getItemDonationId());
            System.out.println("Cancelled a PENDING donation: " + cancelled);

            try {
                itemDonationService.cancelItemDonation(donation1.getItemDonationId());
                System.out.println("ERROR: expected an IllegalStateException but none was thrown");
            } catch (IllegalStateException e) {
                System.out.println("Correctly rejected cancelling a DELIVERED donation: " + e.getMessage());
            }

        } catch (SQLException e) {
            System.out.println("Test failed with SQL error.");
            e.printStackTrace();
        }
    }
}
