package com.careconnect.payment;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;

/**
 * Minimal Razorpay integration with no SDK: one REST call (create order) and HMAC-SHA256
 * signature checks. Needs Java 11+ (java.net.http) and Gson 2.8.6+.
 */
public final class RazorpayClient {

    private static final String ORDERS_URL = "https://api.razorpay.com/v1/orders";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final Gson gson = new Gson();

    /** Creates an order on Razorpay and returns its id (e.g. order_Abc123). Amount is in paise. */
    public String createOrder(long amountPaise, String receipt) throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        body.addProperty("amount", amountPaise);
        body.addProperty("currency", PaymentConfig.currency());
        body.addProperty("receipt", receipt);

        String basic = Base64.getEncoder().encodeToString(
                (PaymentConfig.keyId() + ":" + PaymentConfig.keySecret()).getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder(URI.create(ORDERS_URL))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Basic " + basic)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Razorpay order creation failed: HTTP " + response.statusCode()
                    + " " + response.body());
        }
        return JsonParser.parseString(response.body()).getAsJsonObject().get("id").getAsString();
    }

    /** Verifies the signature the browser receives after checkout: HMAC(order_id|payment_id). */
    public static boolean verifyCheckoutSignature(String orderId, String paymentId, String signature) {
        return constantTimeEquals(hmacSha256Hex(orderId + "|" + paymentId, PaymentConfig.keySecret()), signature);
    }

    /** Verifies a webhook: HMAC of the exact raw request body using the webhook secret. */
    public static boolean verifyWebhookSignature(String rawBody, String signature) {
        return constantTimeEquals(hmacSha256Hex(rawBody, PaymentConfig.webhookSecret()), signature);
    }

    private static String hmacSha256Hex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", e);
        }
    }

    /** Constant-time comparison so signature checks do not leak timing information. */
    private static boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }
}
