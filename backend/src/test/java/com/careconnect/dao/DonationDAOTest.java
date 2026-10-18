package com.careconnect.dao;

import com.careconnect.DatabaseTestBase;
import com.careconnect.model.Donation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests DonationDAO against the real database (see DatabaseTestBase).
 * Replaces the old main()-based runner.
 */
class DonationDAOTest extends DatabaseTestBase {

    private final DonationDAO donationDAO = new DonationDAO();

    private Donation createMoneyDonation(String amount) throws SQLException {
        Donation donation = new Donation(DONOR_ID, INSTITUTION_ID, null, "MONEY", new BigDecimal(amount));
        int donationId = trackDonation(donationDAO.createDonation(donation));
        assertTrue(donationId > 0, "createDonation should return the generated donation_id");
        return donationDAO.getDonationById(donationId);
    }

    @Test
    @DisplayName("createDonation saves a MONEY donation that getDonationById reads back")
    void createAndReadBack() throws SQLException {
        Donation saved = createMoneyDonation("500.00");

        assertNotNull(saved);
        assertEquals(DONOR_ID, saved.getDonorId());
        assertEquals(INSTITUTION_ID, saved.getInstitutionId());
        assertNull(saved.getNeedId());
        assertEquals("MONEY", saved.getDonationType());
        assertEquals(0, new BigDecimal("500.00").compareTo(saved.getAmount()));
        assertEquals("PENDING", saved.getPaymentStatus(), "a new donation starts as a pending pledge");
        assertNotNull(saved.getDonationDate());
    }

    @Test
    @DisplayName("getDonationById returns null for an unknown id")
    void unknownIdReturnsNull() throws SQLException {
        assertNull(donationDAO.getDonationById(Integer.MAX_VALUE));
    }

    @Test
    @DisplayName("a new donation shows up in the donor's pending pledges and in both history lists")
    void appearsInPendingAndHistoryLists() throws SQLException {
        Donation saved = createMoneyDonation("120.00");
        int id = saved.getDonationId();

        assertTrue(donationDAO.getPendingPledgesByDonor(DONOR_ID).stream()
                .anyMatch(d -> d.getDonationId() == id));
        assertTrue(donationDAO.getDonationsByDonor(DONOR_ID).stream()
                .anyMatch(d -> d.getDonationId() == id));
        assertTrue(donationDAO.getDonationsByInstitution(INSTITUTION_ID).stream()
                .anyMatch(d -> d.getDonationId() == id));
    }

    @Test
    @DisplayName("history lists are ordered most recent first")
    void historyIsMostRecentFirst() throws SQLException {
        createMoneyDonation("10.00");
        createMoneyDonation("20.00");

        List<Donation> history = donationDAO.getDonationsByDonor(DONOR_ID);
        for (int i = 1; i < history.size(); i++) {
            Timestamp newer = history.get(i - 1).getDonationDate();
            Timestamp older = history.get(i).getDonationDate();
            if (newer != null && older != null) {   // ignore any old rows with no date
                assertFalse(newer.before(older), "donation_date should never increase going down the list");
            }
        }
    }

    @Test
    @DisplayName("updatePaymentStatus moves a donation out of the pending pledges")
    void updatePaymentStatusRemovesFromPending() throws SQLException {
        int id = createMoneyDonation("75.00").getDonationId();

        assertTrue(donationDAO.updatePaymentStatus(id, "COMPLETED"));

        assertEquals("COMPLETED", donationDAO.getDonationById(id).getPaymentStatus());
        assertFalse(donationDAO.getPendingPledgesByDonor(DONOR_ID).stream()
                .anyMatch(d -> d.getDonationId() == id));
    }

    @Test
    @DisplayName("updateTransactionAndReceipt and updateDonationStatus persist their values")
    void updateTransactionReceiptAndStatus() throws SQLException {
        int id = createMoneyDonation("60.00").getDonationId();
        String receipt = "RCPT-TEST-" + System.nanoTime();

        assertTrue(donationDAO.updateTransactionAndReceipt(id, "TXN-TEST-1", receipt));
        assertTrue(donationDAO.updateDonationStatus(id, "COMPLETED"));

        Donation updated = donationDAO.getDonationById(id);
        assertEquals("TXN-TEST-1", updated.getTransactionId());
        assertEquals(receipt, updated.getReceiptNumber());
        assertEquals("COMPLETED", updated.getDonationStatus());
    }

    @Test
    @DisplayName("updates on an unknown donation id affect nothing")
    void updateOnUnknownIdReturnsFalse() throws SQLException {
        assertFalse(donationDAO.updatePaymentStatus(Integer.MAX_VALUE, "COMPLETED"));
        assertFalse(donationDAO.updateDonationStatus(Integer.MAX_VALUE, "COMPLETED"));
    }
}
