package com.careconnect.payment;

import com.careconnect.service.DonationService;

import java.sql.SQLException;

/**
 * Runs after a payment is verified as PAID. It completes the donation through the project's
 * own DonationService.confirmPayment(), so the receipt number, payment_status and
 * donation_status are set exactly as before.
 *
 * Safe to call more than once for the same payment (the browser verify call and the webhook
 * can both arrive): a donation that is already confirmed is simply skipped.
 */
final class PaymentSuccessHandler {

    private static final DonationService DONATIONS = new DonationService();

    private PaymentSuccessHandler() {}

    /** synchronized so verify and webhook can never confirm the same donation at the same moment. */
    static synchronized void onPaid(Payment payment) throws SQLException {
        if (payment == null || !PayableResolver.TYPE_DONATION.equals(payment.getReferenceType())) {
            return;
        }
        try {
            DONATIONS.confirmPayment((int) payment.getReferenceId(), payment.getRazorpayPaymentId());
        } catch (IllegalStateException alreadyConfirmed) {
            // The donation is no longer PENDING (already confirmed). Nothing left to do.
        }
    }
}
