package com.careconnect.payment;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * POST /api/payments/webhook
 * Razorpay calls this server-to-server, so a payment is still recorded if the donor closes the
 * browser right after paying. It is authenticated by the X-Razorpay-Signature header, not by login.
 *
 * Optional setup (needs a public URL such as an ngrok tunnel): Razorpay Dashboard > Settings >
 * Webhooks > add https://<public-url>/api/payments/webhook, events payment.captured, order.paid,
 * payment.failed, and the same secret as RAZORPAY_WEBHOOK_SECRET.
 */
@WebServlet("/api/payments/webhook")
public class WebhookServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(WebhookServlet.class.getName());

    private final PaymentDAO dao = new PaymentDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // The signature covers the exact raw bytes, so read the body before anything parses it.
        String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String signature = req.getHeader("X-Razorpay-Signature");

        try {
            if (signature == null || !RazorpayClient.verifyWebhookSignature(body, signature)) {
                LOG.warning("Webhook rejected: bad or missing signature");
                resp.setStatus(400);
                return;
            }

            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            String event = str(root, "event");
            JsonObject payload = root.has("payload") ? root.getAsJsonObject("payload") : null;
            JsonObject entity = paymentEntity(payload);

            if (entity != null && event != null) {
                String orderId = str(entity, "order_id");
                String paymentId = str(entity, "id");

                switch (event) {
                    case "payment.captured":
                    case "order.paid":
                        handlePaid(orderId, paymentId, entity);
                        break;
                    case "payment.failed":
                        dao.markFailed(orderId, str(entity, "error_description"));
                        break;
                    default:
                        break; // ignore events we did not subscribe to
                }
            }
            resp.setStatus(200);

        } catch (SQLException | RuntimeException e) {
            // A non-2xx reply makes Razorpay retry later, which is what we want for transient errors.
            LOG.log(Level.SEVERE, "Webhook processing failed", e);
            resp.setStatus(500);
        }
    }

    private void handlePaid(String orderId, String paymentId, JsonObject entity) throws SQLException {
        Payment p = dao.findByOrderId(orderId);
        if (p == null) {
            LOG.warning("Webhook for unknown order " + orderId);
            return;
        }
        // Integrity check: the amount Razorpay captured must match what we stored.
        if (entity.has("amount") && entity.get("amount").getAsLong() != p.getAmountPaise()) {
            LOG.severe("Amount mismatch for order " + orderId + "; payment not marked PAID");
            return;
        }
        dao.markPaid(orderId, paymentId, null);
        try {
            // Idempotent: completes the donation now, or does nothing if it is already completed.
            PaymentSuccessHandler.onPaid(dao.findByOrderId(orderId));
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "PaymentSuccessHandler failed for order " + orderId, e);
        }
    }

    private static JsonObject paymentEntity(JsonObject payload) {
        if (payload == null || !payload.has("payment")) {
            return null;
        }
        JsonObject payment = payload.getAsJsonObject("payment");
        return payment.has("entity") ? payment.getAsJsonObject("entity") : null;
    }

    private static String str(JsonObject o, String key) {
        return (o != null && o.has(key) && !o.get(key).isJsonNull()) ? o.get(key).getAsString() : null;
    }
}
