package com.careconnect.payment;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * POST /api/payments/create-order
 * Body (JSON): { "referenceType": "DONATION", "referenceId": 12 }
 *
 * Creates a Razorpay order for a pending donation that belongs to the logged-in user, stores it
 * as CREATED, and returns what the checkout popup needs. The amount is read from the donations
 * table, never from the browser.
 */
@WebServlet("/api/payments/create-order")
public class CreateOrderServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(CreateOrderServlet.class.getName());
    private static final long MIN_AMOUNT_PAISE = 100; // Razorpay minimum is Rs 1

    private final PaymentDAO dao = new PaymentDAO();
    private final RazorpayClient razorpay = new RazorpayClient();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer userId = PaymentServletSupport.currentUserId(req);
        if (userId == null) {
            PaymentServletSupport.error(resp, 401, "Not logged in.");
            return;
        }

        try {
            JsonObject body = PaymentServletSupport.readJson(req);

            String type = PaymentServletSupport.str(body, "referenceType");
            type = type == null ? "" : type.trim().toUpperCase();
            if (!PayableResolver.SUPPORTED_TYPES.contains(type)) {
                PaymentServletSupport.error(resp, 400, "Unsupported payment type.");
                return;
            }

            String idText = PaymentServletSupport.str(body, "referenceId");
            if (idText == null) {
                throw new NumberFormatException("referenceId missing");
            }
            long referenceId = Long.parseLong(idText.trim());

            // Already paid? Finish any half-done confirmation and refuse a second charge.
            Payment alreadyPaid = dao.findPaidByReference(type, referenceId);
            if (alreadyPaid != null) {
                runSuccessHandler(alreadyPaid);
                PaymentServletSupport.error(resp, 409, "This donation has already been paid.");
                return;
            }

            OptionalLong resolved = PayableResolver.resolveAmountPaise(type, referenceId, userId);
            if (resolved.isEmpty()) {
                PaymentServletSupport.error(resp, 400,
                        "This donation cannot be paid (not found, not yours, already paid or cancelled).");
                return;
            }
            long amountPaise = resolved.getAsLong();
            if (amountPaise < MIN_AMOUNT_PAISE) {
                PaymentServletSupport.error(resp, 400, "Amount must be at least Rs 1.");
                return;
            }

            String receipt = "cc_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
            String orderId = razorpay.createOrder(amountPaise, receipt);

            Payment p = new Payment();
            p.setUserId(userId);
            p.setReferenceType(type);
            p.setReferenceId(referenceId);
            p.setAmountPaise(amountPaise);
            p.setCurrency(PaymentConfig.currency());
            p.setRazorpayOrderId(orderId);
            long paymentId = dao.insertCreated(p);

            JsonObject out = new JsonObject();
            out.addProperty("paymentId", paymentId);
            out.addProperty("orderId", orderId);
            out.addProperty("amount", amountPaise);
            out.addProperty("currency", PaymentConfig.currency());
            out.addProperty("keyId", PaymentConfig.keyId());
            PaymentServletSupport.writeJson(resp, 200, out);

        } catch (JsonParseException e) {
            PaymentServletSupport.error(resp, 400, "Malformed JSON in request body.");
        } catch (NumberFormatException e) {
            PaymentServletSupport.error(resp, 400, "Invalid reference id.");
        } catch (IllegalStateException e) {
            LOG.log(Level.SEVERE, "Payment configuration problem", e);
            PaymentServletSupport.error(resp, 500, "Payment gateway is not configured.");
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Database error while creating order", e);
            PaymentServletSupport.error(resp, 500, "Could not start the payment. Please try again.");
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Razorpay call failed", e);
            PaymentServletSupport.error(resp, 502, "Payment provider is unreachable. Please try again.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            PaymentServletSupport.error(resp, 502, "Payment request was interrupted. Please try again.");
        }
    }

    private void runSuccessHandler(Payment payment) {
        try {
            PaymentSuccessHandler.onPaid(payment);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "PaymentSuccessHandler failed for order " + payment.getRazorpayOrderId(), e);
        }
    }
}
