package com.careconnect.service;

import com.careconnect.DatabaseTestBase;
import com.careconnect.dao.DonationDAO;
import com.careconnect.model.DeliveryStatus;
import com.careconnect.model.ItemDonation;
import com.careconnect.model.Need;
import com.careconnect.model.NeedUrgency;
import com.careconnect.model.PickupOrDrop;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests ItemDonationService - the in-kind donation lifecycle and the
 * Needs <-> Donations integration (a DELIVERED item donation advances the
 * linked need's quantity_received and status) - against the real database
 * (see DatabaseTestBase).
 * Replaces the old main()-based runner.
 */
class ItemDonationServiceTest extends DatabaseTestBase {

    private final NeedService needService = new NeedService();
    private final ItemDonationService itemDonationService = new ItemDonationService();
    private final DonationDAO donationDAO = new DonationDAO();

    private Need newNeed(int quantityRequired) throws SQLException {
        Need need = needService.createNeed(INSTITUTION_ID, "Groceries", "Rice (25kg bags)",
                quantityRequired, NeedUrgency.HIGH, "Created by an automated test.");
        trackNeed(need.getNeedId());
        return need;
    }

    private ItemDonation newItemDonation(Integer needId, String itemName, int quantity,
                                         PickupOrDrop pickupOrDrop, String pickupAddress) throws SQLException {
        ItemDonation item = itemDonationService.createItemDonation(
                DONOR_ID, INSTITUTION_ID, needId, itemName, quantity, pickupOrDrop, pickupAddress);
        trackDonation(item.getDonationId());
        return item;
    }

    // ---------------------------------------------------------------- Needs <-> Donations

    @Test
    @DisplayName("delivered item donations advance the linked need: OPEN -> PARTIALLY_FULFILLED -> FULFILLED (capped)")
    void deliveryAdvancesNeedStatus() throws SQLException {
        Need need = newNeed(20);
        assertEquals("OPEN", String.valueOf(need.getStatus()));

        // 8 of 20 delivered by drop-off -> partially fulfilled
        ItemDonation first = newItemDonation(need.getNeedId(), "Rice (25kg bags)", 8, PickupOrDrop.DROP_OFF, null);
        itemDonationService.approveItemDonation(first.getItemDonationId());
        itemDonationService.markDelivered(first.getItemDonationId(), null);

        Need afterFirst = needService.getNeed(need.getNeedId());
        assertEquals(8, afterFirst.getQuantityReceived());
        assertEquals("PARTIALLY_FULFILLED", String.valueOf(afterFirst.getStatus()));

        // 15 more by pickup -> would be 23, capped at the required 20 -> fulfilled
        ItemDonation second = newItemDonation(need.getNeedId(), "Rice (25kg bags)", 15,
                PickupOrDrop.PICKUP, "12 MG Road, Pune");
        itemDonationService.approveItemDonation(second.getItemDonationId());
        itemDonationService.markPickedUp(second.getItemDonationId());
        itemDonationService.markDelivered(second.getItemDonationId(), null);

        Need afterSecond = needService.getNeed(need.getNeedId());
        assertEquals(20, afterSecond.getQuantityReceived());
        assertEquals("FULFILLED", String.valueOf(afterSecond.getStatus()));
    }

    @Test
    @DisplayName("approving, rejecting or cancelling never changes the need's quantity_received")
    void onlyDeliveryCountsTowardNeed() throws SQLException {
        Need need = newNeed(10);

        ItemDonation approved = newItemDonation(need.getNeedId(), "Rice (25kg bags)", 4, PickupOrDrop.DROP_OFF, null);
        itemDonationService.approveItemDonation(approved.getItemDonationId());

        ItemDonation rejected = newItemDonation(need.getNeedId(), "Rice (25kg bags)", 3, PickupOrDrop.DROP_OFF, null);
        itemDonationService.rejectItemDonation(rejected.getItemDonationId());

        ItemDonation cancelled = newItemDonation(need.getNeedId(), "Rice (25kg bags)", 2, PickupOrDrop.DROP_OFF, null);
        itemDonationService.cancelItemDonation(cancelled.getItemDonationId());

        Need after = needService.getNeed(need.getNeedId());
        assertEquals(0, after.getQuantityReceived());
        assertEquals("OPEN", String.valueOf(after.getStatus()));
    }

    // ---------------------------------------------------------------- lifecycle rules

    @Test
    @DisplayName("a new item donation starts PENDING, and delivery needs approval first")
    void startsPendingAndNeedsApproval() throws SQLException {
        ItemDonation item = newItemDonation(null, "Blankets", 5, PickupOrDrop.DROP_OFF, null);

        assertEquals(DeliveryStatus.PENDING, item.getDeliveryStatus());
        assertThrows(IllegalStateException.class,
                () -> itemDonationService.markDelivered(item.getItemDonationId(), null));
        assertThrows(IllegalStateException.class,
                () -> itemDonationService.markPickedUp(item.getItemDonationId()));
    }

    @Test
    @DisplayName("approve moves PENDING to ACCEPTED and can't be repeated")
    void approveRules() throws SQLException {
        ItemDonation item = newItemDonation(null, "Blankets", 5, PickupOrDrop.DROP_OFF, null);

        ItemDonation approved = itemDonationService.approveItemDonation(item.getItemDonationId());

        assertEquals(DeliveryStatus.ACCEPTED, approved.getDeliveryStatus());
        assertEquals(DeliveryStatus.ACCEPTED,
                itemDonationService.getItemDonation(item.getItemDonationId()).getDeliveryStatus());
        assertThrows(IllegalStateException.class,
                () -> itemDonationService.approveItemDonation(item.getItemDonationId()));
    }

    @Test
    @DisplayName("reject moves PENDING to REJECTED and cancels the parent donation")
    void rejectRules() throws SQLException {
        ItemDonation item = newItemDonation(null, "Blankets", 5, PickupOrDrop.DROP_OFF, null);

        ItemDonation rejected = itemDonationService.rejectItemDonation(item.getItemDonationId());

        assertEquals(DeliveryStatus.REJECTED, rejected.getDeliveryStatus());
        assertEquals("CANCELLED", donationDAO.getDonationById(item.getDonationId()).getDonationStatus());
        assertThrows(IllegalStateException.class,
                () -> itemDonationService.markDelivered(item.getItemDonationId(), null));
        assertThrows(IllegalStateException.class,
                () -> itemDonationService.approveItemDonation(item.getItemDonationId()));
    }

    @Test
    @DisplayName("markDelivered records the date, completes the parent donation, and can't be repeated")
    void markDeliveredRules() throws SQLException {
        ItemDonation item = newItemDonation(null, "Blankets", 5, PickupOrDrop.DROP_OFF, null);
        itemDonationService.approveItemDonation(item.getItemDonationId());

        ItemDonation delivered = itemDonationService.markDelivered(item.getItemDonationId(), null);

        assertEquals(DeliveryStatus.DELIVERED, delivered.getDeliveryStatus());
        assertNotNull(delivered.getDeliveryDate(), "a null date defaults to today");
        assertEquals("COMPLETED", donationDAO.getDonationById(item.getDonationId()).getDonationStatus());
        assertThrows(IllegalStateException.class,
                () -> itemDonationService.markDelivered(item.getItemDonationId(), null));
    }

    @Test
    @DisplayName("cancel works before delivery, but not after DELIVERED, and not twice")
    void cancelRules() throws SQLException {
        ItemDonation pending = newItemDonation(null, "Blankets", 5, PickupOrDrop.DROP_OFF, null);
        ItemDonation cancelled = itemDonationService.cancelItemDonation(pending.getItemDonationId());
        assertEquals(DeliveryStatus.CANCELLED, cancelled.getDeliveryStatus());
        assertEquals("CANCELLED", donationDAO.getDonationById(pending.getDonationId()).getDonationStatus());
        assertThrows(IllegalStateException.class,
                () -> itemDonationService.cancelItemDonation(pending.getItemDonationId()));

        ItemDonation delivered = newItemDonation(null, "Towels", 2, PickupOrDrop.DROP_OFF, null);
        itemDonationService.approveItemDonation(delivered.getItemDonationId());
        itemDonationService.markDelivered(delivered.getItemDonationId(), null);
        assertThrows(IllegalStateException.class,
                () -> itemDonationService.cancelItemDonation(delivered.getItemDonationId()));
    }

    // ---------------------------------------------------------------- validation and lookups

    @Test
    @DisplayName("createItemDonation validates its inputs")
    void createValidation() throws SQLException {
        Need need = newNeed(5);

        assertThrows(IllegalArgumentException.class, () -> itemDonationService.createItemDonation(
                DONOR_ID, Integer.MAX_VALUE, null, "Blankets", 5, PickupOrDrop.DROP_OFF, null));
        assertThrows(IllegalArgumentException.class, () -> itemDonationService.createItemDonation(
                DONOR_ID, INSTITUTION_ID, null, "  ", 5, PickupOrDrop.DROP_OFF, null));
        assertThrows(IllegalArgumentException.class, () -> itemDonationService.createItemDonation(
                DONOR_ID, INSTITUTION_ID, null, "Blankets", 0, PickupOrDrop.DROP_OFF, null));
        assertThrows(IllegalArgumentException.class, () -> itemDonationService.createItemDonation(
                DONOR_ID, INSTITUTION_ID, null, "Blankets", 5, null, null));
        assertThrows(IllegalArgumentException.class, () -> itemDonationService.createItemDonation(
                DONOR_ID, INSTITUTION_ID, null, "Blankets", 5, PickupOrDrop.PICKUP, " "));
        assertThrows(IllegalArgumentException.class, () -> itemDonationService.createItemDonation(
                DONOR_ID, INSTITUTION_ID, Integer.MAX_VALUE, "Blankets", 5, PickupOrDrop.DROP_OFF, null));

        // A valid need of this institution is accepted
        ItemDonation ok = newItemDonation(need.getNeedId(), "Blankets", 5, PickupOrDrop.DROP_OFF, null);
        assertTrue(ok.getItemDonationId() > 0);
    }

    @Test
    @DisplayName("lookups: by id, by donor and by institution")
    void lookups() throws SQLException {
        ItemDonation item = newItemDonation(null, "Blankets", 5, PickupOrDrop.DROP_OFF, null);

        assertEquals("Blankets", itemDonationService.getItemDonation(item.getItemDonationId()).getItemName());
        assertTrue(itemDonationService.getItemDonationsForDonor(DONOR_ID).stream()
                .anyMatch(i -> i.getItemDonationId() == item.getItemDonationId()));
        assertTrue(itemDonationService.getItemDonationsForInstitution(INSTITUTION_ID).stream()
                .anyMatch(i -> i.getItemDonationId() == item.getItemDonationId()));

        assertThrows(IllegalArgumentException.class,
                () -> itemDonationService.getItemDonation(Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class,
                () -> itemDonationService.getItemDonationsForInstitution(Integer.MAX_VALUE));
    }
}
