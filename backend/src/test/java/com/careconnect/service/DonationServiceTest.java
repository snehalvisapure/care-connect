package com.careconnect.service;

import com.careconnect.DatabaseTestBase;
import com.careconnect.model.DeliveryStatus;
import com.careconnect.model.Donation;
import com.careconnect.model.DonationHistoryEntry;
import com.careconnect.model.ItemDonation;
import com.careconnect.model.PickupOrDrop;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests DonationService (pledge lifecycle and the combined donation history)
 * against the real database (see DatabaseTestBase).
 * Replaces the old main()-based runner.
 */
class DonationServiceTest extends DatabaseTestBase {

    private final DonationService donationService = new DonationService();
    private final ItemDonationService itemDonationService = new ItemDonationService();

    private Donation newPledge(String amount) throws SQLException {
        Donation pledge = donationService.createPledge(DONOR_ID, INSTITUTION_ID, null, new BigDecimal(amount));
        trackDonation(pledge.getDonationId());
        return pledge;
    }

    private HistoryFinder historyOf(List<DonationHistoryEntry> history) {
        return new HistoryFinder(history);
    }

    /** Small helper to look an entry up by donation_id. */
    private static final class HistoryFinder {
        private final List<DonationHistoryEntry> history;

        HistoryFinder(List<DonationHistoryEntry> history) {
            this.history = history;
        }

        DonationHistoryEntry byDonationId(int donationId) {
            return history.stream()
                    .filter(e -> e.getDonationId() == donationId)
                    .findFirst()
                    .orElse(null);
        }
    }

    // ---------------------------------------------------------------- pledge lifecycle

    @Test
    @DisplayName("createPledge starts PENDING and shows up in the donor's and institution's pending pledges")
    void pledgeStartsPending() throws SQLException {
        Donation pledge = newPledge("500.00");

        assertEquals("PENDING", pledge.getPaymentStatus());
        assertEquals("MONEY", pledge.getDonationType());
        assertTrue(donationService.getPendingPledgesForDonor(DONOR_ID).stream()
                .anyMatch(d -> d.getDonationId() == pledge.getDonationId()));
        assertTrue(donationService.getPendingPledgesForInstitution(INSTITUTION_ID).stream()
                .anyMatch(d -> d.getDonationId() == pledge.getDonationId()));
    }

    @Test
    @DisplayName("confirmPayment completes the pledge with a transaction id and receipt number")
    void confirmPaymentCompletesPledge() throws SQLException {
        Donation pledge = newPledge("500.00");

        Donation confirmed = donationService.confirmPayment(pledge.getDonationId(), "TXN-TEST-12345");

        assertEquals("COMPLETED", confirmed.getPaymentStatus());
        assertEquals("COMPLETED", confirmed.getDonationStatus());
        assertEquals("TXN-TEST-12345", confirmed.getTransactionId());
        assertNotNull(confirmed.getReceiptNumber());
        assertTrue(confirmed.getReceiptNumber().startsWith("RCPT-"));
        assertFalse(donationService.getPendingPledgesForDonor(DONOR_ID).stream()
                .anyMatch(d -> d.getDonationId() == pledge.getDonationId()));
    }

    @Test
    @DisplayName("confirming the same pledge twice is rejected")
    void doubleConfirmationIsRejected() throws SQLException {
        Donation pledge = newPledge("500.00");
        donationService.confirmPayment(pledge.getDonationId(), "TXN-TEST-1");

        assertThrows(IllegalStateException.class,
                () -> donationService.confirmPayment(pledge.getDonationId(), "TXN-TEST-2"));
    }

    @Test
    @DisplayName("confirmPayment rejects a blank transaction id and an unknown donation")
    void confirmPaymentValidation() throws SQLException {
        Donation pledge = newPledge("50.00");

        assertThrows(IllegalArgumentException.class,
                () -> donationService.confirmPayment(pledge.getDonationId(), "  "));
        assertThrows(IllegalArgumentException.class,
                () -> donationService.confirmPayment(Integer.MAX_VALUE, "TXN-TEST-1"));
    }

    @Test
    @DisplayName("a pending pledge can be cancelled, but a completed one cannot")
    void cancelPledgeRules() throws SQLException {
        Donation toCancel = newPledge("200.00");
        Donation cancelled = donationService.cancelPledge(toCancel.getDonationId());
        assertEquals("CANCELLED", cancelled.getDonationStatus());

        Donation toComplete = newPledge("300.00");
        donationService.confirmPayment(toComplete.getDonationId(), "TXN-TEST-3");
        assertThrows(IllegalStateException.class,
                () -> donationService.cancelPledge(toComplete.getDonationId()));
    }

    @Test
    @DisplayName("createPledge validates institution and amount")
    void createPledgeValidation() {
        assertThrows(IllegalArgumentException.class,
                () -> donationService.createPledge(DONOR_ID, Integer.MAX_VALUE, null, new BigDecimal("10.00")));
        assertThrows(IllegalArgumentException.class,
                () -> donationService.createPledge(DONOR_ID, INSTITUTION_ID, null, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> donationService.createPledge(DONOR_ID, INSTITUTION_ID, null, new BigDecimal("-5.00")));
        assertThrows(IllegalArgumentException.class,
                () -> donationService.createPledge(DONOR_ID, INSTITUTION_ID, null, null));
    }

    @Test
    @DisplayName("getDonation throws for an unknown id")
    void getDonationUnknownId() {
        assertThrows(IllegalArgumentException.class, () -> donationService.getDonation(Integer.MAX_VALUE));
    }

    // ---------------------------------------------------------------- plain history

    @Test
    @DisplayName("plain history shows both a completed and a cancelled pledge")
    void plainHistoryShowsAllStatuses() throws SQLException {
        Donation completed = newPledge("500.00");
        donationService.confirmPayment(completed.getDonationId(), "TXN-TEST-4");
        Donation cancelled = newPledge("200.00");
        donationService.cancelPledge(cancelled.getDonationId());

        List<Donation> history = donationService.getDonationHistoryForDonor(DONOR_ID);

        assertTrue(history.stream().anyMatch(d -> d.getDonationId() == completed.getDonationId()));
        assertTrue(history.stream().anyMatch(d -> d.getDonationId() == cancelled.getDonationId()));
    }

    // ---------------------------------------------------------------- combined history

    @Test
    @DisplayName("combined history attaches item details to ITEM rows and leaves MONEY rows without them")
    void combinedHistoryStitchesItemDetails() throws SQLException {
        Donation money = newPledge("500.00");
        ItemDonation item = itemDonationService.createItemDonation(
                DONOR_ID, INSTITUTION_ID, null, "Blankets", 12, PickupOrDrop.PICKUP, "12 MG Road, Pune");
        trackDonation(item.getDonationId());

        HistoryFinder history = historyOf(donationService.getCombinedHistoryForDonor(DONOR_ID));

        DonationHistoryEntry moneyEntry = history.byDonationId(money.getDonationId());
        assertNotNull(moneyEntry);
        assertEquals("MONEY", moneyEntry.getDonationType());
        assertEquals(0, new BigDecimal("500.00").compareTo(moneyEntry.getAmount()));
        assertNull(moneyEntry.getItemDonationId());
        assertNull(moneyEntry.getItemName());
        assertNull(moneyEntry.getQuantity());
        assertNull(moneyEntry.getDeliveryStatus());

        DonationHistoryEntry itemEntry = history.byDonationId(item.getDonationId());
        assertNotNull(itemEntry);
        assertEquals("ITEM", itemEntry.getDonationType());
        assertEquals(Integer.valueOf(item.getItemDonationId()), itemEntry.getItemDonationId());
        assertEquals("Blankets", itemEntry.getItemName());
        assertEquals(Integer.valueOf(12), itemEntry.getQuantity());
        assertEquals(PickupOrDrop.PICKUP, itemEntry.getPickupOrDrop());
        assertEquals("12 MG Road, Pune", itemEntry.getPickupAddress());
        assertEquals(DeliveryStatus.PENDING, itemEntry.getDeliveryStatus());
        assertNull(itemEntry.getDeliveryDate());
    }

    @Test
    @DisplayName("combined history reflects delivery progress of an item donation")
    void combinedHistoryReflectsDelivery() throws SQLException {
        ItemDonation item = itemDonationService.createItemDonation(
                DONOR_ID, INSTITUTION_ID, null, "Towels", 3, PickupOrDrop.DROP_OFF, null);
        trackDonation(item.getDonationId());
        itemDonationService.approveItemDonation(item.getItemDonationId());
        itemDonationService.markDelivered(item.getItemDonationId(), null);

        DonationHistoryEntry entry = historyOf(donationService.getCombinedHistoryForDonor(DONOR_ID))
                .byDonationId(item.getDonationId());

        assertNotNull(entry);
        assertEquals(DeliveryStatus.DELIVERED, entry.getDeliveryStatus());
        assertNotNull(entry.getDeliveryDate());
        assertEquals("COMPLETED", entry.getDonationStatus());
    }

    @Test
    @DisplayName("combined history for an institution also includes both kinds of donation")
    void combinedHistoryForInstitution() throws SQLException {
        Donation money = newPledge("40.00");
        ItemDonation item = itemDonationService.createItemDonation(
                DONOR_ID, INSTITUTION_ID, null, "Soap", 9, PickupOrDrop.DROP_OFF, null);
        trackDonation(item.getDonationId());

        HistoryFinder history = historyOf(donationService.getCombinedHistoryForInstitution(INSTITUTION_ID));

        assertNotNull(history.byDonationId(money.getDonationId()));
        assertEquals("Soap", history.byDonationId(item.getDonationId()).getItemName());
    }

    @Test
    @DisplayName("combined history has the same donations, in the same order, as the plain history")
    void combinedHistoryMatchesPlainHistoryOrder() throws SQLException {
        newPledge("11.00");
        trackDonation(itemDonationService.createItemDonation(
                DONOR_ID, INSTITUTION_ID, null, "Pens", 2, PickupOrDrop.DROP_OFF, null).getDonationId());

        List<Integer> plain = donationService.getDonationHistoryForDonor(DONOR_ID).stream()
                .map(Donation::getDonationId).toList();
        List<Integer> combined = donationService.getCombinedHistoryForDonor(DONOR_ID).stream()
                .map(DonationHistoryEntry::getDonationId).toList();

        assertEquals(plain, combined);
    }
}
