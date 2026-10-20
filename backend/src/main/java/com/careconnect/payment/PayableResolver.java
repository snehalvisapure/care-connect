package com.careconnect.payment;

import com.careconnect.model.Donation;
import com.careconnect.service.DonationService;

import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.OptionalLong;
import java.util.Set;

/**
 * Decides WHAT can be paid and HOW MUCH, using the database only.
 * The amount never comes from the browser, so a donor cannot edit the request to pay Rs 1.
 */
final class PayableResolver {

    static final String TYPE_DONATION = "DONATION";

    /** Things a user can pay for. */
    static final Set<String> SUPPORTED_TYPES = Set.of(TYPE_DONATION);

    private static final DonationService DONATIONS = new DonationService();

    private PayableResolver() {}

    /**
     * Returns the amount in PAISE if this donation is a pending money donation that belongs
     * to the logged-in user. Returns empty if it does not exist, belongs to someone else,
     * is already paid, or was cancelled.
     */
    static OptionalLong resolveAmountPaise(String referenceType, long referenceId, int userId)
            throws SQLException {

        if (!TYPE_DONATION.equals(referenceType) || referenceId < 1 || referenceId > Integer.MAX_VALUE) {
            return OptionalLong.empty();
        }

        Donation donation;
        try {
            donation = DONATIONS.getDonation((int) referenceId);
        } catch (IllegalArgumentException notFound) {
            return OptionalLong.empty();
        }

        boolean isMine = donation.getDonorId() == userId;
        boolean isPayable = "MONEY".equals(donation.getDonationType())
                && "PENDING".equals(donation.getPaymentStatus())
                && !"CANCELLED".equals(donation.getDonationStatus())
                && donation.getAmount() != null;

        if (!isMine || !isPayable) {
            return OptionalLong.empty();
        }

        long paise = donation.getAmount()
                .movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
        return OptionalLong.of(paise);
    }
}
