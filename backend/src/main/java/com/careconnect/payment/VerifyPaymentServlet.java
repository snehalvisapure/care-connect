package com.careconnect.payment;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * POST /api/payments/verify
 * Body (JSON): { "razorpay_order_id", "razorpay_payment_id", "razorpay_signature" }  (sent by the checkout popup)
 *
 * Only a payment whose HMAC signature checks out is marked PAID, and only then is the donation completed.
 */
@WebServlet("/api/payments/verify")
public class VerifyPaymentServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(VerifyPaymentServlet.class.getName());

    private final PaymentDAO dao = new PaymentDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer userId = PaymentServletSupport.currentUserId(req);
        if (userId == null) {
            PaymentServletSupport.error(resp, 401, "Not logged in.");
            return;
        }

        try {
            JsonObject body = PaymentServletSupport.readJson(req);
            String orderId = trim(PaymentServletSupport.str(body, "razorpay_order_id"));
            String paymentId = trim(PaymentServletSupport.str(body, "razorpay_payment_id"));
            String signature = trim(PaymentServletSupport.str(body, "razorpay_signature"));
            if (orderId.isEmpty() || paymentId.isEmpty() || signature.isEmpty()) {
                PaymentServletSupport.error(resp, 400, "Missing payment details.");
                return;
            }

            Payment p = dao.findByOrderId(orderId);
            if (p == null) {
                PaymentServletSupport.error(resp, 404, "Unknown order.");
                return;
            }
            if (p.getUserId() != userId) {
                PaymentServletSupport.error(resp, 403, "This payment belongs to another user.");
                return;
            }

            if (!"PAID".equals(p.getStatus())) {
                if (!RazorpayClient.verifyCheckoutSignature(orderId, paymentId, signature)) {
                    LOG.warning("Invalid signature for order " + orderId + " (user " + userId + ")");
                    PaymentServletSupport.error(resp, 400, "Payment verification failed.");
                    return;
                }
                dao.markPaid(orderId, paymentId, signature);
            }

            // Idempotent: completes the donation now, or does nothing if it is already completed.
            runSuccessHandler(orderId);

            JsonObject out = new JsonObject();
            out.addProperty("status", "PAID");
            out.addProperty("orderId", orderId);
            PaymentServletSupport.writeJson(resp, 200, out);

        } catch (JsonParseException e) {
            PaymentServletSupport.error(resp, 400, "Malformed JSON in request body.");
        } catch (IllegalStateException e) {
            LOG.log(Level.SEVERE, "Payment configuration problem", e);
            PaymentServletSupport.error(resp, 500, "Payment gateway is not configured.");
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Database error while verifying payment", e);
            PaymentServletSupport.error(resp, 500, "Could not confirm the payment. Contact support if you were charged.");
        }
    }

    /** A failure in the donation update must not undo a payment that is already recorded as PAID. */
    private void runSuccessHandler(String orderId) {
        try {
            PaymentSuccessHandler.onPaid(dao.findByOrderId(orderId));
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "PaymentSuccessHandler failed for order " + orderId, e);
        }
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
