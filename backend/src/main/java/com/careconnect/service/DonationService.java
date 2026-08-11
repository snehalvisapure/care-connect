package com.careconnect.service;

import com.careconnect.dao.DonationDAO;
import com.careconnect.dao.InstitutionDAO;
import com.careconnect.dao.NeedDAO;
import com.careconnect.model.Donation;
import com.careconnect.model.Need;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for monetary donations and donor pledges.
 * Sits between the Controller and DonationDAO - the Controller should call
 * this class, never DonationDAO directly.
 *
 * There's no separate "pledge" concept in the schema: a pledge IS a
 * donation row where payment_status = PENDING (the state createDonation()
 * leaves it in by default). Confirming payment moves it to COMPLETED.
 * This class doesn't touch item_donations - that's ItemDonationService's job.
 */
public class DonationService {

    private static final String DONATION_TYPE_MONEY = "MONEY";

    private static final String PAYMENT_STATUS_PENDING = "PENDING";
    private static final String PAYMENT_STATUS_COMPLETED = "COMPLETED";

    private static final String DONATION_STATUS_COMPLETED = "COMPLETED";
    private static final String DONATION_STATUS_CANCELLED = "CANCELLED";

    private final DonationDAO donationDAO;
    private final InstitutionDAO institutionDAO;
    private final NeedDAO needDAO;

    public DonationService() {
        this.donationDAO = new DonationDAO();
        this.institutionDAO = new InstitutionDAO();
        this.needDAO = new NeedDAO();
    }

    /**
     * Creates a monetary pledge/donation. It starts out as a pledge
     * (payment_status = PENDING, donation_status = PENDING, no
     * transaction_id/receipt_number yet) - call confirmPayment() once the
     * payment actually goes through.
     *
     * @param needId optional - pass null for a general donation to the
     *               institution not tied to any specific need.
     * @throws IllegalArgumentException if the institution doesn't exist,
     *         the need doesn't exist or belongs to a different institution,
     *         or amount isn't positive.
     */
    public Donation createMonetaryDonation(int donorId, int institutionId, Integer needId,
                                            BigDecimal amount) throws SQLException {

        if (institutionDAO.findById(institutionId) == null) {
            throw new IllegalArgumentException("No institution found with institution_id=" + institutionId);
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (needId != null) {
            Need need = needDAO.findById(needId);
            if (need == null) {
                throw new IllegalArgumentException("No need found with need_id=" + needId);
            }
            if (need.getInstitutionId() != institutionId) {
                throw new IllegalArgumentException(
                        "need_id=" + needId + " does not belong to institution_id=" + institutionId);
            }
        }

        Donation donation = new Donation(donorId, institutionId, needId, DONATION_TYPE_MONEY, amount);
        int donationId = donationDAO.createDonation(donation);
        return donationDAO.getDonationById(donationId);
    }

    /**
     * A pledge is just the wording for a freshly-created donation before
     * payment completes - this is a thin alias over createMonetaryDonation()
     * so pledge-specific callers don't need to know that detail.
     */
    public Donation createPledge(int donorId, int institutionId, Integer needId, BigDecimal amount)
            throws SQLException {
        return createMonetaryDonation(donorId, institutionId, needId, amount);
    }

    /**
     * Confirms that payment for a pledge/donation has gone through.
     * Records the transaction ID, auto-generates a receipt number, and
     * moves both payment_status and donation_status to COMPLETED.
     *
     * @throws IllegalArgumentException if no such donation exists, or transactionId is blank.
     * @throws IllegalStateException if the donation isn't currently PENDING
     *         (already confirmed, or was cancelled).
     */
    public Donation confirmPayment(int donationId, String transactionId) throws SQLException {
        Donation donation = donationDAO.getDonationById(donationId);
        if (donation == null) {
            throw new IllegalArgumentException("No donation found with donation_id=" + donationId);
        }
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("transactionId cannot be blank");
        }
        if (!PAYMENT_STATUS_PENDING.equals(donation.getPaymentStatus())) {
            throw new IllegalStateException(
                    "Only a PENDING donation can be confirmed. Current payment_status: "
                            + donation.getPaymentStatus());
        }

        String receiptNumber = generateReceiptNumber(donationId);

        donationDAO.updateTransactionAndReceipt(donationId, transactionId, receiptNumber);
        donationDAO.updatePaymentStatus(donationId, PAYMENT_STATUS_COMPLETED);
        donationDAO.updateDonationStatus(donationId, DONATION_STATUS_COMPLETED);

        return donationDAO.getDonationById(donationId);
    }

    /**
     * Cancels a pledge. Only allowed while payment is still PENDING - once
     * a payment has actually completed, it can't be cancelled through this
     * method (that would need a refund process, which is out of scope here).
     *
     * @throws IllegalArgumentException if no such donation exists.
     * @throws IllegalStateException if payment_status isn't PENDING.
     */
    public Donation cancelPledge(int donationId) throws SQLException {
        Donation donation = donationDAO.getDonationById(donationId);
        if (donation == null) {
            throw new IllegalArgumentException("No donation found with donation_id=" + donationId);
        }
        if (!PAYMENT_STATUS_PENDING.equals(donation.getPaymentStatus())) {
            throw new IllegalStateException(
                    "Only a PENDING pledge can be cancelled. Current payment_status: "
                            + donation.getPaymentStatus());
        }

        donationDAO.updateDonationStatus(donationId, DONATION_STATUS_CANCELLED);
        return donationDAO.getDonationById(donationId);
    }

    /**
     * Fetches a single donation by donation_id.
     *
     * @throws IllegalArgumentException if no such donation exists.
     */
    public Donation getDonation(int donationId) throws SQLException {
        Donation donation = donationDAO.getDonationById(donationId);
        if (donation == null) {
            throw new IllegalArgumentException("No donation found with donation_id=" + donationId);
        }
        return donation;
    }

    /**
     * A donor's full donation history (all types, all statuses), most recent first.
     */
    public List<Donation> getDonationHistoryForDonor(int donorId) throws SQLException {
        return donationDAO.getDonationsByDonor(donorId);
    }

    /**
     * All donations received by an institution (all types, all statuses), most recent first.
     */
    public List<Donation> getDonationHistoryForInstitution(int institutionId) throws SQLException {
        return donationDAO.getDonationsByInstitution(institutionId);
    }

    /**
     * A donor's currently-pending pledges only.
     */
    public List<Donation> getPendingPledgesForDonor(int donorId) throws SQLException {
        return donationDAO.getPendingPledgesByDonor(donorId);
    }

    /**
     * An institution's currently-pending pledges only (donations promised
     * but not yet paid). DonationDAO doesn't have a dedicated query for
     * this, so it's filtered here rather than adding a narrow one-off
     * method to the DAO.
     */
    public List<Donation> getPendingPledgesForInstitution(int institutionId) throws SQLException {
        return donationDAO.getDonationsByInstitution(institutionId).stream()
                .filter(d -> PAYMENT_STATUS_PENDING.equals(d.getPaymentStatus()))
                .collect(Collectors.toList());
    }

    /**
     * Generates a simple, readable receipt number. Not meant to be
     * cryptographically unique across a real production system - just
     * distinct enough for this project's scope (the DB also enforces
     * UNIQUE on receipt_number as a safety net).
     */
    private String generateReceiptNumber(int donationId) {
        return "RCPT-" + String.format("%06d", donationId) + "-" + System.currentTimeMillis();
    }
}
