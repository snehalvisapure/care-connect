package com.careconnect.dao;

import com.careconnect.DatabaseTestBase;
import com.careconnect.model.DeliveryStatus;
import com.careconnect.model.Donation;
import com.careconnect.model.ItemDonation;
import com.careconnect.model.PickupOrDrop;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests ItemDonationDAO against the real database (see DatabaseTestBase).
 * Replaces the old main()-based runner.
 */
class ItemDonationDAOTest extends DatabaseTestBase {

    private final DonationDAO donationDAO = new DonationDAO();
    private final ItemDonationDAO itemDonationDAO = new ItemDonationDAO();

    /** Creates the parent ITEM donation row plus its item_donations row. */
    private ItemDonation createItemDonation(String itemName, int quantity,
                                            PickupOrDrop pickupOrDrop, String pickupAddress) throws SQLException {
        Donation parent = new Donation(DONOR_ID, INSTITUTION_ID, null, "ITEM", BigDecimal.ZERO);
        int donationId = trackDonation(donationDAO.createDonation(parent));

        ItemDonation item = new ItemDonation(donationId, itemName, quantity, pickupOrDrop, pickupAddress);
        assertTrue(itemDonationDAO.insert(item), "insert should affect exactly one row");
        assertTrue(item.getItemDonationId() > 0, "insert should set the generated item_donation_id");
        return item;
    }

    @Test
    @DisplayName("insert saves an item donation that findById and findByDonationId read back")
    void insertAndFind() throws SQLException {
        ItemDonation created = createItemDonation("Blankets", 15, PickupOrDrop.DROP_OFF, null);

        ItemDonation byId = itemDonationDAO.findById(created.getItemDonationId());
        assertNotNull(byId);
        assertEquals(created.getDonationId(), byId.getDonationId());
        assertEquals("Blankets", byId.getItemName());
        assertEquals(15, byId.getQuantity());
        assertEquals(PickupOrDrop.DROP_OFF, byId.getPickupOrDrop());
        assertNull(byId.getPickupAddress());
        assertEquals(DeliveryStatus.PENDING, byId.getDeliveryStatus());
        assertNull(byId.getDeliveryDate());

        ItemDonation byDonationId = itemDonationDAO.findByDonationId(created.getDonationId());
        assertNotNull(byDonationId);
        assertEquals(created.getItemDonationId(), byDonationId.getItemDonationId());
    }

    @Test
    @DisplayName("findById and findByDonationId return null when nothing matches")
    void findReturnsNullWhenMissing() throws SQLException {
        assertNull(itemDonationDAO.findById(Integer.MAX_VALUE));
        assertNull(itemDonationDAO.findByDonationId(Integer.MAX_VALUE));
    }

    @Test
    @DisplayName("a PICKUP donation keeps its pickup address")
    void pickupAddressIsStored() throws SQLException {
        ItemDonation created = createItemDonation("Rice", 4, PickupOrDrop.PICKUP, "12 MG Road, Pune");

        ItemDonation fetched = itemDonationDAO.findById(created.getItemDonationId());
        assertEquals(PickupOrDrop.PICKUP, fetched.getPickupOrDrop());
        assertEquals("12 MG Road, Pune", fetched.getPickupAddress());
    }

    @Test
    @DisplayName("findByInstitutionId and findByDonorId include the new item donation")
    void listsByInstitutionAndDonor() throws SQLException {
        int itemDonationId = createItemDonation("Soap", 30, PickupOrDrop.DROP_OFF, null).getItemDonationId();

        assertTrue(itemDonationDAO.findByInstitutionId(INSTITUTION_ID).stream()
                .anyMatch(i -> i.getItemDonationId() == itemDonationId));
        assertTrue(itemDonationDAO.findByDonorId(DONOR_ID).stream()
                .anyMatch(i -> i.getItemDonationId() == itemDonationId));
    }

    @Test
    @DisplayName("updateDeliveryStatus and updateDeliveryDate persist, and the date can be cleared")
    void updateDeliveryStatusAndDate() throws SQLException {
        int id = createItemDonation("Towels", 6, PickupOrDrop.DROP_OFF, null).getItemDonationId();
        Date today = new Date(System.currentTimeMillis());

        assertTrue(itemDonationDAO.updateDeliveryStatus(id, DeliveryStatus.PICKED_UP));
        assertEquals(DeliveryStatus.PICKED_UP, itemDonationDAO.findById(id).getDeliveryStatus());

        assertTrue(itemDonationDAO.updateDeliveryStatus(id, DeliveryStatus.DELIVERED));
        assertTrue(itemDonationDAO.updateDeliveryDate(id, today));
        ItemDonation delivered = itemDonationDAO.findById(id);
        assertEquals(DeliveryStatus.DELIVERED, delivered.getDeliveryStatus());
        assertEquals(today.toString(), delivered.getDeliveryDate().toString());

        assertTrue(itemDonationDAO.updateDeliveryDate(id, null));
        assertNull(itemDonationDAO.findById(id).getDeliveryDate());
    }

    @Test
    @DisplayName("delete removes just the item_donations row")
    void deleteRemovesRow() throws SQLException {
        int id = createItemDonation("Pillows", 2, PickupOrDrop.DROP_OFF, null).getItemDonationId();

        assertTrue(itemDonationDAO.delete(id));
        assertNull(itemDonationDAO.findById(id));
        assertFalse(itemDonationDAO.delete(id), "deleting it again should affect nothing");
    }

    @Test
    @DisplayName("findByDonationIds fetches several item donations in one call, keyed by donation_id")
    void findByDonationIdsBatch() throws SQLException {
        ItemDonation first = createItemDonation("Blankets", 10, PickupOrDrop.DROP_OFF, null);
        ItemDonation second = createItemDonation("Rice", 20, PickupOrDrop.PICKUP, "Somewhere 1");

        // A MONEY donation has no item_donations row, so it must be absent from the result
        int moneyDonationId = trackDonation(donationDAO.createDonation(
                new Donation(DONOR_ID, INSTITUTION_ID, null, "MONEY", new BigDecimal("5.00"))));

        Map<Integer, ItemDonation> result = itemDonationDAO.findByDonationIds(
                List.of(first.getDonationId(), second.getDonationId(), moneyDonationId));

        assertEquals(2, result.size());
        assertEquals("Blankets", result.get(first.getDonationId()).getItemName());
        assertEquals("Rice", result.get(second.getDonationId()).getItemName());
        assertFalse(result.containsKey(moneyDonationId));
    }

    @Test
    @DisplayName("findByDonationIds with null or an empty list returns an empty map")
    void findByDonationIdsEmptyInput() throws SQLException {
        assertTrue(itemDonationDAO.findByDonationIds(List.of()).isEmpty());
        assertTrue(itemDonationDAO.findByDonationIds(null).isEmpty());
    }
}
